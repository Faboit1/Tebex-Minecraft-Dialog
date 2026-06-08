package io.tebex.plugin;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

public final class NeoForgePermissionNodes {
    public static final String BUY_PERMISSION = "tebex.buy";

    private static final Map<String, PermissionNode<Boolean>> NODES_BY_PERMISSION = new LinkedHashMap<>();

    static {
        registerPublic("base", "Base Tebex command access.");
        registerPublic("buy", "Use the Tebex buy command.");
        registerPublic("help", "View Tebex help.");
        registerPublic("info", "View Tebex store information.");
        registerPublic("checkout", "Create a Tebex checkout link for yourself.");
        registerPublic("goals", "View Tebex community goals.");

        registerAdmin("secret", "Connect the server to a Tebex store.");
        registerAdmin("reload", "Reload Tebex configuration and store data.");
        registerAdmin("forcecheck", "Force Tebex to check for pending commands.");
        registerAdmin("debug", "Toggle Tebex debug logging.");
        registerAdmin("sendlink", "Send a Tebex checkout link to another player.");
        registerAdmin("lookup", "Look up Tebex player transaction information.");
        registerAdmin("ban", "Ban a user from the Tebex webstore.");
    }

    private NeoForgePermissionNodes() {
    }

    private static void registerPublic(String nodeName, String description) {
        registerNode(nodeName, description, true);
    }

    private static void registerAdmin(String nodeName, String description) {
        registerNode(nodeName, description, false);
    }

    private static void registerNode(String nodeName, String description, boolean defaultValue) {
        PermissionNode<Boolean> node = new PermissionNode<>(
                TebexNeoForgePlugin.MOD_ID,
                nodeName,
                PermissionTypes.BOOLEAN,
                (player, playerUUID, context) -> {
                    if (defaultValue) {
                        return true;
                    }

                    return player != null && player.hasPermissions(4);
                }
        ).setInformation(
                Component.literal("Tebex " + nodeName),
                Component.literal(description)
        );

        NODES_BY_PERMISSION.put("tebex." + nodeName, node);
    }

    public static void register(PermissionGatherEvent.Nodes event) {
        event.addNodes(NODES_BY_PERMISSION.values().toArray(new PermissionNode<?>[0]));
    }

    public static boolean hasPermission(net.minecraft.server.level.ServerPlayer player, String permission) {
        PermissionNode<Boolean> node = NODES_BY_PERMISSION.get(permission);
        if (node == null) {
            return player.hasPermissions(4);
        }

        return PermissionAPI.getPermission(player, node);
    }
}
