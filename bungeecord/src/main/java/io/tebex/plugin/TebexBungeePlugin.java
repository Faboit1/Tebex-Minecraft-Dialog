package io.tebex.plugin;

import com.google.common.collect.Lists;
import io.tebex.sdk.Tebex;
import io.tebex.sdk.commands.TebexCommands;
import io.tebex.sdk.obj.ServerEvent;
import io.tebex.sdk.placeholder.PlaceholderManager;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class TebexBungeePlugin extends Plugin {
    private BungeePluginPlatform platform;

    @Override
    public void onEnable() {
        platform = new BungeePluginPlatform(this);
        Tebex.init(platform);

        platform.loadPlatformConfig(); // loads the platform configuration

        platform.initStore(); // Used loaded key to set current store and cache available packages

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

        // Clear server events every minute
        getProxy().getScheduler().schedule(this, () -> {
            List<ServerEvent> allServerEvents = platform.getJoinEvents();
            List<ServerEvent> runEvents;
            synchronized (allServerEvents) {
                runEvents = Lists.newArrayList(allServerEvents.subList(0, Math.min(allServerEvents.size(), 750)));
            }
            if (runEvents.isEmpty()) return;
            if (!platform.isSetup()) return;

            platform.getSDK().sendJoinEvents(runEvents)
                    .thenAccept(aVoid -> {
                        platform.clearSelectedPluginEvents(runEvents);
                        platform.debug("Successfully sent join events.");
                    })
                    .exceptionally(throwable -> {
                        platform.debug("Failed to send join events: " + throwable.getMessage());
                        return null;
                    });
        }, 0, 1, TimeUnit.MINUTES);
    }

    private ProxiedPlayer getPlayer(Object player) {
        if(player == null) return null;

        if (platform.isOnlineMode() && !platform.isGeyser() && player instanceof UUID) {
            return getProxy().getPlayer((UUID) player);
        }

        return getProxy().getPlayer((String) player);
    }
}
