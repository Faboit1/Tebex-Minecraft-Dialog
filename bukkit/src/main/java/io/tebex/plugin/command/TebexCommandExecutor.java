package io.tebex.plugin.command;

import com.google.common.collect.ImmutableList;
import io.tebex.sdk.commands.CommandContext;
import io.tebex.sdk.commands.CommandResponder;
import io.tebex.sdk.commands.TebexCommands;
import io.tebex.sdk.platform.Platform;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TebexCommandExecutor implements TabExecutor {
    private final Platform platform;

    public TebexCommandExecutor(Platform platform) {
        this.platform = platform;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Identify sender information
        String senderName = sender.getName();
        UUID senderUUID = null;
        if (sender instanceof Player) {
            senderUUID = ((Player) sender).getUniqueId();
        }

        // Check for console
        boolean isConsole = sender instanceof ConsoleCommandSender;

        // Identify any player targets
        String targetName = "";
        UUID targetUUID = new UUID(0L,0L);

        // Build the full command and list of arguments, checking if any of the args is a player.
        StringBuilder fullCommand = new StringBuilder(command.getName());
        for (String arg : args) {
            // Assign the target player if we find their username in the args list
            if (targetName.isEmpty()) {
                Player targetPlayer = platform.getPlayer(arg);
                if (targetPlayer != null) {
                    targetName = targetPlayer.getName();
                    targetUUID = targetPlayer.getUniqueId();
                }
            }

            // Build the full command by appending the arg to the base command
            fullCommand.append(" ").append(arg);
        }

        // Build the command context with the information we have so far for responding.
        CommandContext context = CommandContext.from(isConsole, senderName, senderUUID, fullCommand.toString(), args);
        if (targetName != null && !targetName.isEmpty()) {
            context = context.withTarget(targetName, targetUUID);
        }

        // Show splash for no args /tebex command
        if (args.length == 0 && sender.hasPermission("tebex.tebex")) {
            CommandResponder.tellFancy(context, "Welcome to Tebex!");
            CommandResponder.tellFancy(context, "This server is running version {0}", "v" + platform.getVersion());
            return true;
        }

        // Pass context to the command handler
        return TebexCommands.process(context);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1) {
            return TebexCommands.getAllowedCommands(sender.getName())
                    .keySet()
                    .stream()
                    .filter(s -> s.startsWith(args[0]))
                    .collect(Collectors.toList());
        }

        return ImmutableList.of();
    }
}
