package io.tebex.plugin.command.sub;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.BukkitPlatform;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.plugin.gui.BuyGUI;
import io.tebex.sdk.SDK;
import io.tebex.sdk.exception.ServerNotFoundException;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import org.bukkit.command.CommandSender;

import java.io.IOException;

public class SecretCommand extends SubCommand {
    public SecretCommand(BukkitPlatform platform) {
        super(platform, "secret", "tebex.setup");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length == 0) {
            sender.sendMessage("§b[Tebex] §7Usage: §f/tebex secret <key>");
            return;
        }

        String serverToken = args[0];
        BukkitPlatform platform = getPlatform();

        SDK sdk = platform.getSDK();
        ServerPlatformConfig config = (ServerPlatformConfig) platform.getPlatformConfig();
        YamlDocument configFile = config.getYamlDocument();

        sdk.setSecretKey(serverToken);

        platform.getSDK().getServerInformation().thenAccept(serverInformation -> {
            config.setSecretKey(serverToken);
            configFile.set("server.secret-key", serverToken);
            platform.setStoreInfo(serverInformation);

            try {
                configFile.save();
            } catch (IOException e) {
                sender.sendMessage("§b[Tebex] §7Failed to save config: " + e.getMessage());
            }

            platform.loadServerPlatformConfig(configFile);
            //TODO FIXME
//            platform.reloadConfig();
//            platform.setBuyGUI(new BuyGUI(platform));
            platform.refreshListings();
            platform.configure();

            sender.sendMessage("§b[Tebex] §7Connected to §b" + serverInformation.getServer().getName() + "§7.");
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause();

            if(cause instanceof ServerNotFoundException) {
                sender.sendMessage("§b[Tebex] §7Server not found. Please check your secret key.");
                platform.halt();
            } else {
                sender.sendMessage("§b[Tebex] §cAn error occurred: " + cause.getMessage());
                cause.printStackTrace();
            }

            return null;
        });
    }

    @Override
    public String getDescription() {
        return "Connects to your Tebex store.";
    }

    @Override
    public String getUsage() {
        return "<key>";
    }
}
