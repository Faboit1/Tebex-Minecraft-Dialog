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
    private static final Pattern ALTERNATIVE_VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    
    /**
     * Minimum compatible major version for the alternative versioning format (e.g., 26.1.2).
     * Experimental Paper APIs have mapped to versions like 26.1.2 based on issue reports. 
     * This value, 26, is chosen because it was reported to map to the 1.21.6 era, which introduced the dialog API natively.
     * Note: this heuristic is based on issue reports and may need to be updated as Paper's experimental versioning scheme evolves.
     */
    private static final int MIN_COMPATIBLE_MAJOR_VERSION = 26; 

    public BuyCommand(String command, BukkitPluginPlatform platform) {
        super(command);
        this.platform = platform;
    }

    private boolean isVersionAtLeast1_21_6() {
        String version = Bukkit.getBukkitVersion();
        Matcher match = VERSION_PATTERN.matcher(version);
        boolean isCompatible = false;
        boolean isStandardVersion = false;
        
        if (match.find()) {
            isStandardVersion = true;
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
        
        if (!isStandardVersion) {
            // Check for alternative version format without 1. prefix, e.g., "26.1.2"
            Matcher alternativeMatch = ALTERNATIVE_VERSION_PATTERN.matcher(version);
            if (alternativeMatch.find()) {
                try {
                    int major = Integer.parseInt(alternativeMatch.group(1));
                    if (major >= MIN_COMPATIBLE_MAJOR_VERSION) {
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
