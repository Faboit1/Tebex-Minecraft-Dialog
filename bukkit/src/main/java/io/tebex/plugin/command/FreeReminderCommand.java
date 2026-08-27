package io.tebex.plugin.command;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.BukkitPluginPlatform;
import io.tebex.plugin.util.MiniMessageUtil;
import io.tebex.sdk.platform.BasePluginPlatform;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Controls how often players are reminded that they have free packages waiting.
 * Writes through the Tebex YAML document rather than Bukkit's own config saver, which
 * would strip every comment out of config.yml.
 */
public class FreeReminderCommand extends Command {
    private static final List<String> SUGGESTIONS = Arrays.asList("off", "1m", "5m", "10m", "30m");

    private final BukkitPluginPlatform platform;

    public FreeReminderCommand(String name, BukkitPluginPlatform platform) {
        super(name);
        this.platform = platform;
        setDescription("Sets how often players are reminded about free packages.");
        setUsage("/" + name + " <off|1m|5m|10m>");
        setPermission("tebex.showfreereminder");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!testPermission(sender)) return true;

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Free package reminder: " + describeCurrent());
            sender.sendMessage(ChatColor.GRAY + "Usage: " + getUsage());
            return true;
        }

        String argument = args[0].toLowerCase(Locale.ENGLISH);

        if (argument.equals("off") || argument.equals("none") || argument.equals("disable")) {
            if (!apply(sender, false, 0)) return true;
            sender.sendMessage(ChatColor.GREEN + "Free package reminders are now off.");
            return true;
        }

        Integer minutes = parseMinutes(argument);
        if (minutes == null) {
            sender.sendMessage(ChatColor.RED + "Use a number of minutes such as 1m, 5m or 10m, or 'off'.");
            return true;
        }

        if (!apply(sender, true, minutes)) return true;

        sender.sendMessage(ChatColor.GREEN + "Reminding players about free packages every "
                + minutes + (minutes == 1 ? " minute." : " minutes."));
        sender.sendMessage(ChatColor.GRAY + "Preview: " + ChatColor.RESET + MiniMessageUtil.toSection(
                currentMessage().replace("%player%", sender.getName())));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length != 1) return Collections.emptyList();

        String prefix = args[0].toLowerCase(Locale.ENGLISH);
        return SUGGESTIONS.stream()
                .filter(option -> option.startsWith(prefix))
                .collect(Collectors.toList());
    }

    /**
     * @return Whether the new setting was stored; the caller reports success only if so.
     */
    private boolean apply(CommandSender sender, boolean enabled, int minutes) {
        YamlDocument yaml = platform.getPlatformConfig().getYamlDocument();
        if (yaml == null) {
            sender.sendMessage(ChatColor.RED + "Tebex configuration is not loaded yet.");
            return false;
        }

        yaml.set("free-packages.reminder.enabled", enabled);
        if (enabled) {
            yaml.set("free-packages.reminder.interval-minutes", minutes);
        }

        try {
            yaml.save();
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Failed to save config.yml: " + e.getMessage());
            return false;
        }

        // Bukkit caches its own view of the file, so refresh it before the reminder
        // task reads the new values, and restart the countdown from now.
        platform.getPlugin().reloadConfig();
        platform.getPlugin().resetFreeReminderCountdown();
        return true;
    }

    private String describeCurrent() {
        if (!platform.getPlugin().getConfig().getBoolean("free-packages.reminder.enabled", true)) {
            return "off";
        }
        int minutes = Math.max(1, platform.getPlugin().getConfig().getInt("free-packages.reminder.interval-minutes", 10));
        return "every " + minutes + (minutes == 1 ? " minute" : " minutes");
    }

    private String currentMessage() {
        String message = platform.getPlugin().getConfig().getString("free-packages.reminder.message",
                BasePluginPlatform.DEFAULT_FREE_REMINDER_MESSAGE);
        return message == null ? BasePluginPlatform.DEFAULT_FREE_REMINDER_MESSAGE : message;
    }

    /**
     * Accepts "10m", "10min" or a bare "10", all meaning ten minutes.
     */
    private Integer parseMinutes(String input) {
        String digits = input;
        if (digits.endsWith("minutes")) digits = digits.substring(0, digits.length() - 7);
        else if (digits.endsWith("min")) digits = digits.substring(0, digits.length() - 3);
        else if (digits.endsWith("m")) digits = digits.substring(0, digits.length() - 1);

        try {
            int minutes = Integer.parseInt(digits.trim());
            return minutes >= 1 ? minutes : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
