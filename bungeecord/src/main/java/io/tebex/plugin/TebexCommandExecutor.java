package io.tebex.plugin;

import io.tebex.sdk.commands.CommandContext;
import io.tebex.sdk.commands.CommandResponder;
import io.tebex.sdk.commands.TebexCommands;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.UUID;

public class TebexCommandExecutor extends Command {
    private final BungeePlatform platform;
    public TebexCommandExecutor(BungeePlatform platform) {
        super("tebex");
        this.platform = platform;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String commandName = args[0];

        // Identify sender information
        String senderName = sender.getName();
        UUID senderUUID = null;
        if (sender instanceof ProxiedPlayer) {
            senderUUID = ((ProxiedPlayer) sender).getUniqueId();
        }

        // Identify any player targets
        String targetName = "";
        UUID targetUUID = new UUID(0L,0L);

        // Build the full command and list of arguments, checking if any of the args is a player.
        StringBuilder fullCommand = new StringBuilder(commandName);
        for (String arg : args) {
            // Assign the target player if we find their username in the args list
            if (targetName.isEmpty()) {
                ProxiedPlayer targetPlayer = platform.getPlayer(arg);
                if (targetPlayer != null) {
                    targetName = targetPlayer.getName();
                    targetUUID = targetPlayer.getUniqueId();
                }
            }

            // Build the full command by appending the arg to the base command
            fullCommand.append(" ").append(arg);
        }

        // Build the command context with the information we have so far for responding.
        CommandContext context = CommandContext.from(false, senderName, senderUUID, fullCommand.toString(), args);
        if (targetName != null && !targetName.isEmpty()) {
            context = context.withTarget(targetName, targetUUID);
        }

        // Show splash for no args /tebex command
        if (args.length == 1 && sender.hasPermission("tebex.tebex")) {
            sender.sendMessage(TextComponent.fromLegacyText(CommandResponder.formatFancy(context, "Welcome to Tebex!")));
            sender.sendMessage(TextComponent.fromLegacyText(CommandResponder.formatFancy(context,"This server is running version {0}", "v" + platform.getVersion())));
            return;
        }

        // Pass context to the command handler, but respond via command sender so it's sent through the appropriate
        // channels (RCON, Console, Player).
        TebexCommands.process(context, (future) -> {
            future.thenAccept((responseArr) -> {
                for (String message : responseArr) {
                    sender.sendMessage(TextComponent.fromLegacyText(message));
                }
            });
        });
    }
}
