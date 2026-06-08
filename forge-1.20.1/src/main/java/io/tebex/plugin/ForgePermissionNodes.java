package io.tebex.plugin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ForgePermissionNodes {
    private static final Map<String, PermissionNode<Boolean>> NODES = new HashMap<>();

    public static final PermissionNode<Boolean> ADMIN = register("admin", ForgePermissionNodes::isOp);
    public static final PermissionNode<Boolean> BASE = register("base", (player) -> true);
    public static final PermissionNode<Boolean> BUY = register("buy", (player) -> true);
    public static final PermissionNode<Boolean> HELP = register("help", (player) -> true);
    public static final PermissionNode<Boolean> CHECKOUT = register("checkout", (player) -> true);
    public static final PermissionNode<Boolean> GOALS = register("goals", (player) -> true);

    public static final PermissionNode<Boolean> BAN = register("ban", ForgePermissionNodes::isOp);
    public static final PermissionNode<Boolean> DEBUG = register("debug", ForgePermissionNodes::isOp);
    public static final PermissionNode<Boolean> FORCECHECK = register("forcecheck", ForgePermissionNodes::isOp);
    public static final PermissionNode<Boolean> INFO = register("info", ForgePermissionNodes::isOp);
    public static final PermissionNode<Boolean> LOOKUP = register("lookup", ForgePermissionNodes::isOp);
    public static final PermissionNode<Boolean> RELOAD = register("reload", ForgePermissionNodes::isOp);
    public static final PermissionNode<Boolean> SECRET = register("secret", ForgePermissionNodes::isOp);
    public static final PermissionNode<Boolean> SENDLINK = register("sendlink", ForgePermissionNodes::isOp);

    private ForgePermissionNodes() {
    }

    public static Optional<PermissionNode<Boolean>> get(String permission) {
        return Optional.ofNullable(NODES.get(permission));
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(ForgePermissionNodes::registerNodes);
    }

    private static void registerNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(NODES.values().toArray(new PermissionNode<?>[0]));
    }

    private static PermissionNode<Boolean> register(String path, PermissionDefault permissionDefault) {
        PermissionNode<Boolean> node = new PermissionNode<>(
                TebexForgePlugin.MOD_ID,
                path,
                PermissionTypes.BOOLEAN,
                (player, playerUUID, context) -> permissionDefault.resolve(player)
        );
        NODES.put("tebex." + path, node);
        return node;
    }

    private static boolean isOp(ServerPlayer player) {
        return player != null && player.level().getServer().getPlayerList().isOp(player.getGameProfile());
    }

    @FunctionalInterface
    private interface PermissionDefault {
        boolean resolve(ServerPlayer player);
    }
}
