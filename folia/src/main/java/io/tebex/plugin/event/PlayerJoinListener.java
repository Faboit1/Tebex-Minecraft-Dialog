package io.tebex.plugin.event;

import io.tebex.plugin.FoliaPlatform;
import io.tebex.sdk.obj.QueuedPlayer;
import io.tebex.sdk.obj.ServerEvent;
import io.tebex.sdk.obj.ServerEventType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final FoliaPlatform platform;

    public PlayerJoinListener(FoliaPlatform platform) {
        this.platform = platform;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Object playerId = platform.getPlayerId(player.getName(), player.getUniqueId());
        platform.getJoinEvents().add(new ServerEvent(player.getUniqueId().toString(), player.getName(), player.getAddress().getAddress().getHostAddress(), ServerEventType.JOIN));

        if(! platform.getQueuedPlayers().containsKey(playerId)) {
            return;
        }

        platform.handleOnlineCommands(new QueuedPlayer(platform.getQueuedPlayers().get(playerId), player.getName(), player.getUniqueId().toString()));
    }
}
