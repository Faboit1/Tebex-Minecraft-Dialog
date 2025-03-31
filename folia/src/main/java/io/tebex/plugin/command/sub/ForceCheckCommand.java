package io.tebex.plugin.command.sub;

import io.tebex.plugin.FoliaPlatform;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import org.bukkit.command.CommandSender;

public class ForceCheckCommand extends SubCommand {
    private final FoliaPlatform platform;

    public ForceCheckCommand(FoliaPlatform platform) {
        super(platform, "forcecheck", "tebex.forcecheck");
        this.platform = platform;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(! platform.isSetup()) {
            sender.sendMessage("§cTebex is not setup yet!");
            return;
        }

        sender.sendMessage("§b[Tebex] §7Performing force check...");
        getPlatform().performCheck(false).thenAccept(sender::sendMessage);
    }

    @Override
    public String getDescription() {
        return "Checks immediately for new purchases.";
    }
}
