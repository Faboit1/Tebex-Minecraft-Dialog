package io.tebex.plugin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.event.JoinListener;
import io.tebex.plugin.manager.CommandManager;
import io.tebex.sdk.platform.BasePlatform;
import io.tebex.sdk.util.Multithreading;
import io.tebex.sdk.util.TickScheduler;
import io.tebex.sdk.SDK;
import io.tebex.sdk.Tebex;
import io.tebex.sdk.obj.Category;
import io.tebex.sdk.obj.ServerEvent;
import io.tebex.sdk.placeholder.PlaceholderManager;
import io.tebex.sdk.platform.Platform;
import io.tebex.sdk.platform.PlatformTelemetry;
import io.tebex.sdk.platform.PlatformType;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import io.tebex.sdk.request.response.ServerInformation;
import io.tebex.sdk.util.CommandResult;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TebexPlugin extends BasePlatform implements DedicatedServerModInitializer {
    // Fabric Related
    private static final String MOD_ID = "tebex";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    private final String MOD_VERSION = "@VERSION@";
    private final File MOD_PATH = new File("./mods/" + MOD_ID);

    private MinecraftServer server;

    /**
     * Starts the Fabric platform.
     */
    @Override
    public void onInitializeServer() {
        try {
            // Load the platform config file.
            configYaml = initPlatformConfig();
            config = loadServerPlatformConfig(configYaml);
        } catch (IOException e) {
            warning("Failed to load configuration: " + e.getMessage(),
                    "Check that your configuration is valid and in the proper format and reload the plugin. You may delete `Tebex/config.yml` and a new configuration will be generated.");
            return;
        }

        // Register event hooks
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            onEnable();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            Multithreading.shutdown();
            sdk.shutdown();
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            fabricTick();
        });

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> new CommandManager(this).register(dispatcher));
    }

    private void fabricTick() {
        List<Runnable> runnableTasks = TickScheduler.tick();
        for (Runnable runnable : runnableTasks) {
            try {
                server.execute(runnable);
            } catch (Throwable t) {
                error("Failed to execute runnable task: ", t);
            }
        }
    }

    private void onEnable() {
        // Bind SDK.
        Tebex.init(this);

        // Initialise SDK.
        sdk = new SDK(this, config.getSecretKey());
        placeholderManager = new PlaceholderManager();
        queuedPlayers = Maps.newConcurrentMap();
        storeCategories = new ArrayList<>();
        serverEvents = new ArrayList<>();

        placeholderManager.registerDefaults();

        // Initialise the platform.
        init();

        new JoinListener(this);

        executeAsync(new Runnable() {
            @Override
            public void run() {
                if (!config.getSecretKey().isEmpty()) {
                    info("Loading store information...");
                    getSDK().getServerInformation()
                            .thenAccept(information -> storeInformation = information)
                            .exceptionally(error -> {
                                warning("Failed to load server information: " + error.getMessage(), "Please check that your secret key is valid.");
                                return null;
                            });
                    getSDK().getListing()
                            .thenAccept(listing -> storeCategories = listing)
                            .exceptionally(error -> {
                                warning("Failed to load store categories: " + error.getMessage(), "Please check that your secret key is valid.");
                                return null;
                            });
                }
            }
        });

        Multithreading.executeAsync(() -> {
            getSDK().getServerInformation().thenAccept(information -> storeInformation = information);
            getSDK().getListing().thenAccept(listing -> storeCategories = listing);
        }, 0, 30, TimeUnit.MINUTES);

        Multithreading.executeAsync(() -> {
            getSDK().sendPluginEvents();
        }, 0, 10, TimeUnit.MINUTES);

        Multithreading.executeAsync(() -> {
            List<ServerEvent> runEvents = Lists.newArrayList(serverEvents.subList(0, Math.min(serverEvents.size(), 750)));
            if (runEvents.isEmpty()) return;

            sdk.sendJoinEvents(runEvents)
                    .thenAccept(aVoid -> {
                        serverEvents.removeAll(runEvents);
                        debug("Successfully sent join events");
                    })
                    .exceptionally(throwable -> {
                        error("Failed to send join events: " + throwable.getMessage(), throwable);
                        return null;
                    });
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public PlatformType getType() {
        return PlatformType.FABRIC;
    }

    @Override
    public File getDirectory() {
        return MOD_PATH;
    }

    @Override
    public boolean isOnlineMode() {
        ServerPlatformConfig serverConfig = (ServerPlatformConfig) getPlatformConfig();
        return serverConfig.isProxyMode() || server.isOnlineMode();
    }

    @Override
    public CommandResult dispatchCommand(String command) {
        server.getCommandManager().execute(server.getCommandSource().getDispatcher().parse(command, server.getCommandSource()), command);
        return CommandResult.from(true); // we assume success because the command manager does not report any result
    }

    private Optional<ServerPlayerEntity> getPlayer(Object player) {
        if(player == null) return Optional.empty();

        if (isOnlineMode() && !isGeyser() && player instanceof UUID) {
            return Optional.ofNullable(server.getPlayerManager().getPlayer((UUID) player));
        }

        return Optional.ofNullable(server.getPlayerManager().getPlayer((String) player));
    }

    @Override
    public boolean isPlayerOnline(Object player) {
        return getPlayer(player).isPresent();
    }

    @Override
    public int getFreeSlots(Object playerId) {
        ServerPlayerEntity player = getPlayer(playerId).orElse(null);
        if (player == null) return -1;

        DefaultedList<ItemStack> inv = player.getInventory().main;
        return (int) inv.stream()
                .filter(obj -> obj == null || obj.isEmpty())
                .count();
    }

    @Override
    public String getVersion() {
        return MOD_VERSION;
    }

    @Override
    public void log(Level level, String message) {
        if(level == Level.INFO) {
            LOGGER.info(message);
        } else if(level == Level.WARNING) {
            LOGGER.warn(message);
        } else if(level == Level.SEVERE) {
            LOGGER.error(message);
        } else {
            LOGGER.info(message);
        }
    }

    @Override
    public PlatformTelemetry getTelemetry() {
        String serverVersion = server.getVersion();

        Pattern pattern = Pattern.compile("MC: (\\d+\\.\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(serverVersion);
        if (matcher.find()) {
            serverVersion = matcher.group(1);
        }

        return new PlatformTelemetry(
                getVersion(),
                server.getName(),
                serverVersion,
                System.getProperty("java.version"),
                System.getProperty("os.arch"),
                server.isOnlineMode()
        );
    }
}
