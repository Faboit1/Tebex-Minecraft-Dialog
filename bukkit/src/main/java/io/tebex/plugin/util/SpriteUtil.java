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

    private static Boolean compare(String version, int targetMinor, int targetPatch) {
        if (version == null) return null;

        Matcher match = VERSION_PATTERN.matcher(version);
        if (!match.find()) return null;

        int minor = Integer.parseInt(match.group(1));
        int patch = match.group(2) != null ? Integer.parseInt(match.group(2)) : 0;
        if (minor != targetMinor) return minor > targetMinor;
        return patch >= targetPatch;
    }
}
