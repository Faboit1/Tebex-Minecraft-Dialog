package io.tebex.sdk.platform;

import com.google.common.collect.Maps;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.sdk.SDK;
import io.tebex.sdk.exception.ServerNotFoundException;
import io.tebex.sdk.obj.*;
import io.tebex.sdk.placeholder.PlaceholderManager;
import io.tebex.sdk.placeholder.defaults.UuidPlaceholder;
import io.tebex.sdk.platform.config.IPlatformConfig;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import io.tebex.sdk.request.response.ServerInformation;
import io.tebex.sdk.triage.EnumEventLevel;
import io.tebex.sdk.triage.PluginEvent;
import io.tebex.sdk.util.*;
import org.jetbrains.annotations.NotNull;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.util.LinkedPlayer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import static io.tebex.sdk.util.ResourceUtil.getBundledFile;

/**
 * BasePlatform class represents the foundational platform for basic Tebex functionality.
 */
public abstract class BasePluginPlatform implements PluginPlatform {
    public final int MAX_COMMANDS_PER_BATCH = 3;

    public static final String DEFAULT_CHECKOUT_MESSAGE = "<green>Checkout started! Complete payment here: <yellow>%url%";
    public static final String DEFAULT_BEDROCK_CHECKOUT_MESSAGE = "<green>Checkout started! Type this link into your browser: <yellow>%url%";
    public static final String DEFAULT_FREE_REMINDER_MESSAGE = "<green>You have free items waiting! Use <yellow>/buy<green> to claim them.";

    protected SDK sdk;
    protected ServerPlatformConfig config;
    protected YamlDocument configYaml;

    protected boolean setup;
    protected PlaceholderManager placeholderManager;
    protected Map<Object, Integer> queuedPlayers;

    protected ServerInformation storeInformation;
    protected List<Category> storeCategories = new ArrayList<>();
    protected List<ServerEvent> serverEvents = Collections.synchronizedList(new ArrayList<>());

    private final ArrayList<PluginEvent> PLUGIN_EVENTS = new ArrayList<>();
    private final AtomicBoolean commandCheckInProgress = new AtomicBoolean(false);
    private final AtomicBoolean floodgateWarningLogged = new AtomicBoolean(false);
    private final Map<String, UUID> resolvedFloodgateIds = Maps.newConcurrentMap();

    /**
     * Checks if the configured store is Geyser/Offline
     *
     * @return Whether the store is an Offline/Geyser-type webstore
     */
    public final boolean isGeyser() {
        if (!isSetup()) return false;

        String storeType = getStoreType();
        if (storeType == null || storeType.isEmpty()) {
            return false;
        }

        return storeType.toLowerCase(Locale.ROOT).contains("geyser");
    }

    public final void initStore() {
        sdk = new SDK(this, config.getSecretKey());
        placeholderManager = new PlaceholderManager();
        queuedPlayers = Maps.newConcurrentMap();
        resolvedFloodgateIds.clear();
        storeCategories = new ArrayList<>();
        serverEvents = Collections.synchronizedList(new ArrayList<>());
        placeholderManager.register(new UuidPlaceholder(placeholderManager));

        if (getPlatformConfig().getSecretKey() != null && !getPlatformConfig().getSecretKey().isEmpty()) {
            getSDK().getServerInformation().thenAccept(serverInformation -> {
                ServerInformation.Server server = serverInformation.getServer();
                ServerInformation.Store store = serverInformation.getStore();

                info(String.format("Connected to %s - %s server.", server.getName(), store.getGameType()));
                createPluginEvent(EnumEventLevel.INFO, "Server init");
                setStoreInfo(serverInformation);
                setSetup(true);
                configure();
                refreshListings();

                // Start the initial check, which is rescheduled per each remote next check
                checkCommandQueue(true);
            }).exceptionally(ex -> {
                Throwable cause = ex.getCause();
                setSetup(false);

                if (cause instanceof ServerNotFoundException) {
                    warning("Failed to connect your server: " + cause.getMessage(), "Please double-check your server key or run the setup command again.");
                    this.halt();
                } else {
                    warning("Failed to retrieve server information. " + cause.getMessage(), "Please double check your server key or run the setup command again.", ex);
                }

                return null;
            });
        } else {
            info("Welcome to Tebex! It seems like this is a new setup.");
            info("To get started, please use the 'tebex secret <key>' command in the console.");
        }
    }

    public final void performCheck() {
        checkCommandQueue(true);
    }

    public final CompletableFuture<String[]> checkCommandQueue(boolean useRemoteNextCheck) {
        CompletableFuture<String[]> forceCheckOutput = new CompletableFuture<>();

        int checkInterval = getCheckIntervalSeconds();

        if(!isSetup()) {
            debug("Tebex is not set up. Skipping check.");
            executeAsyncLater(this::performCheck, checkInterval, TimeUnit.SECONDS);
            return forceCheckOutput;
        }

        if (!commandCheckInProgress.compareAndSet(false, true)) {
            debug("Command queue check already in progress. Skipping to avoid duplicate execution.");
            if (useRemoteNextCheck) {
                executeAsyncLater(this::performCheck, checkInterval, TimeUnit.SECONDS);
            }
            return forceCheckOutput;
        }

        debug("Checking for due players...");
        getQueuedPlayers().clear();

        getSDK().getDuePlayers().whenComplete((duePlayersResponse, ex) -> {
            ArrayList<String> output = new ArrayList<>();
            if (ex != null) {
                if (ex.getMessage().contains("429")) { // handling for rate limits
                    warning("Failed to get due players: Rate Limit", "We will try again after 5 minutes.", ex);
                    output.add("Failed to get due players: Rate Limit. We will try again after 5 minutes.");
                    executeAsyncLater(this::performCheck, 5, TimeUnit.MINUTES);
                } else if (ex.getMessage().contains("403")) {
                    warning("Failed to get due players: Forbidden", "Please check your secret key and run `/tebex.forcecheck` to try again. We will wait 30 minutes before trying again.", ex);
                    output.add("Failed to get due players: Forbidden. Please check your secret key and run `/tebex.forcecheck` to try again. We will wait 30 minutes before trying again.");
                    executeAsyncLater(this::performCheck, 30, TimeUnit.MINUTES);
                } else { // unexpected status code
                    warning("Failed to get due players: " + ex.getMessage(), "We will try again at the next due player check.", ex);
                    output.add("Failed to get due players: '" + ex.getMessage() + "'. We will try again at the next due player check.");
                    executeAsyncLater(this::performCheck, 1, TimeUnit.MINUTES);
                }
                commandCheckInProgress.set(false);
                forceCheckOutput.complete((String[]) output.toArray());
                return;
            }

            if (useRemoteNextCheck) {
                executeAsyncLater(this::performCheck, checkInterval, TimeUnit.SECONDS);
            }

            List<QueuedPlayer> playerList = duePlayersResponse.getPlayers();
            List<CompletableFuture<Void>> onlineCommandFutures = new ArrayList<>();
            if(! playerList.isEmpty()) {
                String listMessage = "Found " + playerList.size() + " " + StringUtil.pluralise(playerList.size(), "player", "players") + " with pending commands.";

                for (QueuedPlayer queuedPlayer : playerList) {
                    try {
                        onlineCommandFutures.add(handleOnlineCommands(queuedPlayer));
                    } catch (Exception e) {
                        error("Failed to handle online commands for player '" + queuedPlayer.getName() + "': " + e.getMessage(), e);
                    }
                }
            }

            // Hold the flag until all per-player command fetches settle. Resetting it when
            // getDuePlayers returns lets a concurrent forcecheck re-fetch and re-dispatch the
            // same commands before they are deleted.
            CompletableFuture.allOf(onlineCommandFutures.toArray(new CompletableFuture[0]))
                .whenComplete((v, allEx) -> {
                    if (duePlayersResponse.isExecuteOffline()) {
                        handleOfflineCommands();
                    }
                    commandCheckInProgress.set(false);
                });
        });

        return forceCheckOutput;
    }

    public final CompletableFuture<Void> handleOnlineCommands(QueuedPlayer player) {
        if(! isSetup()) return CompletableFuture.completedFuture(null);

        debug("Processing online commands for player '" + player.getName() + "'...");
        Object playerId = getPlayerId(player.getName(), UUIDUtil.mojangIdToJavaId(player.getUuid()));
        if(!canProcessOnlineCommands(player, playerId)) {
            getQueuedPlayers().put(playerId, player.getId()); // will cause commands to be processed when player connects
            return CompletableFuture.completedFuture(null);
        }

        return getSDK().getOnlineCommands(player).thenAccept(onlineCommands -> {
            if(onlineCommands.isEmpty()) {
                debug("No commands found for " + player.getName() + ".");
                return;
            }

            debug("Found " + onlineCommands.size() + " online " + StringUtil.pluralise(onlineCommands.size(), "command") + ".");
            processOnlineCommands(player, playerId, onlineCommands);
        }).exceptionally(ex -> {
            warning("Failed to get online commands: " + ex.getMessage(), "We will try again at the next due player check.", ex);
            return null;
        });
    }

    private boolean canProcessOnlineCommands(QueuedPlayer player, Object playerId) {
        if (isOnlineMode() && !isGeyser() && !player.hasUuid()) {
            debug("Player " + player.getName() + " has online commands but no UUID was provided for an online-mode store. Skipping.");
            return false;
        }

        if (!isPlayerOnline(playerId)) {
            debug("Player " + player.getName() + " has online commands but is not connected. Skipping.");
            return false;
        }

        return true;
    }

    /**
     * Selects the appropriate player ID for a player based on platform configuration.
     * @param name The name of the player.
     * @param uuid The UUID of the player.
     * @return The player ID to use.
     */
    @NotNull
    public final Object getPlayerId(String name, UUID uuid) {
        // Geyser/offline stores and missing UUIDs must use usernames for consistent matching.
        boolean useUuid = isOnlineMode() && !isGeyser() && uuid != null && !UUIDUtil.EMPTY_UUID.equals(uuid);
        if (useUuid) {
            return uuid;
        }

        return (name == null) ? "" : name;
    }

    @Override
    public String resolveCommandPlayerId(QueuedPlayer player) {
        if (player == null) {
            return "";
        }

        if (player.hasUuid()) {
            return player.getUuid();
        }

        if (!player.hasXuid()) {
            return player.getDefaultCommandIdentifier();
        }

        UUID resolvedUuid = resolvedFloodgateIds.computeIfAbsent(player.getXuid(), ignored -> resolveFloodgateUniqueId(player));
        if (resolvedUuid != null && !UUIDUtil.EMPTY_UUID.equals(resolvedUuid)) {
            return resolvedUuid.toString();
        }

        return player.getDefaultCommandIdentifier();
    }

    @Override
    public void sendCheckoutLink(String playerName, String checkoutUrl) {
        boolean bedrock = isOnlineFloodgatePlayer(playerName);
        String template = getCheckoutMessage(bedrock);

        if (template == null || template.isEmpty()) {
            return;
        }

        String message = template
                .replace("%url%", checkoutUrl)
                .replace("%player%", playerName);

        sendPlayerMessage(playerName, formatMessage(message));
    }

    /**
     * @param bedrock Whether the player is a Bedrock player, who cannot click links in chat.
     * @return The configured checkout message template.
     */
    public String getCheckoutMessage(boolean bedrock) {
        String fallback = bedrock ? DEFAULT_BEDROCK_CHECKOUT_MESSAGE : DEFAULT_CHECKOUT_MESSAGE;

        if (!(config instanceof ServerPlatformConfig)) {
            return fallback;
        }

        ServerPlatformConfig serverConfig = (ServerPlatformConfig) config;
        String message = bedrock ? serverConfig.getBedrockCheckoutMessage() : serverConfig.getCheckoutMessage();

        return message != null ? message : fallback;
    }

    private UUID resolveFloodgateUniqueId(QueuedPlayer player) {
        if (!isGeyser()) {
            return null;
        }

        Long xuid = player.getXuidAsLong();
        if (xuid == null) {
            debug("Unable to parse Bedrock XUID '" + player.getXuid() + "' for player '" + player.getName() + "'.");
            return null;
        }

        try {
            FloodgateApi api = FloodgateApi.getInstance();
            UUID bedrockId = api.createJavaPlayerId(xuid);

            if (bedrockId == null || UUIDUtil.EMPTY_UUID.equals(bedrockId)) {
                return null;
            }

            FloodgatePlayer onlinePlayer = api.getPlayer(bedrockId);
            if (onlinePlayer != null && onlinePlayer.getCorrectUniqueId() != null) {
                return onlinePlayer.getCorrectUniqueId();
            }

            if (api.getPlayerLink() != null) {
                try {
                    LinkedPlayer linkedPlayer = api.getPlayerLink().getLinkedPlayer(bedrockId).get(2, TimeUnit.SECONDS);
                    if (linkedPlayer != null && linkedPlayer.getJavaUniqueId() != null) {
                        return linkedPlayer.getJavaUniqueId();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    debug("Interrupted while resolving Floodgate UUID for player '" + player.getName() + "'.");
                } catch (ExecutionException | TimeoutException e) {
                    debug("Failed to resolve Floodgate link data for player '" + player.getName() + "': " + e.getMessage());
                }
            }

            return bedrockId;
        } catch (IllegalStateException | NoClassDefFoundError e) {
            warnMissingFloodgateApi();
        }

        return null;
    }

    private boolean isOnlineFloodgatePlayer(String playerName) {
        UUID playerUniqueId = getPlayerUniqueId(playerName);
        if (playerUniqueId == null) {
            return false;
        }

        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(playerUniqueId);
        } catch (IllegalStateException | NoClassDefFoundError e) {
            warnMissingFloodgateApi();
            return false;
        }
    }

    private void warnMissingFloodgateApi() {
        if (!floodgateWarningLogged.compareAndSet(false, true)) {
            return;
        }

        warning(
                "Received a Bedrock XUID for command placeholder resolution, but the Floodgate API is unavailable.",
                "Install Floodgate on the same server or proxy running Tebex so {id}/{uuid} placeholders can be resolved for Bedrock players."
        );
    }

    /**
     * Processes the online commands for a player.
     *
     * @param player The queued player.
     * @param playerId The Unique Identifier of the player.
     * @param commands The commands to process.
     */
    public final void processOnlineCommands(QueuedPlayer player, Object playerId, List<QueuedCommand> commands) {
        if(! isSetup()) return;

        String playerName = player.getName();
        boolean hasInventorySpace = true;
        for (QueuedCommand command : commands) {
            String parsedCommand = command.getParsedCommand(this);
            int freeSlots = getFreeSlots(playerId);
            if(freeSlots < command.getRequiredSlots()) {
                info(String.format("Skipping command '%s' for player '%s' due to no inventory space. Free slots: %d. Slots required: %d", parsedCommand, playerName, freeSlots, command.getRequiredSlots()));
                hasInventorySpace = false;
                continue;
            }

            final Runnable commandRunnable = () -> {
                if (!canProcessOnlineCommands(player, playerId)) {
                    info(String.format("Skipping command '%s' for player '%s' because they are no longer verified online", parsedCommand, playerName));
                    return;
                }

                info(String.format("Dispatching command '%s' for player '%s'", parsedCommand, playerName));
                CommandResult commandResult = dispatchCommand(parsedCommand);

                // report whether the command succeeded or failed
                if (!commandResult.isSuccess()) {
                    String extraInfo = "";
                    Throwable commandException = commandResult.getException();
                    if (commandResult.getMessage() != null && !commandResult.getMessage().isEmpty()) {
                        extraInfo = commandResult.getMessage();
                    }
                    if (commandException != null) {
                        extraInfo = commandResult.getException().getMessage();
                    }

                    if (extraInfo.isEmpty()) {
                        extraInfo = "No further information";
                    }

                    String solution = String.format("Manually try `%s` for player %s. Check that the command syntax is correct.", parsedCommand, playerName);
                    if (command.getPayment() != 0) {
                        solution += " Re-run this command at https://creator.tebex.io/payments/" + command.getPayment();
                    }
                    warning("Command failed to execute: " + extraInfo, solution);
                }

                // Mark online commands completed only after their scheduled runnable has verified the player and dispatched.
                List<Integer> completedCommands = new ArrayList<>();
                completedCommands.add(command.getId());
                deleteCompletedCommands(completedCommands);
            };
            if (command.getDelay() > 0) {
                executeBlockingLater(commandRunnable, command.getDelay(), TimeUnit.SECONDS);
            } else {
                executeBlocking(commandRunnable);
            }
        }

        if(! hasInventorySpace) return;
        getQueuedPlayers().remove(playerId);
    }

    public final void handleOfflineCommands() {
        if(! isSetup()) return;

        getSDK().getOfflineCommands().thenAccept(offlineData -> {
            if(offlineData.getCommands().isEmpty()) {
                return;
            }

            for (QueuedCommand command : offlineData.getCommands()) {
                String parsedCommand = command.getParsedCommand(this);
                final Runnable commandRunnable = () -> {
                    info(String.format("Dispatching offline command '%s' for player '%s'.", parsedCommand, command.getPlayer().getName()));
                    CommandResult offlineCommandResult = dispatchCommand(parsedCommand);

                    // report whether the offline command succeeded or failed
                    if (!offlineCommandResult.isSuccess()) {
                        String extraInfo = "";
                        Throwable commandException = offlineCommandResult.getException();
                        if (!offlineCommandResult.getMessage().isEmpty()) {
                            extraInfo = offlineCommandResult.getMessage();
                        }
                        if (commandException != null) {
                            extraInfo = offlineCommandResult.getException().getMessage();
                        }

                        String solution = "Check that the command syntax is correct.";
                        if (command.getPayment() != 0) {
                            solution += " Re-run this command at https://creator.tebex.io/payments/" + command.getPayment();
                        }
                        warning(String.format("Command `%s` failed to execute: %s", parsedCommand, extraInfo), solution);
                    }

                    List<Integer> completed = new ArrayList<>();
                    completed.add(command.getId());
                    deleteCompletedCommands(completed);
                };
                if (command.getDelay() > 0) {
                    executeBlockingLater(commandRunnable, command.getDelay(), TimeUnit.SECONDS);
                } else {
                    executeBlocking(commandRunnable);
                }
            }
        }).exceptionally(ex -> {
            warning("Failed to retrieve offline commands - some commands may not have been processed. " + ex.getMessage(), "We will try again at the next due player check.", ex);
            return null;
        });
    }

    public final void deleteCompletedCommands(List<Integer> completedCommands) {
        getSDK().deleteCommands(completedCommands).thenRun(completedCommands::clear).exceptionally(ex -> {
            error("Failed to delete commands: " + ex.getMessage(), ex);
            return null;
        });
    }

    /**
     * Converts the version string into a version number.
     *
     * @return The version number.
     */
    public final int getVersionNumber() {
        return Integer.parseInt(getPluginVersion().replace(".", ""));
    }

    /**
     * Logs an informational message to the console.
     *
     * @param message The message to log.
     */
    public final void info(String message) {
        log(Level.INFO, message);
    }

    /**
     * Logs a warning message to the console. A "warning" is due to a problem that either the user can solve, or a
     * problem that can resolve itself later. All warnings must have solutions provided.
     * <p>
     * ex.)
     * @param message The message to log.
     * @param solution User-friendly description of how to resolve the problem.
     */
    public final void warning(String message, String solution) {
        log(Level.WARNING, message);
        log(Level.WARNING, "- " + solution);
        createPluginEvent(EnumEventLevel.WARNING, message);
    }

    public final void warning(String message, String solution, Throwable t) {
        log(Level.WARNING, message);
        log(Level.WARNING, "- " + solution);
        createPluginEvent(EnumEventLevel.WARNING, message);
    }

    public final void error(String message) {
        log(Level.SEVERE, message);
        createPluginEvent(EnumEventLevel.ERROR, message);
    }

    public final void error(String message, Throwable t) {
        log(Level.SEVERE, message);
        createPluginEvent(EnumEventLevel.ERROR, message, t);
    }

    /**
     * Logs a debug message to the console if debugging is enabled in the platform configuration.
     *
     * @param message The message to log.
     */
    public final void debug(String message) {
        if (getPlatformConfig() != null && !getPlatformConfig().isVerbose()) return;
        info("[DEBUG] " + message);
    }

    // Create and update the file
    public final YamlDocument initPlatformConfig() throws IOException {
        return YamlDocument.create(getBundledFile(this, getRunningDirectory(), "config.yml"));
    }

    /**
     * Loads the server platform configuration from the file.
     *
     * @param configFile The configuration file.
     * @return The PlatformConfig instance representing the loaded configuration.
     */
    public final ServerPlatformConfig loadServerPlatformConfig(YamlDocument configFile) {
        ServerPlatformConfig config = new ServerPlatformConfig(configFile.getInt("config-version", 1));
        config.setYamlDocument(configFile);

        if(config.getConfigVersion() < 2) {
            return config;
        }

        config.setSecretKey(configFile.getString("server.secret-key"));
        config.setBuyCommandName(configFile.getString("buy-command.name", "buy"));
        config.setBuyCommandEnabled(configFile.getBoolean("buy-command.enabled", true));

        config.setCheckForUpdates(configFile.getBoolean("check-for-updates", true));
        config.setVerbose(configFile.getBoolean("verbose", false));

        config.setProxyMode(configFile.getBoolean("server.proxy", false));
        config.setAutoReportEnabled(configFile.getBoolean("auto-report-enabled", true));
        config.setCheckIntervalSeconds(configFile.getInt("check-interval", 3));

        config.setCheckoutMessage(configFile.getString("messages.checkout", DEFAULT_CHECKOUT_MESSAGE));
        config.setBedrockCheckoutMessage(configFile.getString("messages.checkout-bedrock", DEFAULT_BEDROCK_CHECKOUT_MESSAGE));

        return config;
    }

    /**
     * Writes any configuration keys added by newer plugin versions into an existing
     * config.yml, leaving every value the user has already set untouched. Without this,
     * upgrading installs keep a config file that never shows the new options.
     */
    private void applyMissingConfigDefaults(YamlDocument configFile) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("check-interval", 3);
        defaults.put("messages.checkout", DEFAULT_CHECKOUT_MESSAGE);
        defaults.put("messages.checkout-bedrock", DEFAULT_BEDROCK_CHECKOUT_MESSAGE);
        defaults.put("free-packages.default-cooldown", 0);
        defaults.put("free-packages.cooldowns", new LinkedHashMap<String, Object>());
        defaults.put("free-packages.reminder.enabled", true);
        defaults.put("free-packages.reminder.interval-minutes", 10);
        defaults.put("free-packages.reminder.message", DEFAULT_FREE_REMINDER_MESSAGE);
        defaults.put("gui.dialog.sprite-version-check", true);
        defaults.put("gui.dialog.tooltips.enabled", true);
        defaults.put("gui.dialog.tooltips.categories", new LinkedHashMap<String, Object>());
        defaults.put("gui.dialog.tooltips.packages", new LinkedHashMap<String, Object>());

        boolean changed = false;
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            if (!configFile.contains(entry.getKey())) {
                configFile.set(entry.getKey(), entry.getValue());
                changed = true;
            }
        }

        if (!changed) return;

        try {
            configFile.save();
            debug("Added new configuration options to config.yml.");
        } catch (IOException e) {
            warning("Failed to add new configuration options to config.yml: " + e.getMessage(),
                    "You may need to add the new options manually or regenerate the file.");
        }
    }

    public final void refreshListings() {
        getSDK().getListing().thenAccept(categories -> {
            setStoreCategories(categories);
            getSDK().getPackageExtras().thenAccept(extras -> {
                if (extras.isEmpty()) return;
                for (Category cat : categories) {
                    applyExtras(cat.getPackages(), extras);
                    if (cat.getSubCategories() != null) {
                        for (SubCategory sub : cat.getSubCategories()) {
                            applyExtras(sub.getPackages(), extras);
                        }
                    }
                }
            }).exceptionally(throwable -> {
                debug("Failed to apply package extras: " + throwable.getMessage());
                return null;
            });
        }).exceptionally(throwable -> {
            debug("Failed to refresh store listings: " + throwable.getMessage());
            return null;
        });
    }

    private void applyExtras(List<CategoryPackage> packages, Map<Integer, com.google.gson.JsonObject> extras) {
        for (CategoryPackage pkg : packages) {
            com.google.gson.JsonObject raw = extras.get(pkg.getId());
            if (raw == null) continue;

            if (raw.has("description") && !raw.get("description").isJsonNull()) {
                String desc = raw.get("description").getAsString();
                if (!desc.isEmpty()) {
                    pkg.setDescription(desc);
                }
            }

            if (pkg.getDescription() == null || pkg.getDescription().isEmpty()) {
                debug("Package " + pkg.getId() + " (" + pkg.getName() + ") has no description from the store API"
                        + "; set gui.dialog.tooltips.packages." + pkg.getId() + " in config.yml to give it a tooltip.");
            }

            int cooldownSeconds = readCooldownSeconds(raw);
            if (cooldownSeconds > 0) {
                pkg.setCooldownSeconds(cooldownSeconds);
                debug("Package " + pkg.getId() + " has a " + cooldownSeconds + "s cooldown from the store API.");
            }
        }
    }

    /**
     * Reads a free-package cooldown out of a raw package payload.
     *
     * <p>Not every store exposes package meta through the plugin API, and the stores that
     * do are inconsistent about it: meta arrives as a nested object on some and as an
     * embedded JSON string on others, with the value itself either a number or a string.
     * Anything unreadable yields 0 so the config-based cooldown takes over, and a parse
     * failure here must never abort the listing refresh.
     */
    private int readCooldownSeconds(com.google.gson.JsonObject raw) {
        try {
            com.google.gson.JsonObject meta = null;

            if (raw.has("meta") && !raw.get("meta").isJsonNull()) {
                com.google.gson.JsonElement metaElement = raw.get("meta");
                if (metaElement.isJsonObject()) {
                    meta = metaElement.getAsJsonObject();
                } else if (metaElement.isJsonPrimitive()) {
                    meta = new com.google.gson.Gson().fromJson(metaElement.getAsString(), com.google.gson.JsonObject.class);
                }
            }

            // Fall back to the package root, in case the field is not nested under meta.
            com.google.gson.JsonObject source = meta != null && meta.has("cooldown_seconds") ? meta : raw;
            if (!source.has("cooldown_seconds") || source.get("cooldown_seconds").isJsonNull()) {
                return 0;
            }

            String value = source.get("cooldown_seconds").getAsString().trim();
            return value.isEmpty() ? 0 : Math.max(0, (int) Double.parseDouble(value));
        } catch (Exception e) {
            debug("Unable to read cooldown_seconds from package meta: " + e.getMessage());
            return 0;
        }
    }

    public final void clearPluginEvents() {
        this.PLUGIN_EVENTS.clear();
    }

    public final void createPluginEvent(EnumEventLevel level, String message) {
        if (!getPlatformConfig().isAutoReportEnabled()) {
            return;
        }

        PluginEvent event = new PluginEvent(this, level, message);
        if (isSetup()) {
            event = event.onStore(getStore()).onServer(getStoreServer());
        }
        PLUGIN_EVENTS.add(event);
    }

    public final void createPluginEvent(EnumEventLevel level, String message, Throwable e) {
        if (!getPlatformConfig().isAutoReportEnabled()) {
            log(Level.SEVERE, e.getMessage());
            return;
        }
        PluginEvent event = new PluginEvent(this, level, message).withTrace(e);
        PLUGIN_EVENTS.add(event);
    }

    @Override
    public final SDK getSDK() {
        return sdk;
    }

    /**
     * Checks if the platform is set up and ready to use.
     *
     * @return True if the platform is set up, false otherwise.
     */
    public final boolean isSetup() {
        return setup;
    }

    /**
     * Sets whether the platform is set up and ready to use.
     *
     * @param setup True if the platform is set up, false otherwise.
     */
    public final void setSetup(boolean setup) {
        this.setup = setup;
    }

    /**
     * Halts the platform and stops any ongoing tasks.
     */
    public final void halt() {
        this.setup = false;
    }

    public final PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    public final Map<Object, Integer> getQueuedPlayers() {
        return queuedPlayers;
    }

    public final void setStoreInfo(ServerInformation info) {
        this.storeInformation = info;
    }

    public final ServerInformation getStoreInformation() {
        return this.storeInformation;
    }

    public final void setStoreCategories(List<Category> categories) {
        this.storeCategories = categories;
    }

    public final List<Category> getStoreCategories() {
        return this.storeCategories;
    }

    public final ServerInformation.Server getStoreServer() {
        return this.storeInformation.getServer();
    }

    public final ServerInformation.Store getStore() {
        return this.storeInformation.getStore();
    }

    public final String getStoreType() {
        return storeInformation == null ? "" : storeInformation.getStore().getGameType();
    }

    /**
     * Gets the current platform configuration.
     *
     * @return The PlatformConfig instance representing the current configuration.
     */
    public IPlatformConfig getPlatformConfig() {
        return config;
    }

    public int getCheckIntervalSeconds() {
        if (config instanceof ServerPlatformConfig) {
            int interval = ((ServerPlatformConfig) config).getCheckIntervalSeconds();
            return interval > 0 ? interval : 3;
        }
        return 3;
    }

    public void executeAsync(Runnable runnable) {
        Multithreading.runAsync(runnable);
    }

    public void executeAsyncLater(Runnable runnable, long time, TimeUnit unit) {
        Multithreading.executeAsyncLater(runnable, time, unit);
    }

    public void executeBlocking(Runnable runnable) {
        try {
            Multithreading.executeBlocking(runnable);
        } catch (InterruptedException | ExecutionException e) {
            error("Failed to execute blocking task: " + e.getMessage(), e);
        }
    }

    public void executeBlockingLater(Runnable runnable, long time, TimeUnit unit) {
        TickScheduler.scheduleLater(runnable, time, unit);
    }

    public final List<ServerEvent> getJoinEvents() {
        return serverEvents;
    }

    public void createJoinEvent(String uuid, String username, String ip) {
        serverEvents.add(new ServerEvent(uuid, username, ip, EnumServerEventType.JOIN));
    }

    public final void configure() {
        setup = true;
        sdk.sendTelemetry();
    }

    public ArrayList<PluginEvent> getPluginEvents() {
        return PLUGIN_EVENTS;
    }

    public void loadPlatformConfig() {
        try {
            configYaml = initPlatformConfig();
            applyMissingConfigDefaults(configYaml);
            config = loadServerPlatformConfig(configYaml);
            String envKey = System.getenv("TEBEX_SECRET_KEY");
            if (envKey != null && !envKey.isEmpty()) {
                debug("Using secret key from environment variable");
                config.setSecretKey(envKey);
            }

            if (config.getYamlDocument() == null) {
                error("YamlDocument is null after loading config");
            }
        } catch (IOException e) {
            warning("Failed to load configuration: " + e.getMessage(),
                    "Check that your configuration is valid and in the proper format and reload the plugin. You may delete `Tebex/config.yml` and a new configuration will be generated.");
        }

    }

    @Override
    public void reloadConfig() {
        loadPlatformConfig();
    }

    @Override
    public void saveConfig(IPlatformConfig platformConfig) {
        try {
            config.setSecretKey(platformConfig.getSecretKey());
            config.setVerbose(platformConfig.isVerbose());
            this.config = (ServerPlatformConfig) platformConfig;
            YamlDocument yamlDoc = this.config.getYamlDocument();
            if (yamlDoc == null) {
                error("YamlDocument is null when trying to save config!?");
                return;
            }

            // ensure actual value is set in the document
            yamlDoc.set("server.secret-key", this.config.getSecretKey());
            yamlDoc.set("verbose", this.config.isVerbose());

            yamlDoc.save(); // Save the updated YAML document
        } catch (Exception e) {
            error("Failed to save configuration: " + e.getMessage(), e);
        }
    }


    public void clearSelectedPluginEvents(List<ServerEvent> events) {
        serverEvents.removeAll(events);
    }

    public void setServerInformation(ServerInformation info) {
        this.storeInformation = info;
    }
}
