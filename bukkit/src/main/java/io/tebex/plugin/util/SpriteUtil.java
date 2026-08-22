package io.tebex.plugin.util;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpriteUtil {
    private static final Pattern VERSION_PATTERN = Pattern.compile("1\\.(\\d+)(?:\\.(\\d+))?");

    public static boolean isSpriteSupported() {
        return isVersionAtLeast(21, 9);
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

        // The content type for an atlas sprite is "object", and the sprite path is
        // relative to the atlas, so it carries no namespace of its own.
        JsonObject sprite = new JsonObject();
        sprite.addProperty("type", "object");
        sprite.addProperty("atlas", atlas);
        sprite.addProperty("sprite", prefix + name.toLowerCase(Locale.ENGLISH));
        return sprite;
    }

    private static boolean itemsHaveDedicatedAtlas() {
        return isVersionAtLeast(21, 11);
    }

    private static boolean isVersionAtLeast(int targetMinor, int targetPatch) {
        // Paper/Folia/Canvas expose the real MC version via getMinecraftVersion()
        try {
            String mcVersion = (String) Bukkit.class.getMethod("getMinecraftVersion").invoke(null);
            Matcher match = VERSION_PATTERN.matcher(mcVersion);
            if (match.find()) {
                int minor = Integer.parseInt(match.group(1));
                int patch = match.group(2) != null ? Integer.parseInt(match.group(2)) : 0;
                if (minor != targetMinor) return minor > targetMinor;
                return patch >= targetPatch;
            }
        } catch (Exception ignored) {
        }

        // Fallback to getBukkitVersion() for vanilla Spigot/CraftBukkit
        String version = Bukkit.getBukkitVersion();
        try {
            Matcher match = VERSION_PATTERN.matcher(version);
            if (match.find()) {
                int minor = Integer.parseInt(match.group(1));
                int patch = match.group(2) != null ? Integer.parseInt(match.group(2)) : 0;
                if (minor != targetMinor) return minor > targetMinor;
                return patch >= targetPatch;
            }
        } catch (Exception ignored) {
        }

        return false;
    }
}
