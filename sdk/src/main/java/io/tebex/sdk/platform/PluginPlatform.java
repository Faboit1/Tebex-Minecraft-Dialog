package io.tebex.sdk.platform;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.sdk.SDK;
import io.tebex.sdk.obj.QueuedPlayer;
import io.tebex.sdk.platform.config.IPlatformConfig;
import io.tebex.sdk.request.response.ServerInformation;
import io.tebex.sdk.util.CommandResult;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * The Platform interface defines the base methods required for interacting with a server platform.
 * Implementations should provide functionality specific to their platform, such as Bukkit or Sponge.
 */
public interface PluginPlatform {
    SDK getSDK();

    /**
     * @return The plugin's version string.
     */
    String getPluginVersion();

    /**
     * @return The directory where the plugin is running from as a File
     */
    File getRunningDirectory();

    void initStore();

    /**
     * Logs a message with the specified level.
     *
     * @param level   The level of the message.
     * @param message The message to log.
     */
    void log(Level level, String message);

    void error(String message, Throwable e);

    void debug(String message);

    /**
     * @return Information about the server we are connected as.
     */
    ServerInformation.Server getStoreServer();

    /**
     * @return Information about the store we're connected to.
     */
    ServerInformation.Store getStore();

    /**
     * Creates a new configuration file for this platform.
     * @return A reference to the platform's config file.
     */
    YamlDocument initPlatformConfig() throws IOException;

    /**
     * @return A configuration instance containing the config parameters for this platform.
     */
    IPlatformConfig getPlatformConfig();

    /**
     * Loads the platform configuration from file.
     */
    void loadPlatformConfig();

    /**
     * Refreshes any changes from the config file to the loaded config
     */
    void reloadConfig();

    /**
     * Saves the current platform config instance to file.
     *
     * @param platformConfig The config values to save.
     */
    void saveConfig(IPlatformConfig platformConfig);

    /**
     * @return True if we can send Tebex requests ( the secret key is set and valid )
     */
    boolean isSetup();

    /**
     * Queries Tebex for the most recent packages and categories, then saves them to cache.
     */
    void refreshListings();

    /**
     * Checks for any due online and offline commands and executes them if conditions are appropriate.
     *
     * @param useRemoteNextCheck True if we should reschedule the next check based on `next_check` received from the API.
     *                           Usually you should honor this to avoid rate limiting.
     *                           False if we are running a one-off check.
     * @return A CompletableFuture with any output as an array of strings.
     */
    CompletableFuture<String[]> checkCommandQueue(boolean useRemoteNextCheck);

    /**
     * Executes a command on the server.
     *
     * @param command The command to dispatch.
     */
    CommandResult dispatchCommand(String command);

    /**
     * @return True if the server is in online mode. This usually means we can verify users and usernames
     */
    boolean isOnlineMode();

    /**
     * Retrieves a player entity based on the provided UUID or username.
     *
     * @param uuidOrUsername The UUID or username of the player to retrieve.
     * @return The player entity.
     */
    <T> T getPlayer(Object uuidOrUsername);

    /**
     * @return True if the player is online.
     */
    default boolean isPlayerOnline(Object player) {
        return getPlayer(player) != null;
    }

    /**
     * Returns the UUID of an online player when available.
     *
     * @param playerName The player's username.
     * @return The online player's UUID, or null when unavailable.
     */
    default java.util.UUID getPlayerUniqueId(String playerName) {
        return null;
    }

    /**
     * Resolves the identifier that should fill {id} and {uuid} placeholders for a queued player.
     *
     * @param player The queued player record from Tebex.
     * @return The identifier to inject into queued commands.
     */
    default String resolveCommandPlayerId(QueuedPlayer player) {
        return player == null ? "" : player.getDefaultCommandIdentifier();
    }

    /**
     * @return Number of inventory slots free for the given player
     */
    int getFreeSlots(Object player);

    /**
     * Sends a checkout link to a player.
     *
     * @param playerName The player who should receive the link.
     * @param checkoutUrl The Tebex checkout URL.
     */
    default void sendCheckoutLink(String playerName, String checkoutUrl) {
        sendPlayerMessage(playerName, "Checkout started! Complete payment here: " + checkoutUrl);
    }

    void setServerInformation(ServerInformation serverInformation);

    /**
     * @return The PlatformTelemetry instance.
     */
    PlatformTelemetry getTelemetry();

    /**
     * Sends a message to the player. This should be implemented per-platform.
     *
     * @param playerName The player receiving the message. This should be their username - it is used to look up their Player instance
     * @param message The message to send.
     */
    void sendPlayerMessage(String playerName, String message);

    /**
     * Returns true if a given username has permission for a command. Permissions are usually implemented per-platform.
     *
     * @param username The player's username who is executing the command
     * @param permission The permission required to use the command (ex. tebex.info)
     * @return True if player has permission
     */
    boolean hasPermission(String username, String permission);

    PlatformType getType();
}
