package io.tebex.plugin;

import io.tebex.sdk.Tebex;
import io.tebex.sdk.commands.TebexCommands;
import io.tebex.sdk.placeholder.PlaceholderManager;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class TebexPlugin extends Plugin {
    private BungeePlatform platform;

    @Override
    public void onEnable() {
        platform = new BungeePlatform(this);
        Tebex.init(platform);

        platform.load(); // loads the platform configuration

        platform.init(); // Used loaded key to set current store and cache available packages

        // Bungee-specific registration
        TebexCommands.setRestrictedToCommands("help", "forcecheck", "reload", "secret", "debug");
        TebexCommandExecutor tebexCommand = new TebexCommandExecutor(platform);
        PluginManager pluginManager = platform.getPlugin().getProxy().getPluginManager();
        pluginManager.registerCommand(platform.getPlugin(), tebexCommand);

        PlaceholderManager placeholderManager = platform.getPlaceholderManager();
        placeholderManager.registerDefaults();

        getProxy().getPluginManager().registerListener(this, new JoinListener(platform));

        getProxy().getScheduler().schedule(this, () -> {
            platform.getSDK().getServerInformation().thenAccept(information -> platform.setStoreInfo(information));
            platform.getSDK().getListing().thenAccept(listing -> platform.setStoreCategories(listing));
        }, 0, 5, TimeUnit.MINUTES);
    }

    private ProxiedPlayer getPlayer(Object player) {
        if(player == null) return null;

        if (platform.isOnlineMode() && !platform.isGeyser() && player instanceof UUID) {
            return getProxy().getPlayer((UUID) player);
        }

        return getProxy().getPlayer((String) player);
    }
}
