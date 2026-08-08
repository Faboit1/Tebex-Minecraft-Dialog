package io.tebex.plugin.util;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpriteUtil {
    private static final Pattern VERSION_PATTERN = Pattern.compile("1\\.(\\d+)(?:\\.(\\d+))?");
    private static final Pattern ALT_VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    private static final int MIN_ALT_MAJOR = 26;

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

        JsonObject sprite = new JsonObject();
        sprite.addProperty("type", "sprite");
        sprite.addProperty("atlas", atlas);
        sprite.addProperty("sprite", "minecraft:" + prefix + name.toLowerCase(Locale.ENGLISH));
        return sprite;
    }

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
        String version = Bukkit.getBukkitVersion();
        try {
            Matcher match = VERSION_PATTERN.matcher(version);
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

        try {
            Matcher alt = ALT_VERSION_PATTERN.matcher(version);
            if (alt.find()) {
                int major = Integer.parseInt(alt.group(1));
                if (major >= MIN_ALT_MAJOR) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }
}
