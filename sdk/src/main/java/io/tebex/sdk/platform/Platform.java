package io.tebex.sdk.platform;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.sdk.platform.config.IPlatformConfig;
import io.tebex.sdk.request.response.ServerInformation;
import io.tebex.sdk.util.CommandResult;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * The Platform interface defines the base methods required for interacting with a server platform.
 * Implementations should provide functionality specific to their platform, such as Bukkit or Sponge.
 */
public interface Platform {
    /**
     * @return The version string of the platform implementation (plugin version).
     */
    String getVersion();

    /**
     * @return A PlatformType that represents server platform's framework (BungeeCord, Bukkit, etc.)
     */
    PlatformType getType();

    /**
     * @return The directory where the plugin is running from as a File
     */
    File getDirectory();

    /**
     * Logs a message with the specified level.
     *
     * @param level   The level of the message.
     * @param message The message to log.
     */
    void log(Level level, String message);

    void error(String message, Throwable e);

    void debug(String message);

    ServerInformation.Server getStoreServer();

    ServerInformation.Store getStore();

    IPlatformConfig getPlatformConfig();

    boolean isSetup();

    void refreshListings();

    void performCheck(boolean runAfter);

    YamlDocument initPlatformConfig() throws IOException;

    /**
     * Executes a command on the server.
     *
     * @param command The command to dispatch.
     */
    CommandResult dispatchCommand(String command);

    /**
     * @return True if the server is in online mode.
     */
    boolean isOnlineMode();

    /**
     * @return True if the player is online.
     */
    boolean isPlayerOnline(Object player);

    /**
     * @return Number of inventory slots free for the given player
     */
    int getFreeSlots(Object player);

    /**
     * @return The PlatformTelemetry instance.
     */
    PlatformTelemetry getTelemetry();

    void load();
}
