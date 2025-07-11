package io.tebex.plugin;

import com.google.common.collect.Lists;
import io.tebex.plugin.command.BuyCommand;
import io.tebex.plugin.command.TebexCommandExecutor;
import io.tebex.plugin.event.InventoryClickListener;
import io.tebex.plugin.event.PlayerJoinListener;
import io.tebex.plugin.placeholder.BukkitNamePlaceholder;
import io.tebex.sdk.Tebex;
import io.tebex.sdk.obj.ServerEvent;
import io.tebex.sdk.placeholder.PlaceholderManager;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.*;

public final class TebexFoliaPlugin extends JavaPlugin {
    private FoliaPluginPlatform platform;

    public FoliaPluginPlatform getPlatform() {
        return platform;
    }

    /**
     * Starts the Folia platform.
     */
    @Override
    public void onEnable() {
        platform = new FoliaPluginPlatform(this);
        Tebex.init(platform);

        platform.loadPlatformConfig(); // loads the configuration file for the platform

        platform.initStore(); // uses loaded key to set current store and cache the available packages

        // Bukkit specific registration
        TebexCommandExecutor tebexCommands = new TebexCommandExecutor(platform);
        PluginCommand pluginCommand = platform.getPlugin().getCommand("tebex");
        if (pluginCommand == null) {
            throw new RuntimeException("Tebex command not found.");
        }
        pluginCommand.setExecutor(tebexCommands);
        pluginCommand.setTabCompleter(tebexCommands);

        registerBuyCommand();
        registerEvents(new PlayerJoinListener(platform));
        registerEvents(new InventoryClickListener());

        PlaceholderManager placeholderManager = platform.getPlaceholderManager();
        placeholderManager.register(new BukkitNamePlaceholder(placeholderManager));

        // Refresh store listings every 5 minutes
        getServer().getScheduler().runTaskTimerAsynchronously(this, platform::refreshListings, 0, 20 * 60 * 5);

        // Every 10 minutes clear the plugin event queue
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            platform.getSDK().sendPluginEvents();
        }, 0, 60 * 20 * 10);

        // clear server events every minute
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            List<ServerEvent> allServerEvents = platform.getJoinEvents();
            List<ServerEvent> runEvents = Lists.newArrayList(allServerEvents.subList(0, Math.min(allServerEvents.size(), 750)));
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
        }, 0, 20 * 60);
    }

    /**
     * Registers the specified listener with the plugin manager.
     * @param l the listener to register
     */
    public <T extends Listener> void registerEvents(T l) {
        getServer().getPluginManager().registerEvents(l, this);
    }

    public void registerBuyCommand() {
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            ServerPlatformConfig config = (ServerPlatformConfig) platform.getPlatformConfig();
            if (config.isBuyCommandEnabled()) {
                commandMap.register(config.getBuyCommandName(), new BuyCommand(config.getBuyCommandName(), platform));
            }
        } catch (Throwable e) {
            platform.error("Failed to register buy command: " + e.getMessage(), e);
        }
    }

    public Player getPlayer(Object player) {
        if(player == null) return null;

        if (platform.isOnlineMode() && !platform.isGeyser() && player instanceof UUID) {
            return getServer().getPlayer((UUID) player);
        }

        return getServer().getPlayerExact((String) player);
    }

}
