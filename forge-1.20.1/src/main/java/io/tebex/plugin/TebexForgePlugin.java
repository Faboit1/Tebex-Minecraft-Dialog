package io.tebex.plugin;

import com.google.common.collect.Lists;
import io.tebex.plugin.command.TebexCommandExecutor;
import io.tebex.sdk.Tebex;
import io.tebex.sdk.obj.ServerEvent;
import io.tebex.sdk.util.Multithreading;
import io.tebex.sdk.util.TickScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Mod(TebexForgePlugin.MOD_ID)
public final class TebexForgePlugin {
    public static final String MOD_ID = "tebex";

    private final ForgePluginPlatform platform;

    public TebexForgePlugin() {
        platform = new ForgePluginPlatform(this);
        Tebex.init(platform);
        platform.loadPlatformConfig();

        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        ForgePermissionNodes.register();
    }

    public ForgePluginPlatform getPlatform() {
        return platform;
    }

    public void onRegisterCommands(RegisterCommandsEvent event) {
        platform.debug("registering commands");
        new TebexCommandExecutor(platform).register(event.getDispatcher());
    }

    public void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        platform.setMinecraftServer(server);
        onEnableForge();
    }

    public void onServerStopping(ServerStoppingEvent event) {
        Multithreading.shutdown();
        if (platform.getSDK() != null) {
            platform.getSDK().shutdown();
        }
    }

    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        List<Runnable> runnableTasks = TickScheduler.tick();
        for (Runnable runnable : runnableTasks) {
            try {
                platform.getServer().execute(runnable);
            } catch (Throwable t) {
                platform.error("Failed to execute runnable task: ", t);
            }
        }
    }

    private void onEnableForge() {
        platform.initStore();
        platform.initBuyGui();
        JoinListener.register(this);

        Multithreading.executeAsync(platform::refreshListings, 0, 5, TimeUnit.MINUTES);
        Multithreading.executeAsync(() -> platform.getSDK().sendPluginEvents(), 0, 10, TimeUnit.MINUTES);
        Multithreading.executeAsync(() -> {
            List<ServerEvent> allServerEvents = platform.getJoinEvents();
            List<ServerEvent> runEvents;
            synchronized (allServerEvents) {
                runEvents = Lists.newArrayList(allServerEvents.subList(0, Math.min(allServerEvents.size(), 750)));
            }
            if (runEvents.isEmpty() || !platform.isSetup()) {
                return;
            }

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
}
