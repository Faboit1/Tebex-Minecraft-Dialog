package io.tebex.plugin.util;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds Minecraft 1.21.9+ "object" text components that render an item/block
 * sprite straight from the vanilla texture atlas, no resource pack required.
 *
 * <p>The sprite object component was introduced in 1.21.9. The MiniMessage tag
 * form is {@code <sprite:"atlas":"prefix/name">}; because the dialog API consumes
 * raw JSON text components (this plugin has no Adventure/MiniMessage runtime), we
 * emit the equivalent component directly:
 * {@code {"atlas":"minecraft:items","sprite":"item/diamond"}}.</p>
 */
public class SpriteUtil {
    private static final Pattern VERSION_PATTERN = Pattern.compile("1\\.(\\d+)(?:\\.(\\d+))?");

    /**
     * @return true if the running server is at least 1.21.9, the first version to
     *         support inline sprite object components.
     */
    public static boolean isSpriteSupported() {
        return isVersionAtLeast(21, 9);
    }

    /**
     * Builds the sprite object text component for the given material, or
     * {@code null} when no icon should be shown (air / unknown material).
     */
    public static JsonObject spriteComponent(Material material) {
        if (material == null) {
            return null;
        }

        String name = material.name();
        // Material.isAir() does not exist on the 1.8 compile target, so match by name.
        if (name.equals("AIR") || name.endsWith("_AIR")) {
            return null;
        }

        boolean block = material.isBlock();
        String prefix = block ? "block/" : "item/";
        String atlas;
        if (block) {
            atlas = "minecraft:blocks";
        } else {
            // Items only gained their own atlas in 1.21.11; before that every sprite
            // (items included) lives in the "minecraft:blocks" atlas.
            atlas = itemsHaveDedicatedAtlas() ? "minecraft:items" : "minecraft:blocks";
        }

        JsonObject sprite = new JsonObject();
        sprite.addProperty("type", "sprite");
        sprite.addProperty("atlas", atlas);
        sprite.addProperty("sprite", prefix + name.toLowerCase(Locale.ENGLISH));
        return sprite;
    }

    /**
     * MiniMessage sprite tag equivalent of {@link #spriteComponent(Material)},
     * kept for use on Adventure-capable surfaces (chat, titles, etc.).
     */
    public static String getMiniMessageSprite(Material material) {
        JsonObject sprite = spriteComponent(material);
        if (sprite == null) {
            return "";
        }
        return String.format("<sprite:\"%s\":\"%s\">",
                sprite.get("atlas").getAsString(),
                sprite.get("sprite").getAsString());
    }

    private static boolean itemsHaveDedicatedAtlas() {
        return isVersionAtLeast(21, 11);
    }

    private static boolean isVersionAtLeast(int targetMinor, int targetPatch) {
        try {
            Matcher match = VERSION_PATTERN.matcher(Bukkit.getBukkitVersion());
            if (match.find()) {
                int minor = Integer.parseInt(match.group(1));
                int patch = match.group(2) != null ? Integer.parseInt(match.group(2)) : 0;
                if (minor != targetMinor) {
                    return minor > targetMinor;
                }
                return patch >= targetPatch;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
