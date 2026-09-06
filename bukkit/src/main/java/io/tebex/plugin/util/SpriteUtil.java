package io.tebex.plugin.util;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpriteUtil {
    // Anchored at the start so "26.1.2" is read as 26.1.2 rather than matching the "1.2"
    // buried inside it, which is what an unanchored "1\.(\d+)" pattern finds.
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    public static boolean isSpriteSupported() {
        return isVersionAtLeast(21, 9);
    }

    /**
     * Reports what the version probes actually saw, so a server that renders no sprites
     * can be told apart from one that is simply too old for them.
     */
    public static String describeSupport() {
        return "sprites=" + isSpriteSupported()
                + ", minecraftVersion=" + describe(readMinecraftVersion())
                + ", bukkitVersion=" + describe(readBukkitVersion())
                + ", dedicatedItemAtlas=" + itemsHaveDedicatedAtlas();
    }

    private static String describe(String value) {
        return value == null ? "unavailable" : value;
    }

    private static String readMinecraftVersion() {
        try {
            return (String) Bukkit.class.getMethod("getMinecraftVersion").invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static String readBukkitVersion() {
        try {
            return Bukkit.getBukkitVersion();
        } catch (Throwable e) {
            return null;
        }
    }

    public static JsonObject spriteComponent(Material material) {
        if (material == null) {
            return null;
        }

        String name = material.name();
        if (name.equals("AIR") || name.endsWith("_AIR")) {
            return null;
        }

        boolean block = material.isBlock();
        String prefix = block ? "block/" : "item/";
        String atlas;
        if (block) {
            atlas = "minecraft:blocks";
        } else {
            atlas = itemsHaveDedicatedAtlas() ? "minecraft:items" : "minecraft:blocks";
        }

        // An atlas sprite is an "object" content type, inferred from the atlas/sprite
        // pair rather than declared, which is how every documented example writes it.
        // The sprite path is relative to the atlas, so it carries no namespace itself.
        JsonObject sprite = new JsonObject();
        sprite.addProperty("atlas", atlas);
        sprite.addProperty("sprite", prefix + name.toLowerCase(Locale.ENGLISH));
        return sprite;
    }

    private static boolean itemsHaveDedicatedAtlas() {
        return isVersionAtLeast(21, 11);
    }

    private static boolean isVersionAtLeast(int targetMinor, int targetPatch) {
        // Paper/Folia/Canvas expose the real MC version via getMinecraftVersion();
        // vanilla Spigot/CraftBukkit only has getBukkitVersion().
        Boolean fromMinecraft = compare(readMinecraftVersion(), targetMinor, targetPatch);
        if (fromMinecraft != null) return fromMinecraft;

        Boolean fromBukkit = compare(readBukkitVersion(), targetMinor, targetPatch);
        return fromBukkit != null && fromBukkit;
    }

    /**
     * Compares a server version string against a {@code 1.<minor>.<patch>} target.
     *
     * <p>Minecraft left the {@code 1.x} scheme behind after 1.21.11 and now versions by
     * year, so 26.1 and later are newer than every 1.x release. Treating them as a major
     * version above 1 gets that ordering right; the previous pattern only recognised
     * {@code 1.x} at all, which quietly turned sprites off on every 26.x server.</p>
     */
    private static Boolean compare(String version, int targetMinor, int targetPatch) {
        if (version == null) return null;

        Matcher match = VERSION_PATTERN.matcher(version.trim());
        if (!match.find()) return null;

        int major = Integer.parseInt(match.group(1));
        if (major != 1) return major > 1;

        int minor = Integer.parseInt(match.group(2));
        int patch = match.group(3) != null ? Integer.parseInt(match.group(3)) : 0;
        if (minor != targetMinor) return minor > targetMinor;
        return patch >= targetPatch;
    }
}
