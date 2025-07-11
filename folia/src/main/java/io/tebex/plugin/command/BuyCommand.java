package io.tebex.plugin.command;

import io.tebex.plugin.FoliaPluginPlatform;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class BuyCommand extends Command {
    private final FoliaPluginPlatform platform;

    public BuyCommand(String command, FoliaPluginPlatform platform) {
        super(command);
        this.platform = platform;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if(! platform.isSetup()) {
            sender.sendMessage(ChatColor.RED + "Tebex is not setup yet!");
            return true;
        }

        return true;
    }
}
