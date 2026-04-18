package io.tebex.plugin;

import io.tebex.plugin.gui.BuyGUI;
import io.tebex.sdk.platform.BasePluginPlatform;
import io.tebex.sdk.platform.PlatformTelemetry;
import io.tebex.sdk.platform.PlatformType;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import io.tebex.sdk.util.CommandResult;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoForgePluginPlatform extends BasePluginPlatform {
    private BuyGUI buyGUI;
    private final TebexNeoForgePlugin plugin;

    public static final Logger LOGGER = LogManager.getLogger(TebexNeoForgePlugin.MOD_ID);
    private final File MOD_PATH = new File("./mods/" + TebexNeoForgePlugin.MOD_ID);

    private MinecraftServer server;

    public NeoForgePluginPlatform(TebexNeoForgePlugin plugin) {
        this.plugin = plugin;
    }

    public void initBuyGui() {
        buyGUI = new BuyGUI(this);
    }

    public MinecraftServer getServer() {
        return server;
    }

    public void setMinecraftServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public PlatformType getType() {
        return PlatformType.NEOFORGE;
    }

    @Override
    public File getRunningDirectory() {
        return MOD_PATH;
    }

    @Override
    public boolean isOnlineMode() {
        ServerPlatformConfig serverConfig = (ServerPlatformConfig) getPlatformConfig();
        return serverConfig.isProxyMode() || server.usesAuthentication();
    }

    @Override
    public CommandResult dispatchCommand(String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
        return CommandResult.from(true); // we assume success because the command manager does not report any result
    }

    @Override
    public <T> T getPlayer(Object uuidOrUsername) {
        if(uuidOrUsername == null) return null;

        if (isOnlineMode() && !isGeyser() && uuidOrUsername instanceof UUID) {
            return (T)server.getPlayerList().getPlayer((UUID) uuidOrUsername);
        }

        return (T)(server.getPlayerList().getPlayerByName((String) uuidOrUsername));
    }

    @Override
    public int getFreeSlots(Object playerId) {
        ServerPlayer player = getPlayer(playerId);
        if (player == null) return -1;

        NonNullList<ItemStack> inv = player.getInventory().getNonEquipmentItems();
        return (int) inv.stream()
                .filter(obj -> obj == null || obj.isEmpty())
                .count();
    }

    @Override
    public String getPluginVersion() {
        return "@VERSION@";
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
        String serverVersion = server.getServerVersion();

        Pattern pattern = Pattern.compile("MC: (\\d+\\.\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(serverVersion);
        if (matcher.find()) {
            serverVersion = matcher.group(1);
        }

        return new PlatformTelemetry(
                getPluginVersion(),
                getType().toString(),
                serverVersion,
                System.getProperty("java.version"),
                System.getProperty("os.arch"),
                server.usesAuthentication()
        );
    }

    @Override
    public void sendPlayerMessage(String playerName, String message) {
        ServerPlayer player = getPlayer(playerName);
        if (player != null){
            player.sendSystemMessage(Component.nullToEmpty(message));
        }
    }

    @Override
    public boolean hasPermission(String username, String permission) {
        ServerPlayer player = getPlayer(username);
        if (player == null) return false;
        return NeoForgePermissionNodes.hasPermission(player, permission);
    }
}
