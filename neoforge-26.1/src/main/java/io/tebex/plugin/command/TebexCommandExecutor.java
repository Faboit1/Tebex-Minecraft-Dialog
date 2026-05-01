package io.tebex.plugin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.tebex.plugin.NeoForgePermissionNodes;
import io.tebex.plugin.NeoForgePluginPlatform;
import io.tebex.sdk.commands.PlayerCommand;
import io.tebex.sdk.commands.Context;
import io.tebex.sdk.commands.Responder;
import io.tebex.sdk.commands.TebexCommands;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.Arrays;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class TebexCommandExecutor {
    private final NeoForgePluginPlatform platform;

    public TebexCommandExecutor(NeoForgePluginPlatform platform) {
        this.platform = platform;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ServerPlatformConfig config = (ServerPlatformConfig) platform.getPlatformConfig();
        if (config.isBuyCommandEnabled()) {
            BuyCommand buyCommand = new BuyCommand(platform);
            dispatcher.register(literal(config.getBuyCommandName())
                    .requires(source -> source.getEntity() == null || platform.hasPermission(source.getTextName(), NeoForgePermissionNodes.BUY_PERMISSION))
                    .executes(buyCommand::execute));
            platform.debug("buy command registered as: " + config.getBuyCommandName());
        }

        LiteralArgumentBuilder<CommandSourceStack> root = literal("tebex")
                .executes(context -> {
                    runRoot(context.getSource());
                    return 1;
                });

        // register each command from the sdk
        TebexCommands.getCommands().forEach((name, command) -> {
            platform.debug("registering command: " + name);
            platform.debug(command.getDescription());

            // Parse command usage and add arguments
            String usage = command.getUsage();
            String[] args = new String[0];
            if (!usage.isEmpty()) {
                args = usage.replace("<", "").replace(">", "").split(" ");
            }

            LiteralArgumentBuilder<CommandSourceStack> subCommand = literal(command.getCommandName())
                    .requires(source -> source.getEntity() == null || platform.hasPermission(source.getTextName(), command.getPermission()));

            if (args.length == 3) {
                subCommand.then(argument(args[0], StringArgumentType.string())
                        .then(argument(args[1], StringArgumentType.string())
                        .then(argument(args[2], StringArgumentType.string())
                        .executes(context -> { run(command, context); return 1;}))));
            } else if (args.length == 2) {
                subCommand.then(argument(args[0], StringArgumentType.string())
                        .then(argument(args[1], StringArgumentType.string())
                        .executes(context -> { run(command, context); return 1;})));
            } else if (args.length == 1) {
                subCommand.then(argument(args[0], StringArgumentType.string()).executes(context -> { run(command, context); return 1;}));
            } else if (args.length == 0) {
                subCommand.executes(context -> { run(command, context); return 1;});
            } else {
                return;
            }

            root.then(subCommand);
        });

        dispatcher.register(root);
    }

    public void run(PlayerCommand command, com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        CommandSourceStack sender = context.getSource();
        String senderName = sender.getTextName();
        UUID senderUUID = sender.getEntity() instanceof ServerPlayer ? sender.getEntity().getUUID() : null;
        boolean isConsole = sender.getEntity() == null;

        String[] splitInput = context.getInput().split("\\s+");
        String[] tokens = splitInput.length > 1 ? Arrays.copyOfRange(splitInput, 1, splitInput.length) : new String[0];

        String targetName = "";
        UUID targetUUID = new UUID(0L, 0L);
        for (String token : tokens) {
            if (targetName.isEmpty()) {
                ServerPlayer targetPlayer = platform.getPlayer(token);
                if (targetPlayer != null) {
                    targetName = targetPlayer.getName().getString();
                    targetUUID = targetPlayer.getUUID();
                }
            }
        }

        Context tebexCtx = Context.from(isConsole, senderName, senderUUID, "tebex " + command.getCommandName(), targetName, targetUUID, tokens);
        if (tokens.length == 0 && platform.hasPermission(senderName, "tebex.base")) {
            sender.sendSystemMessage(Component.literal(Responder.formatFancy(tebexCtx, "Welcome to Tebex!")));
            sender.sendSystemMessage(Component.literal(Responder.formatFancy(tebexCtx, "This server is running version {0}", "v" + platform.getPluginVersion())));
            return;
        }

        TebexCommands.process(tebexCtx, future -> future.thenAccept(msgs -> {
            for (String msg : msgs) {
                sender.sendSystemMessage(Component.literal(msg));
            }
        }));
    }

    private void runRoot(CommandSourceStack sender) {
        String senderName = sender.getTextName();
        UUID senderUUID = sender.getEntity() instanceof ServerPlayer ? sender.getEntity().getUUID() : null;
        boolean isConsole = sender.getEntity() == null;

        Context tebexCtx = Context.from(isConsole, senderName, senderUUID, "tebex", "", new UUID(0L, 0L), new String[0]);
        if (isConsole || platform.hasPermission(senderName, "tebex.base")) {
            sender.sendSystemMessage(Component.literal(Responder.formatFancy(tebexCtx, "Welcome to Tebex!")));
            sender.sendSystemMessage(Component.literal(Responder.formatFancy(tebexCtx, "This server is running version {0}", "v" + platform.getPluginVersion())));
            return;
        }

        sender.sendSystemMessage(Component.literal(Responder.formatError(tebexCtx, "Unrecognized command or no permission.")));
    }
}
