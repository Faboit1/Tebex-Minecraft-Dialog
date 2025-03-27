package io.tebex.plugin.command.sub;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.BukkitPlatform;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.plugin.gui.BuyGUI;
import org.bukkit.command.CommandSender;

import java.io.IOException;

public class ReloadCommand extends SubCommand {
    public ReloadCommand(BukkitPlatform platform) {
        super(platform, "reload", "tebex.admin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        BukkitPlatform platform = getPlatform();
            //TODO
//            YamlDocument configYaml = platform.initPlatformConfig();
//            platform.loadServerPlatformConfig(configYaml);
//            platform.reloadConfig();
//            platform.setBuyGUI(new BuyGUI(platform));
//            platform.refreshListings();
//            platform.registerBuyCommand();
//            platform.getSDK().sendPluginEvents();

            sender.sendMessage("§8[Tebex] §7Successfully reloaded.");
    }

    @Override
    public String getDescription() {
        return "Reloads the plugin.";
    }
}
