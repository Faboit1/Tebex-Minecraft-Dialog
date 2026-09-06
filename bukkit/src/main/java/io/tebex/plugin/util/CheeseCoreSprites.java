package io.tebex.plugin.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Reflective bridge to <a href="https://github.com/Faboit1/CheeseCore">CheeseCore</a>,
 * which resolves a material to the atlas sprite the viewer's own client actually has.
 *
 * <p>Doing this correctly is harder than it looks. The atlas holding a texture moved
 * between versions (item textures lived in {@code minecraft:blocks} until 1.21.10 and
 * got their own {@code minecraft:items} atlas in 1.21.11), and a material's sprite is
 * often not named after the material at all — {@code GRASS_BLOCK} draws
 * {@code block/grass_block_side}. CheeseCore derives both from the real client assets
 * per version, and with ViaVersion installed resolves against each player's client
 * rather than the server, so a mixed-version server still renders correctly.</p>
 *
 * <p>Everything is reflective and cached: CheeseCore is a Java 21 Paper plugin and an
 * optional dependency, so it must never be a load-time or compile-time requirement.</p>
 */
public final class CheeseCoreSprites {
    private static final String API_CLASS = "top.cheesesmp.cheesecore.api.CheeseCore";

    private static volatile boolean lookedUp;
    private static volatile boolean methodsResolved;
    private static volatile boolean broken;
    private static volatile String unavailableReason = "not checked yet";

    private static Method isAvailable;
    private static Method sprites;
    private static Method spriteFor;
    private static Method versionOf;
    private static Method supportsSprites;
    private static Method atlasOf;
    private static Method spriteIdOf;
    private static Method approximateOf;
    private static Method asMinimalString;

    private CheeseCoreSprites() {
    }

    /** One resolved sprite: the atlas, the sprite id inside it, and whether it is a stand-in. */
    public static final class SpriteRef {
        private final String atlas;
        private final String sprite;
        private final boolean approximate;

        SpriteRef(String atlas, String sprite, boolean approximate) {
            this.atlas = atlas;
            this.sprite = sprite;
            this.approximate = approximate;
        }

        public String getAtlas() {
            return atlas;
        }

        public String getSprite() {
            return sprite;
        }

        /**
         * Whether this is only a colour swatch rather than a real icon. Chests, banners,
         * shulker boxes, mob heads and shields are drawn by a block entity and have no
         * flat texture in the client's assets, so all that can be shown is their break
         * particle colour.
         */
        public boolean isApproximate() {
            return approximate;
        }
    }

    public static boolean isAvailable() {
        if (broken) return false;
        if (!resolveMethods()) return false;

        try {
            if ((Boolean) isAvailable.invoke(null)) return true;
            unavailableReason = "installed but not enabled";
            return false;
        } catch (Throwable e) {
            broken = true;
            unavailableReason = "unusable: " + e;
            return false;
        }
    }

    /** What the last availability check saw, for the dialog diagnostics line. */
    public static String describe() {
        return isAvailable() ? "available" : unavailableReason;
    }

    /**
     * Resolves a material for one viewer.
     *
     * @return the sprite, or {@code null} if CheeseCore is absent, the viewer's client
     *         cannot draw sprites, or the material has no sprite at all.
     */
    public static SpriteRef resolve(Material material, Player viewer) {
        if (material == null || viewer == null || !isAvailable()) return null;

        try {
            Object service = sprites.invoke(null);
            if (service == null) return null;

            // A client older than 1.21.9 has no object component to render into, and
            // with ViaVersion that is a per-player fact rather than a server-wide one.
            Object version = versionOf.invoke(service, viewer);
            if (version == null || !((Boolean) supportsSprites.invoke(version))) return null;

            Object result = spriteFor.invoke(service, material, viewer);
            if (!(result instanceof Optional)) return null;

            Optional<?> optional = (Optional<?>) result;
            if (!optional.isPresent()) return null;

            Object sprite = optional.get();
            String atlas = (String) asMinimalString.invoke(atlasOf.invoke(sprite));
            String spriteId = (String) asMinimalString.invoke(spriteIdOf.invoke(sprite));
            if (atlas == null || spriteId == null) return null;

            return new SpriteRef(atlas, spriteId, (Boolean) approximateOf.invoke(sprite));
        } catch (Throwable e) {
            // A CheeseCore that changed shape under us must degrade to the built-in
            // resolver rather than break the shop, and must not retry on every button.
            broken = true;
            unavailableReason = "call failed: " + e;
            return null;
        }
    }

    /**
     * Resolves and caches the reflective handles. Only the lookup is cached — whether
     * CheeseCore is currently enabled is asked fresh every time, because it may enable
     * after this plugin does.
     */
    private static boolean resolveMethods() {
        if (methodsResolved) return true;
        if (lookedUp) return false;

        synchronized (CheeseCoreSprites.class) {
            if (methodsResolved) return true;
            if (lookedUp) return false;
            lookedUp = true;

            try {
                if (Bukkit.getPluginManager().getPlugin("CheeseCore") == null) {
                    unavailableReason = "not installed";
                    return false;
                }

                Class<?> api = Class.forName(API_CLASS);
                isAvailable = api.getMethod("isAvailable");
                sprites = api.getMethod("sprites");

                Class<?> service = Class.forName("top.cheesesmp.cheesecore.api.SpriteService");
                spriteFor = service.getMethod("sprite", Material.class, Player.class);
                versionOf = service.getMethod("versionOf", Player.class);

                Class<?> clientVersion = Class.forName("top.cheesesmp.cheesecore.api.ClientVersion");
                supportsSprites = clientVersion.getMethod("supportsSprites");

                Class<?> sprite = Class.forName("top.cheesesmp.cheesecore.api.Sprite");
                atlasOf = sprite.getMethod("atlas");
                spriteIdOf = sprite.getMethod("sprite");
                approximateOf = sprite.getMethod("approximate");

                Class<?> key = Class.forName("net.kyori.adventure.key.Key");
                asMinimalString = key.getMethod("asMinimalString");

                methodsResolved = true;
                return true;
            } catch (Throwable e) {
                unavailableReason = "unusable: " + e;
                return false;
            }
        }
    }
}
