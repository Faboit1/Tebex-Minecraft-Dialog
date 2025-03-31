package io.tebex.plugin.command;

import io.tebex.plugin.FoliaPlatform;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class BuyCommand extends Command {
    private final FoliaPlatform platform;

    public BuyCommand(String command, FoliaPlatform platform) {
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
