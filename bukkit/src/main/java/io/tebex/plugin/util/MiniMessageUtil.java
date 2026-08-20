package io.tebex.plugin.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniMessageUtil {
    private static final Map<String, String> NAMED_COLORS = new HashMap<>();
    private static final Pattern TAG_PATTERN = Pattern.compile("<(/?)([^>]+)>");
    private static final Pattern HEX_PATTERN = Pattern.compile("#([0-9a-fA-F]{6})");

    static {
        NAMED_COLORS.put("black", "§0");
        NAMED_COLORS.put("dark_blue", "§1");
        NAMED_COLORS.put("dark_green", "§2");
        NAMED_COLORS.put("dark_aqua", "§3");
        NAMED_COLORS.put("dark_red", "§4");
        NAMED_COLORS.put("dark_purple", "§5");
        NAMED_COLORS.put("gold", "§6");
        NAMED_COLORS.put("gray", "§7");
        NAMED_COLORS.put("grey", "§7");
        NAMED_COLORS.put("dark_gray", "§8");
        NAMED_COLORS.put("dark_grey", "§8");
        NAMED_COLORS.put("blue", "§9");
        NAMED_COLORS.put("green", "§a");
        NAMED_COLORS.put("aqua", "§b");
        NAMED_COLORS.put("red", "§c");
        NAMED_COLORS.put("light_purple", "§d");
        NAMED_COLORS.put("yellow", "§e");
        NAMED_COLORS.put("white", "§f");
        NAMED_COLORS.put("bold", "§l");
        NAMED_COLORS.put("b", "§l");
        NAMED_COLORS.put("italic", "§o");
        NAMED_COLORS.put("i", "§o");
        NAMED_COLORS.put("em", "§o");
        NAMED_COLORS.put("underlined", "§n");
        NAMED_COLORS.put("u", "§n");
        NAMED_COLORS.put("strikethrough", "§m");
        NAMED_COLORS.put("st", "§m");
        NAMED_COLORS.put("obfuscated", "§k");
        NAMED_COLORS.put("obf", "§k");
        NAMED_COLORS.put("reset", "§r");
        NAMED_COLORS.put("r", "§r");
    }

    public static String toSection(String input) {
        if (input == null) return "";

        String result = input.replace("&", "§");

        StringBuffer sb = new StringBuffer();
        Matcher matcher = TAG_PATTERN.matcher(result);
        while (matcher.find()) {
            boolean closing = !matcher.group(1).isEmpty();
            String tag = matcher.group(2).toLowerCase();

            String replacement;
            if (closing) {
                replacement = "§r";
            } else {
                Matcher hex = HEX_PATTERN.matcher(tag);
                if (hex.matches()) {
                    String hexColor = hex.group(1);
                    StringBuilder hexCode = new StringBuilder("§x");
                    for (char c : hexColor.toCharArray()) {
                        hexCode.append('§').append(c);
                    }
                    replacement = hexCode.toString();
                } else {
                    replacement = NAMED_COLORS.getOrDefault(tag, matcher.group(0));
                }
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
