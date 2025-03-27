package io.tebex.plugin;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.ProxyVersion;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.event.JoinListener;
import io.tebex.plugin.manager.CommandManager;
import io.tebex.sdk.SDK;
import io.tebex.sdk.Tebex;
import io.tebex.sdk.obj.Category;
import io.tebex.sdk.placeholder.PlaceholderManager;
import io.tebex.sdk.platform.BasePlatform;
import io.tebex.sdk.platform.Platform;
import io.tebex.sdk.platform.PlatformTelemetry;
import io.tebex.sdk.platform.PlatformType;
import io.tebex.sdk.platform.config.ProxyPlatformConfig;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import io.tebex.sdk.request.response.ServerInformation;
import io.tebex.sdk.util.CommandResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin(
        id = "tebex",
        name = "Tebex",
        version = Constants.VERSION,
        description = "The Velocity plugin for Tebex.",
        url = "https://tebex.io",
        authors = {"Tebex"}
)
public class TebexPlugin extends BasePlatform {
    protected ProxyPlatformConfig config;

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public TebexPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        Tebex.init(this);

        load(); // load config file for the platform

        init(); // use loaded key to set current store

        // Velocity specific
        new CommandManager(this).register();
        placeholderManager.registerDefaults();
        proxy.getEventManager().register(this, new JoinListener(this));
        proxy.getScheduler()
                .buildTask(this, () -> {
                    getSDK().getServerInformation().thenAccept(information -> storeInformation = information);
                    getSDK().getListing().thenAccept(listing -> storeCategories = listing);
                })
                .repeat(5, TimeUnit.MINUTES)
                .delay(0, TimeUnit.MINUTES)
                .schedule();
    }

    @Override
    public PlatformType getType() {
        return PlatformType.VELOCITY;
    }

    @Override
    public File getDirectory() {
        return dataDirectory.toFile();
    }

    @Override
    public boolean isOnlineMode() {
        return proxy.getConfiguration().isOnlineMode();
    }

    @Override
    public CommandResult dispatchCommand(String command) {
        proxy.getCommandManager().executeAsync(proxy.getConsoleCommandSource(), command);
        return CommandResult.from(true); // no additional information from commandManager so we assume success
    }

    @Override
    public void executeAsync(Runnable runnable) {
        proxy.getScheduler()
                .buildTask(this, runnable)
                .schedule();
    }

    @Override
    public void executeAsyncLater(Runnable runnable, long time, TimeUnit unit) {
        proxy.getScheduler()
                .buildTask(this, runnable)
                .delay(time, unit)
                .schedule();
    }

    @Override
    public void executeBlocking(Runnable runnable) {
        // Velocity has no concept of "blocking"
        executeAsync(runnable);
    }

    @Override
    public void executeBlockingLater(Runnable runnable, long time, TimeUnit unit) {
        // Velocity has no concept of "blocking"
        executeAsyncLater(runnable, time, unit);
    }

    private Optional<Player> getPlayer(Object player) {
        if(player == null) return Optional.empty();

        if (isOnlineMode() && !isGeyser() && player instanceof UUID) {
            return proxy.getPlayer((UUID) player);
        }

        return proxy.getPlayer((String) player);
    }

    @Override
    public boolean isPlayerOnline(Object player) {
        return getPlayer(player).isPresent();
    }

    @Override
    public int getFreeSlots(Object player) {
        // Bungee has no concept of an inventory
        return 0;
    }

    @Override
    public String getVersion() {
        return Constants.VERSION;
    }

    @Override
    public void log(Level level, String message) {
        logger.log(level, message);
    }

    @Override
    public PlatformTelemetry getTelemetry() {
        ProxyVersion proxyVersion = proxy.getVersion();
        String serverVersion = proxyVersion.getVersion();

        Pattern pattern = Pattern.compile("MC: (\\d+\\.\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(serverVersion);
        if (matcher.find()) {
            serverVersion = matcher.group(1);
        }

        return new PlatformTelemetry(
                getVersion(),
                proxyVersion.getName(),
                serverVersion,
                System.getProperty("java.version"),
                System.getProperty("os.arch"),
                proxy.getConfiguration().isOnlineMode()
        );
    }
}
