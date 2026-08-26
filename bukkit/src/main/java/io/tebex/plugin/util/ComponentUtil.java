package io.tebex.plugin.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Turns the MiniMessage-style strings in config.yml into real JSON text components.
 *
 * <p>Dialogs are sent as JSON, where styling lives in dedicated component fields.
 * Collapsing tags into legacy section codes throws away everything those codes cannot
 * encode — {@code shadow_color} most visibly — so tags are translated straight into the
 * component fields instead. Unrecognised tags are left in the text untouched.
 */
public final class ComponentUtil {
    private static final Map<String, NamedColor> COLORS = new HashMap<>();
    private static final Map<Character, String> LEGACY_COLORS = new HashMap<>();
    private static final Map<Character, String> LEGACY_FORMATS = new HashMap<>();
    private static final Map<String, String> FORMAT_ALIASES = new HashMap<>();

    private static final class NamedColor {
        private final String name;
        private final int rgb;

        private NamedColor(String name, int rgb) {
            this.name = name;
            this.rgb = rgb;
        }
    }

    static {
        register('0', "black", 0x000000);
        register('1', "dark_blue", 0x0000AA);
        register('2', "dark_green", 0x00AA00);
        register('3', "dark_aqua", 0x00AAAA);
        register('4', "dark_red", 0xAA0000);
        register('5', "dark_purple", 0xAA00AA);
        register('6', "gold", 0xFFAA00);
        register('7', "gray", 0xAAAAAA);
        register('8', "dark_gray", 0x555555);
        register('9', "blue", 0x5555FF);
        register('a', "green", 0x55FF55);
        register('b', "aqua", 0x55FFFF);
        register('c', "red", 0xFF5555);
        register('d', "light_purple", 0xFF55FF);
        register('e', "yellow", 0xFFFF55);
        register('f', "white", 0xFFFFFF);

        COLORS.put("grey", COLORS.get("gray"));
        COLORS.put("dark_grey", COLORS.get("dark_gray"));

        LEGACY_FORMATS.put('l', "bold");
        LEGACY_FORMATS.put('o', "italic");
        LEGACY_FORMATS.put('n', "underlined");
        LEGACY_FORMATS.put('m', "strikethrough");
        LEGACY_FORMATS.put('k', "obfuscated");

        FORMAT_ALIASES.put("bold", "bold");
        FORMAT_ALIASES.put("b", "bold");
        FORMAT_ALIASES.put("italic", "italic");
        FORMAT_ALIASES.put("i", "italic");
        FORMAT_ALIASES.put("em", "italic");
        FORMAT_ALIASES.put("underlined", "underlined");
        FORMAT_ALIASES.put("under", "underlined");
        FORMAT_ALIASES.put("u", "underlined");
        FORMAT_ALIASES.put("strikethrough", "strikethrough");
        FORMAT_ALIASES.put("st", "strikethrough");
        FORMAT_ALIASES.put("obfuscated", "obfuscated");
        FORMAT_ALIASES.put("obf", "obfuscated");
    }

    private static void register(char code, String name, int rgb) {
        NamedColor color = new NamedColor(name, rgb);
        COLORS.put(name, color);
        LEGACY_COLORS.put(code, name);
    }

    private ComponentUtil() {
    }

    /**
     * Parses a configured string into a single component, for fields that take one
     * (a dialog title, a tooltip, a body message).
     */
    public static JsonObject parse(String input) {
        JsonObject root = new JsonObject();
        root.addProperty("text", "");

        JsonArray parts = parseParts(input);
        if (parts.size() > 0) {
            root.add("extra", parts);
        }

        return root;
    }

    /**
     * Parses a configured string into its individually styled runs, so callers can
     * compose them with other components such as sprites.
     */
    public static JsonArray parseParts(String input) {
        JsonArray parts = new JsonArray();
        if (input == null || input.isEmpty()) {
            return parts;
        }

        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(null, new Style()));

        StringBuilder text = new StringBuilder();
        int index = 0;

        while (index < input.length()) {
            char current = input.charAt(index);

            if (current == '<') {
                int close = input.indexOf('>', index);
                if (close != -1 && handleTag(input.substring(index + 1, close), stack, parts, text)) {
                    index = close + 1;
                    continue;
                }
            } else if ((current == '&' || current == '§') && index + 1 < input.length()) {
                char code = Character.toLowerCase(input.charAt(index + 1));
                if (handleLegacyCode(code, stack, parts, text)) {
                    index += 2;
                    continue;
                }
            }

            text.append(current);
            index++;
        }

        flush(parts, text, stack.peek().style);
        return parts;
    }

    private static boolean handleTag(String rawTag, Deque<Frame> stack, JsonArray parts, StringBuilder text) {
        String tag = rawTag.trim().toLowerCase(Locale.ENGLISH);
        if (tag.isEmpty()) return false;

        if (tag.charAt(0) == '/') {
            String name = canonical(tag.substring(1).trim());
            if (name == null) return false;

            flush(parts, text, stack.peek().style);
            closeFrame(stack, name);
            return true;
        }

        if (tag.equals("reset") || tag.equals("r")) {
            flush(parts, text, stack.peek().style);
            while (stack.size() > 1) {
                stack.pop();
            }
            stack.peek().style = new Style();
            return true;
        }

        Style style = stack.peek().style.copy();
        String name = canonical(tag);

        if (name == null) {
            return false;
        }

        if (name.startsWith("shadow")) {
            Integer shadow = parseShadow(tag);
            if (shadow == null) return false;
            style.shadowColor = shadow;
        } else if (FORMAT_ALIASES.containsKey(name)) {
            style.setFormat(FORMAT_ALIASES.get(name), true);
        } else {
            String color = parseColor(name);
            if (color == null) return false;
            style.color = color;
        }

        flush(parts, text, stack.peek().style);
        stack.push(new Frame(name, style));
        return true;
    }

    /**
     * Normalises a tag to the key its matching closing tag will produce, so that
     * {@code <b>…</bold>} still pairs up.
     */
    private static String canonical(String tag) {
        if (tag.isEmpty()) return null;

        if (tag.startsWith("shadow")) return "shadow";
        if (FORMAT_ALIASES.containsKey(tag)) return FORMAT_ALIASES.get(tag);

        String colorTag = tag;
        if (colorTag.startsWith("color:")) colorTag = colorTag.substring(6);
        else if (colorTag.startsWith("colour:")) colorTag = colorTag.substring(7);

        if (COLORS.containsKey(colorTag)) return COLORS.get(colorTag).name;
        if (isHex(colorTag)) return colorTag;

        return null;
    }

    private static String parseColor(String name) {
        NamedColor named = COLORS.get(name);
        if (named != null) return named.name;
        return isHex(name) ? name : null;
    }

    private static boolean isHex(String value) {
        if (value.length() != 7 || value.charAt(0) != '#') return false;
        for (int i = 1; i < 7; i++) {
            if (Character.digit(value.charAt(i), 16) == -1) return false;
        }
        return true;
    }

    /**
     * Parses {@code shadow}, {@code shadow:<color>} and {@code shadow:<color>:<alpha>}
     * into the packed ARGB integer the component field expects.
     */
    private static Integer parseShadow(String tag) {
        String[] segments = tag.split(":");
        int rgb = 0x000000;
        double alpha = 1.0D;

        if (segments.length > 1 && !segments[1].isEmpty()) {
            NamedColor named = COLORS.get(segments[1]);
            if (named != null) {
                rgb = named.rgb;
            } else if (isHex(segments[1])) {
                rgb = Integer.parseInt(segments[1].substring(1), 16);
            } else {
                return null;
            }
        }

        if (segments.length > 2 && !segments[2].isEmpty()) {
            try {
                alpha = Double.parseDouble(segments[2]);
            } catch (NumberFormatException e) {
                return null;
            }
            // Alpha is a 0-1 fraction in MiniMessage, but 0-255 is a natural mistake.
            if (alpha > 1.0D) alpha = alpha / 255.0D;
            alpha = Math.max(0.0D, Math.min(1.0D, alpha));
        }

        return ((int) Math.round(alpha * 255.0D) << 24) | (rgb & 0xFFFFFF);
    }

    private static boolean handleLegacyCode(char code, Deque<Frame> stack, JsonArray parts, StringBuilder text) {
        if (code == 'r') {
            flush(parts, text, stack.peek().style);
            while (stack.size() > 1) {
                stack.pop();
            }
            stack.peek().style = new Style();
            return true;
        }

        String color = LEGACY_COLORS.get(code);
        if (color != null) {
            flush(parts, text, stack.peek().style);
            // A legacy colour code clears any active formatting, as it does in chat.
            Style style = new Style();
            style.color = color;
            style.shadowColor = stack.peek().style.shadowColor;
            stack.peek().style = style;
            return true;
        }

        String format = LEGACY_FORMATS.get(code);
        if (format != null) {
            flush(parts, text, stack.peek().style);
            Style style = stack.peek().style.copy();
            style.setFormat(format, true);
            stack.peek().style = style;
            return true;
        }

        return false;
    }

    private static void closeFrame(Deque<Frame> stack, String name) {
        boolean present = false;
        for (Frame frame : stack) {
            if (name.equals(frame.tag)) {
                present = true;
                break;
            }
        }
        if (!present) return;

        while (stack.size() > 1) {
            Frame frame = stack.pop();
            if (name.equals(frame.tag)) return;
        }
    }

    private static void flush(JsonArray parts, StringBuilder text, Style style) {
        if (text.length() == 0) return;

        JsonObject part = new JsonObject();
        part.addProperty("text", text.toString());
        style.applyTo(part);
        parts.add(part);

        text.setLength(0);
    }

    private static final class Frame {
        private final String tag;
        private Style style;

        private Frame(String tag, Style style) {
            this.tag = tag;
            this.style = style;
        }
    }

    private static final class Style {
        private String color;
        private Integer shadowColor;
        private Boolean bold;
        private Boolean italic;
        private Boolean underlined;
        private Boolean strikethrough;
        private Boolean obfuscated;

        private Style copy() {
            Style copy = new Style();
            copy.color = color;
            copy.shadowColor = shadowColor;
            copy.bold = bold;
            copy.italic = italic;
            copy.underlined = underlined;
            copy.strikethrough = strikethrough;
            copy.obfuscated = obfuscated;
            return copy;
        }

        private void setFormat(String format, boolean value) {
            if ("bold".equals(format)) bold = value;
            else if ("italic".equals(format)) italic = value;
            else if ("underlined".equals(format)) underlined = value;
            else if ("strikethrough".equals(format)) strikethrough = value;
            else if ("obfuscated".equals(format)) obfuscated = value;
        }

        private void applyTo(JsonObject part) {
            if (color != null) part.addProperty("color", color);
            if (shadowColor != null) part.addProperty("shadow_color", shadowColor);
            if (bold != null) part.addProperty("bold", bold);
            if (italic != null) part.addProperty("italic", italic);
            if (underlined != null) part.addProperty("underlined", underlined);
            if (strikethrough != null) part.addProperty("strikethrough", strikethrough);
            if (obfuscated != null) part.addProperty("obfuscated", obfuscated);
        }
    }
}
