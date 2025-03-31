package io.tebex.plugin.command;

import io.tebex.plugin.BukkitPlatform;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuyCommand extends Command {
    private final BukkitPlatform platform;

    public BuyCommand(String command, BukkitPlatform platform) {
        super(command);
        this.platform = platform;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if(! platform.isSetup()) {
            sender.sendMessage(ChatColor.RED + "Tebex is not setup yet!");
            return true;
        }

        return true;
    }
}
