package io.tebex.sdk.commands;

import io.tebex.sdk.obj.CommunityGoal;
import io.tebex.sdk.platform.Platform;
import io.tebex.sdk.request.response.ServerInformation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;

public class TebexCommands {
    public static final String TEBEX_COMMAND_PREFIX = "tebex";
    private static Platform platform;

    private static final Map<String, Command> commands = new HashMap<>();

    public static Map<String, Command> getCommands() {
        return Collections.unmodifiableMap(commands);
    }

    private TebexCommands() {}

    public static void setPlatform(Platform platform) {
        TebexCommands.platform = platform;
    }

    private static void register(String name, String usage, String description, Consumer<CommandContext> handler) {
        Command command = new Command(name, usage, description, handler);
        commands.put(name, command);
        platform.log(Level.INFO, "Registered command: '/tebex " + name + "' with permission '" +command.getPermission() + "'");
    }

    public static boolean process(CommandContext context) {
        if (context.getFullCommand().startsWith(TEBEX_COMMAND_PREFIX)) {
            Command command = commands.get(context.getCommandName());
            if (command == null) {
                CommandResponder.tellError(context, "Unrecognized command or no permission.");
                return false;
            }

            Consumer<CommandContext> handler = command.getHandler();

            // Check permissions
            boolean hasPermission = context.isFromConsole() || TebexCommands.getPlatform().hasPermission(context.getSenderUsername(), command.getPermission());
            if (!hasPermission) {
                CommandResponder.tellError(context, "Unrecognized command or no permission.");
                return false;
            }

            // Check number of args required
            if (context.getArgs().length != command.getNumArgsRequired()) {
                CommandResponder.tellError(context, "Usage: /tebex " + command.getName() + " " + command.getUsage());
                return false;
            }

            handler.accept(context);
            return true;
        }
        return false;
    }

    public static Platform getPlatform() {
        return platform;
    }

    public static Map<String, Command> getAllowedCommands(String username) {
        Map<String,Command> allowedCommands = new HashMap<>();
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            Command command = entry.getValue();

            // Only show commands the user has permissions for
            if (!platform.hasPermission(username, command.getPermission())) {
                continue;
            }

            allowedCommands.put(entry.getKey(), command);
        }
        return allowedCommands;
    }

    public static void register(Platform platform) {
        TebexCommands.platform = platform;

        register("ban","<playerName> <reason> <ip>", "Bans a user from the webstore.", (ctx) -> {
            String name = ctx.getArgs()[0]; // player may be offline, so don't rely on target username which requires successful getPlayer() call
            String reason = ctx.getArgs()[1];
            String ip = ctx.getArgs()[2];
            platform.getSDK().createBan(name, ip, reason).thenAccept((success) -> {
                if (success) {
                    CommandResponder.tellSuccess(ctx, "User {0} has been banned successfully.", name);
                } else {
                    CommandResponder.tellError(ctx, "Failed to ban user.");
                }
            }).exceptionally(e -> {
                CommandResponder.tellError(ctx, "Failed to ban user. " + e.getMessage());
                return null;
            });
        });


        register("checkout", "<packageId>", "Creates a payment link for a package.", (ctx) -> {
            if (ctx.isFromConsole()) {
                CommandResponder.tellError(ctx, "This command cannot be run from the console.");
                return;
            }

            String packageId = ctx.getArgs()[0];
            int intPackageId = -1;
            try {
                intPackageId = Integer.parseInt(packageId);
            } catch (NumberFormatException e) {
                CommandResponder.tellError(ctx, "The Package ID must be a number.");
                return;
            }

            platform.getSDK().createCheckoutUrl(intPackageId, ctx.getSenderUsername()).thenAccept((url) -> {
                CommandResponder.tellFancy(ctx, "Checkout started! Click here to complete payment: {0}", url.getUrl());
            }).exceptionally(e -> {
                CommandResponder.tellError(ctx, e.getMessage());
                return null;
            });;
        });


        register("debug", "<true/false>", "Enables or disables debug logging.", (ctx) -> {
            String value = ctx.getArgs()[0].toLowerCase();
            if (!value.equals("true") && !value.equals("false")) {
                CommandResponder.tellError(ctx, "Specify 'true' or 'false' for debug mode.");
                return;
            }
            boolean debugValue = Boolean.parseBoolean(value);
            platform.getPlatformConfig().setVerbose(debugValue);
            CommandResponder.tellFancy(ctx, "Debug mode set to {0}", value);
        });


        register("forcecheck", "", "Checks immediately for any purchases that need to be delivered.", (ctx) -> {
            CommandResponder.tellFancy(ctx, "Beginning force check for due commands...");
            boolean previousDebugState = platform.getPlatformConfig().isVerbose();

            // Temporarily enable debug logging while a forcecheck runs
            platform.getPlatformConfig().setVerbose(true);
            platform.performCheck(false); //TODO completable future

            // Restore previous debug state
            platform.getPlatformConfig().setVerbose(previousDebugState);
            CommandResponder.tellSuccess(ctx, "Check completed. See console for details.");
        });


        register("goals", "", "Shows progress to community goals.", (ctx) -> platform.getSDK().getCommunityGoals().thenAccept((goals) -> {
            if (goals.isEmpty()) { //TODO cache
                CommandResponder.tellFancy(ctx, "No community goals available.");
                return;
            }

            CommandResponder.tellFancy(ctx, "Community Goals: ");
            for (CommunityGoal goal: goals) {

                if (goal.getStatus() != CommunityGoal.Status.DISABLED) {
                    CommandResponder.tellFancy(ctx, String.format("- %s (%.2f/%.2f) [%s]", goal.getName(), goal.getCurrent(), goal.getTarget(), goal.getStatus()));
                }
            }
        }));

        register("info", "", "Shows information about the connected store.", (ctx) -> {
            ServerInformation.Store store = platform.getStore();
            ServerInformation.Server server = platform.getStoreServer();

            CommandResponder.tellFancy(ctx, "Information for this server:");
            CommandResponder.tellFancy(ctx, "{0} for webstore {1}", server.getName(), store.getName());
            CommandResponder.tellFancy(ctx, "Server prices are in {0}", store.getCurrency().getIso4217());
            CommandResponder.tellFancy(ctx, "Webstore URL: {0}", store.getDomain());
        });

        register("lookup", "<username>", "Gets user transaction info from your webstore.", (ctx) -> {
            String username = ctx.getArgs()[0]; // Use provided username as player is not required to be online / no instance required
            CommandResponder.tellFancy(ctx, "Performing player lookup for {0}...", username);
            platform.getSDK().getPlayerLookupInfo(ctx.getTargetUsername()).thenAccept((lookupInfo) -> {
                CommandResponder.tellFancy(ctx, "Username: {0}", lookupInfo.getLookupPlayer().getUsername());
                CommandResponder.tellFancy(ctx, "Id: {0}", lookupInfo.getLookupPlayer().getId());
                CommandResponder.tellFancy(ctx, "Chargeback Rate: {0}%", String.valueOf(lookupInfo.chargebackRate));
                CommandResponder.tellFancy(ctx, "Bans Total: {0}", String.valueOf(lookupInfo.banCount));
                CommandResponder.tellFancy(ctx, "Payments: {0}", String.valueOf(lookupInfo.payments.size()));
            }).exceptionally(e -> {
                CommandResponder.tellError(ctx, e.getMessage());
                return null;
            });
        });

        register("reload", "", "Reloads the plugin configuration and store connection.", (ctx) -> {
            CommandResponder.tellFancy(ctx, "Tebex is reloading...");
            platform.reloadConfig();
            platform.refreshListings();
            platform.getSDK().sendPluginEvents();
            // platform.registerBuyCommand(); //TODO
            //platform.setBuyGUI(new BuyGUI(platform)); //TODO
            CommandResponder.tellSuccess(ctx, "Reload completed.");
        });

        register("secret", "<key>", "Connects to your store.", (ctx) -> {
            CommandResponder.tellFancy(ctx, "Checking your secret key...");
            String oldKey = platform.getPlatformConfig().getSecretKey();

            // Set the new key and attempt to query server info. If it works, key is valid and we set in config.
            platform.getSDK().setSecretKey(ctx.getArgs()[0]);
            platform.getSDK().getServerInformation().thenAccept((newInfo) -> {
                if (newInfo != null) {
                    platform.getPlatformConfig().setSecretKey(ctx.getArgs()[0]);
                    platform.saveConfig(platform.getPlatformConfig());
                    platform.setStoreInfo(newInfo);
                    platform.refreshListings();
                    CommandResponder.tellFancy(ctx, "Successfully connected to your store: {0} as {1}", newInfo.getStore().getName(), newInfo.getServer().getName());
                }
            }).exceptionally((e) -> {
                platform.getSDK().setSecretKey(oldKey);
                CommandResponder.tellError(ctx, "Your secret key was invalid. Please try again.");
                return null;
            });
        });

        register("sendlink", "<packageId> <username>", "Sends a purchase link to a player.", (ctx) -> {
            String packageId = ctx.getArgs()[0];
            String username = ctx.getArgs()[1];

            if (ctx.getTargetUsername().isEmpty()) { // target will be empty if player was offline / no entity found
                CommandResponder.tellError(ctx, username + " must be online to receive a package link.");
                return;
            }

            platform.getSDK().createCheckoutUrl(Integer.parseInt(ctx.getArgs()[0]), ctx.getTargetUsername()).thenAccept(checkoutUrl -> {
                CommandResponder.tellSuccess(ctx, "Checkout link sent to " + ctx.getTargetUsername());
                CommandResponder.tellOtherFancy(ctx, "A checkout link has been created for you. Click here to complete payment: {0}", checkoutUrl.getUrl());
            }).exceptionally(e -> {
                CommandResponder.tellError(ctx, "Failed to send checkout link: " + e.getMessage());
                return null;
            });
        });

        register("help", "", "Shows available commands.", (ctx) -> {
            CommandResponder.tellFancy(ctx, "Available commands:");

            for (Map.Entry<String, Command> entry : commands.entrySet()) {
                Command command = entry.getValue();

                // Only show commands the user has permissions for, or all for console
                boolean hasPermission = ctx.isFromConsole() || TebexCommands.getPlatform().hasPermission(ctx.getSenderUsername(), command.getPermission());
                if (!hasPermission) {
                    continue;
                }

                StringBuilder helpMessage = new StringBuilder();
                helpMessage.append("/");
                helpMessage.append(TEBEX_COMMAND_PREFIX);
                helpMessage.append(" ");
                helpMessage.append(command.getName());
                if (!command.getUsage().isEmpty()) {
                    helpMessage.append(" ");
                    helpMessage.append(command.getUsage());
                }
                helpMessage.append(" | ");
                helpMessage.append(command.getDescription());
                CommandResponder.tellFancy(ctx, helpMessage.toString());
            }
        });
    }
}
