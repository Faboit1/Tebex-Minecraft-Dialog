package io.tebex.plugin;

import com.google.common.collect.Lists;
import io.tebex.plugin.command.TebexCommandExecutor;
import io.tebex.sdk.Tebex;
import io.tebex.sdk.obj.ServerEvent;
import io.tebex.sdk.util.Multithreading;
import io.tebex.sdk.util.TickScheduler;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;

@Mod(TebexNeoForgePlugin.MOD_ID)
public class TebexNeoForgePlugin {
    public static final String MOD_ID = "tebex";

    private final NeoForgePluginPlatform platform;
    private final JoinListener joinListener;

    public TebexNeoForgePlugin(IEventBus modEventBus) {
        platform = new NeoForgePluginPlatform(this);
        joinListener = new JoinListener(this);

        Tebex.init(platform);
        platform.loadPlatformConfig();

        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onRegisterPermissionNodes);
    }

    public NeoForgePluginPlatform getPlatform() {
        return platform;
    }

    private void onServerStarted(ServerStartedEvent event) {
        platform.setMinecraftServer(event.getServer());
        onEnableNeoForge();
    }

    private void onServerStopping(ServerStoppingEvent event) {
        Multithreading.shutdown();
        if (platform.getSDK() != null) {
            platform.getSDK().shutdown();
        }
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        neoForgeTick();
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        platform.debug("registering commands");
        new TebexCommandExecutor(platform).register(event.getDispatcher());
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            joinListener.onPlayerJoin(player);
        }
    }

    private void onRegisterPermissionNodes(PermissionGatherEvent.Nodes event) {
        NeoForgePermissionNodes.register(event);
    }

    private void neoForgeTick() {
        List<Runnable> runnableTasks = TickScheduler.tick();
        for (Runnable runnable : runnableTasks) {
            try {
                getPlatform().getServer().execute(runnable);
            } catch (Throwable t) {
                getPlatform().error("Failed to execute runnable task: ", t);
            }
        }
    }

    private void onEnableNeoForge() {
        platform.initStore();
        platform.initBuyGui();

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
