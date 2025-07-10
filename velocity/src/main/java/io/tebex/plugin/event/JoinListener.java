package io.tebex.plugin.event;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import io.tebex.plugin.TebexVelocityPlugin;
import io.tebex.sdk.obj.QueuedPlayer;

public class JoinListener {
    private final TebexVelocityPlugin plugin;

    public JoinListener(TebexVelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPlayerConnect(LoginEvent event) {
        Player player = event.getPlayer();

        Object playerId = plugin.getPlayerId(player.getUsername(), player.getUniqueId());

        if (!plugin.getQueuedPlayers().containsKey(playerId)) {
            return;
        }

        plugin.handleOnlineCommands(new QueuedPlayer(plugin.getQueuedPlayers().get(playerId), player.getUsername(), player.getUniqueId().toString()));
    }
}
