package io.tebex.plugin;

import io.tebex.sdk.obj.QueuedPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class JoinListener {
    private final TebexFabricPlugin plugin;

    public JoinListener(TebexFabricPlugin plugin) {
        this.plugin = plugin;
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onPlayerJoin(handler.player));
    }

    private void onPlayerJoin(ServerPlayerEntity player) {
        Object playerId = plugin.getPlatform().getPlayerId(player.getName().getString(), player.getUuid());
        plugin.getPlatform().createJoinEvent(player.getUuid().toString(), player.getName().getString(), player.getIp());

        if(! plugin.getPlatform().getQueuedPlayers().containsKey(playerId)) {
            return;
        }

        plugin.getPlatform().handleOnlineCommands(new QueuedPlayer(plugin.getPlatform().getQueuedPlayers().get(playerId), player.getName().getString(), player.getUuid().toString()));
    }
}
