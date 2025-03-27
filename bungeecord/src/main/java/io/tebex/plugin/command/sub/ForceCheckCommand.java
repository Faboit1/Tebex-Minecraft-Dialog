package io.tebex.plugin.command.sub;

import io.tebex.plugin.BungeePlatform;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

public class ForceCheckCommand extends SubCommand {
    private final BungeePlatform platform;

    public ForceCheckCommand(BungeePlatform platform) {
        super(platform, "forcecheck", "tebex.forcecheck");
        this.platform = platform;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(! platform.isSetup()) {
            sender.sendMessage(ChatColor.RED + "Tebex is not setup yet!");
            return;
        }

        sender.sendMessage("§b[Tebex] §7Performing force check..");
        getPlatform().performCheck();
    }

    @Override
    public String getDescription() {
        return "Rechecks for new purchases.";
    }
}
