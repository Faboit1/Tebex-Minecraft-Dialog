package io.tebex.sdk.commands;

public class CommandResponder {
    public static String formatFancy(CommandContext context, String message, String... args) {
        // "Tebex" appears always in cyan from the raw message
        if (!context.isFromConsole()) {
            message = message.replaceAll("Tebex", "§bTebex");
        }

        // Insert args and color them gold
        for (int i = 0; i < args.length; i++) {
            String placeholder = "{" + i + "}";

            // underline addresses and append positional arg colored gold
            if (!context.isFromConsole()) {
                if (args[i].contains("https://")) {
                    args[i] = args[i].replace(args[i], "§n" + args[i]);
                }

                message = message.replace(placeholder, "§6" + args[i] + "§f");
            } else { // otherwise for the console just replace the positional arg without coloring
                message = message.replace(placeholder, args[i]);
            }
        }

        // Stop before adding a colored prefix if this is a console response
        if (context.isFromConsole()) {
            return message;
        }

        // Otherwise append a prefix (cyan), message is white
        String prefixedFormattedMessage = "§b[Tebex] §f" + message;
        return prefixedFormattedMessage;
    }

    public static String formatError(CommandContext context, String message) {
        if (context.isFromConsole()) {
            return message;
        }

        // "Tebex" appears always in cyan
        message = message.replaceAll("Tebex", "§bTebex");

        // Prefix is cyan, message is red
        return "§b[Tebex] §c" + message;
    }

    public static String formatSuccess(CommandContext context, String message, String... args) {
        if (context.isFromConsole()) {
            return message;
        }

        // "Tebex" appears always in cyan
        message = message.replaceAll("Tebex", "§bTebex");

        // Insert args and color them gold
        for (int i = 0; i < args.length; i++) {
            String placeholder = "{" + i + "}";
            message = message.replace(placeholder, "§6" + args[i] + "§f");
        }

        // Prefix is cyan, message is white
        return "§b[Tebex] §a" + message;
    }

    public static void tellOtherFancy(CommandContext context, String message, String... args) {
        context.tellTarget(formatFancy(context, message, args));
    }
}
