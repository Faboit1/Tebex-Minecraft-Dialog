package io.tebex.sdk.commands;

import io.tebex.sdk.obj.CommunityGoal;
import io.tebex.sdk.platform.Platform;
import io.tebex.sdk.request.response.ServerInformation;

import java.util.*;
import java.util.function.Consumer;

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
    }

    public static boolean process(CommandContext context) {
        if (context.fullCommand.startsWith(TEBEX_COMMAND_PREFIX)) {
            Command command = commands.get(context.commandName);
            Consumer<CommandContext> handler = command.getHandler();

            // Check number of args required
            if (context.arguments.length < command.getNumArgsRequired()) {
                context.tellSender("Usage: " + command.getUsage());
            }

            handler.accept(context);
            return true;
        }

        return false;
    }

    static {
        register("ban","<playerName> <reason> <ip>", "Bans a user from the webstore. Unbans can only be made from the store panel.", (ctx) -> {
            String reason = ctx.arguments[1];
            String ip = ctx.arguments[2];
            platform.getSDK().createBan(ctx.targetUUID.toString(), ip, reason).thenAccept((success) -> {
                if (success) {
                    ctx.tellSender("User " + ctx.targetUsername + " has been banned successfully.");
                } else {
                    ctx.tellSender("Failed to ban user " + ctx.targetUsername + ".");
                }
            });
        });


        register("checkout", "<packageId>", "Creates a payment link for a package.", (ctx) -> {
            String packageId = ctx.arguments[0];
            platform.getSDK().createCheckoutUrl(Integer.parseInt(packageId), ctx.targetUsername).thenAccept((url) -> {
                ctx.tellTarget("Checkout started! Click here to complete payment: " + url.getUrl());
            });
        });


        register("debug", "<true/false>", "Enables or disables debug logging.", (ctx) -> {
            boolean value = Boolean.parseBoolean(ctx.arguments[0]);
            platform.getPlatformConfig().setVerbose(value);
            ctx.tellSender("Debug mode set to " + value);
        });


        register("forcecheck", "", "Checks immediately for any purchases that need to be delivered.", (ctx) -> {
            ctx.tellSender("Beginning force check for due commands...");
            platform.performCheck(false);
            ctx.tellSender("Check completed.");
        });


        register("goals", "", "Shows progress to community goals.", (ctx) -> platform.getSDK().getCommunityGoals().thenAccept((goals) -> {
            for (CommunityGoal goal: goals) {
                if (goal.getStatus() != CommunityGoal.Status.DISABLED) {
                    ctx.tellSender("Community Goals: ");
                    ctx.tellSender(String.format("- %s (%.2f/%.2f) [%s]", goal.getName(), goal.getCurrent(), goal.getTarget(), goal.getStatus()));
                }
            }
        }));

        register("info", "", "Shows information about the connected Tebex store.", (ctx) -> {
            ServerInformation.Store store = platform.getStore();
            ServerInformation.Server server = platform.getStoreServer();

            ctx.tellSender("Information for this server:");
            ctx.tellSender(server.getName() + " for webstore " + store.getName());
            ctx.tellSender("Server prices are in " +  store.getCurrency().getIso4217());
            ctx.tellSender("Webstore domain " +  store.getDomain());
        });

        register("lookup", "<username>", "Gets user transaction info from your webstore.", (ctx) -> {
            ctx.tellSender("Performing player lookup for " + ctx.targetUsername + "...");
            platform.getSDK().getPlayerLookupInfo(ctx.targetUsername).thenAccept((lookupInfo) -> {
                ctx.tellSender("Username: " + lookupInfo.getLookupPlayer().getUsername());
                ctx.tellSender("Id: " + lookupInfo.getLookupPlayer().getId());
                ctx.tellSender("Chargeback Rate: " + lookupInfo.chargebackRate);
                ctx.tellSender("Bans Total: " + lookupInfo.banCount);
                ctx.tellSender("Payments: " + lookupInfo.payments.size());
            });
        });

        register("reload", "", "Reloads the plugin configuration and store connection.", (ctx) -> {
            ctx.tellSender("Tebex is reloading...");
            platform.reloadConfig();
            platform.refreshListings();
            platform.getSDK().sendPluginEvents();
            // platform.registerBuyCommand(); //TODO
            //platform.setBuyGUI(new BuyGUI(platform)); //TODO
            ctx.tellSender("Reload completed.");
        });

        register("secret", "<key>", "Connects to your Tebex store.", (ctx) -> {
            ctx.tellSender("Checking your secret key...");

            String oldKey = platform.getPlatformConfig().getSecretKey();

            // Set the new key and attempt to query server info. If it works, key is valid and we set in config.
            platform.getSDK().setSecretKey(ctx.arguments[0]);
            platform.getSDK().getServerInformation().thenAccept((newInfo) -> {
                if (newInfo != null) {
                    platform.getPlatformConfig().setSecretKey(ctx.arguments[0]);
                    platform.saveConfig(platform.getPlatformConfig());
                    platform.setStoreInfo(newInfo);
                    platform.refreshListings();
                    ctx.tellSender("Successfully connected to your store: " + newInfo.getStore().getName() + " as " + newInfo.getServer().getName());
                }
            }).exceptionally((e) -> {
                platform.getSDK().setSecretKey(oldKey);
                return null;
            });
        });

        register("sendlink", "<packageId> <username>", "Sends a purchase link to a player.", (ctx) -> {
            platform.getSDK().createCheckoutUrl(Integer.parseInt(ctx.arguments[0]), ctx.targetUsername).thenAccept(checkoutUrl -> {
                ctx.tellTarget("A checkout link has been created for you. Click here to complete payment: " + checkoutUrl.getUrl());
                ctx.tellSender("A checkout link has been sent to " + ctx.targetUsername);
            }).exceptionally(e -> {
                ctx.tellSender("Failed to create a checkout link for package: " + e.getMessage());
                return null;
            });
        });

        register("help", "", "Shows available commands.", (ctx) -> {
            //TODO
        });
    }

    public static Platform getPlatform() {
        return platform;
    }
}
