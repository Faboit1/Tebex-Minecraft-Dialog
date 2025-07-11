package io.tebex.plugin.command;

import io.tebex.plugin.BukkitPluginPlatform;
import io.tebex.plugin.gui.BuyGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuyCommand extends Command {
    private final BukkitPluginPlatform platform;

    public BuyCommand(String command, BukkitPluginPlatform platform) {
        super(command);
        this.platform = platform;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if(! platform.isSetup()) {
            sender.sendMessage(ChatColor.RED + "Tebex is not setup yet!");
            return true;
        }

        if (sender instanceof Player) {
            new BuyGUI(platform).open((Player) sender);
            return true;
        }

        sender.sendMessage(ChatColor.RED + "The buy command cannot be used from the console.");
        return false;
    }
}
