package io.tebex.plugin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.tebex.plugin.FabricPluginPlatform;
import io.tebex.sdk.commands.PlayerCommand;
import io.tebex.sdk.commands.Context;
import io.tebex.sdk.commands.Responder;
import io.tebex.sdk.commands.TebexCommands;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TebexCommandExecutor {
    private final FabricPluginPlatform platform;

    public TebexCommandExecutor(FabricPluginPlatform platform) {
        this.platform = platform;
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerPlatformConfig config = (ServerPlatformConfig) platform.getPlatformConfig();
        if (config.isBuyCommandEnabled()) {
            BuyCommand buyCommand = new BuyCommand(platform);
            dispatcher.register(literal(config.getBuyCommandName()).executes(buyCommand::execute));
            platform.debug("buy command registered as: " + config.getBuyCommandName());
        }

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

            ArgumentBuilder<ServerCommandSource, ?> subCommand;
            if (args.length == 3) {
                subCommand = literal(command.getCommandName())
                        .then(argument(args[0], StringArgumentType.string())
                        .then(argument(args[1], StringArgumentType.string())
                        .then(argument(args[2], StringArgumentType.string())
                        .executes(context -> { run(command,context); return 1;}))));
            } else if (args.length == 2) {
                subCommand = literal(command.getCommandName())
                        .then(argument(args[0], StringArgumentType.string())
                        .then(argument(args[1], StringArgumentType.string())
                        .executes(context -> { run(command,context); return 1;})));
            } else if (args.length == 1) {
                subCommand = literal(command.getCommandName()).then(argument(args[0], StringArgumentType.string()).executes(context -> { run(command,context); return 1;}));
            } else if (args.length == 0) {
                subCommand = literal(command.getCommandName()).executes(context -> { run(command,context); return 1;});
            } else {
                return;
            }

            // Final root: tebex -> subcommand -> args -> execute
            LiteralArgumentBuilder<ServerCommandSource> root = literal("tebex").requires(Permissions.require(command.getPermission(), false)
                    .or(source -> {
                        try {
                            return source.hasPermissionLevel(4);
                        } catch (NoSuchMethodError e) {
                            return platform.hasPermission(source.getName(), "tebex.admin");
                        }
                    })).then(subCommand);
            dispatcher.register(root);
        });
    }

    public void run(PlayerCommand command, com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
        ServerCommandSource sender = context.getSource();
        String senderName = sender.getName();
        UUID senderUUID = sender.getEntity() instanceof ServerPlayerEntity ? sender.getEntity().getUuid() : null;
        boolean isConsole = sender.getEntity() == null;

        String[] splitInput = context.getInput().split("\\s+");
        String[] tokens = splitInput.length > 1 ? Arrays.copyOfRange(splitInput, 1, splitInput.length) : new String[0];

        String targetName = "";
        UUID targetUUID = new UUID(0L, 0L);
        for (String token : tokens) {
            if (targetName.isEmpty()) {
                ServerPlayerEntity targetPlayer = platform.getPlayer(token);
                if (targetPlayer != null) {
                    targetName = targetPlayer.getName().getString();
                    targetUUID = targetPlayer.getUuid();
                }
            }
        }

        Context tebexCtx = Context.from(isConsole, senderName, senderUUID, "tebex " + command.getCommandName(), targetName, targetUUID, tokens);
        if (tokens.length == 0 && platform.hasPermission(senderName, "tebex.base")) {
            sender.sendMessage(Text.literal(Responder.formatFancy(tebexCtx, "Welcome to Tebex!")));
            sender.sendMessage(Text.literal(Responder.formatFancy(tebexCtx, "This server is running version {0}", "v" + platform.getPluginVersion())));
            return;
        }

        TebexCommands.process(tebexCtx, future -> future.thenAccept(msgs -> {
            for (String msg : msgs) {
                sender.sendMessage(Text.literal(msg));
            }
        }));
    }
}
