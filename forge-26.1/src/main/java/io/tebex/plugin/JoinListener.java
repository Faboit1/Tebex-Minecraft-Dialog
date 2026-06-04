package io.tebex.plugin;

import io.tebex.sdk.obj.QueuedPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class JoinListener {
    private final TebexForgePlugin plugin;

    private JoinListener(TebexForgePlugin plugin) {
        this.plugin = plugin;
    }

    public static void register(TebexForgePlugin plugin) {
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(new JoinListener(plugin)::onPlayerJoin);
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Object playerId = plugin.getPlatform().getPlayerId(player.getName().getString(), player.getUUID());
        plugin.getPlatform().createJoinEvent(player.getUUID().toString(), player.getName().getString(), player.getIpAddress());

        if (!plugin.getPlatform().getQueuedPlayers().containsKey(playerId)) {
            return;
        }

        plugin.getPlatform().handleOnlineCommands(new QueuedPlayer(plugin.getPlatform().getQueuedPlayers().get(playerId), player.getName().getString(), player.getUUID().toString()));
    }
}
