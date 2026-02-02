package io.tebex.plugin;

import io.tebex.sdk.obj.QueuedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class JoinListener implements Listener {
    private final BungeePluginPlatform platform;

    public JoinListener(BungeePluginPlatform platform) {
        this.platform = platform;
    }

    @EventHandler
    public void onPlayerConnect(LoginEvent event) {
        UUID uuid = event.getConnection().getUniqueId();
        String name = event.getConnection().getName();

        Object playerId = platform.getPlayerId(name, uuid);
        platform.createJoinEvent(uuid.toString(), name, event.getConnection().getAddress().getAddress().getHostAddress());

        if (!platform.getQueuedPlayers().containsKey(playerId)) {
            return;
        }

        platform.handleOnlineCommands(new QueuedPlayer(platform.getQueuedPlayers().get(playerId), name, uuid.toString()));
    }
}
