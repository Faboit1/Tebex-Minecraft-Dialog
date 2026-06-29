package io.tebex.plugin.command;

import io.tebex.plugin.BukkitPluginPlatform;
import io.tebex.plugin.gui.BuyGUI;
import io.tebex.plugin.gui.DialogGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuyCommand extends Command {
    private final BukkitPluginPlatform platform;
    private static final Pattern VERSION_PATTERN = Pattern.compile("1\\.(\\d+)(?:\\.(\\d+))?");
    private static final Pattern NEW_VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    public BuyCommand(String command, BukkitPluginPlatform platform) {
        super(command);
        this.platform = platform;
    }

    private boolean isVersionAtLeast1_21_6() {
        String version = Bukkit.getBukkitVersion();
        Matcher match = VERSION_PATTERN.matcher(version);
        boolean isCompatible = false;
        
        if (match.find()) {
            try {
                int minor = Integer.parseInt(match.group(1));
                int patch = match.group(2) != null ? Integer.parseInt(match.group(2)) : 0;
                if (minor > 21) {
                    isCompatible = true;
                } else if (minor == 21 && patch >= 6) {
                    isCompatible = true;
                }
            } catch (Exception ignored) {
            }
        }
        
        if (!isCompatible) {
            // Check for format where "1." might be omitted, e.g., "26.1.2"
            Matcher newMatch = NEW_VERSION_PATTERN.matcher(version);
            if (newMatch.find()) {
                try {
                    int major = Integer.parseInt(newMatch.group(1));
                    if (major >= 26) {
                        isCompatible = true;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        
        return isCompatible;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if(! platform.isSetup()) {
            sender.sendMessage(ChatColor.RED + "Tebex is not setup yet!");
            return true;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            
            if (isVersionAtLeast1_21_6()) {
                if (args.length == 0) {
                    new DialogGUI(platform).open(player);
                } else if (args.length >= 2 && args[0].equalsIgnoreCase("category")) {
                    try {
                        int categoryId = Integer.parseInt(args[1]);
                        new DialogGUI(platform).openCategory(player, categoryId);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid category ID.");
                    }
                } else if (args.length >= 2 && args[0].equalsIgnoreCase("package")) {
                    try {
                        int packageId = Integer.parseInt(args[1]);
                        new DialogGUI(platform).openPackage(player, packageId);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid package ID.");
                    }
                } else {
                    new DialogGUI(platform).open(player);
                }
            } else {
                new BuyGUI(platform).open(player);
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "The buy command cannot be used from the console.");
        return false;
    }
}
