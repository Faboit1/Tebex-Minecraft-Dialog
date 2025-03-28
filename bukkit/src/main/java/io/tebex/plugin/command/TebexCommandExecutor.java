package io.tebex.plugin.command;

import com.google.common.collect.ImmutableList;
import io.tebex.sdk.commands.CommandContext;
import io.tebex.sdk.commands.TebexCommands;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TebexCommandExecutor implements TabExecutor {
    public TebexCommandExecutor() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Identify sender information
        String senderName = sender.getName();
        UUID senderUUID = null;
        if (sender instanceof Player) {
            senderUUID = ((Player) sender).getUniqueId();
        }

        // Identify any player targets
        String targetName = null;
        UUID targetUUID = null;

        // Build the command name (tebex + command)
        if (args.length == 0) {
            //FIXME Show Tebex splash
//            sender.sendMessage("§8[Tebex] §7Welcome to Tebex!");
//            sender.sendMessage("§8[Tebex] §7This server is running version §fv" + commandManager.getPlatform().getPlugin().getDescription().getVersion() + "§7.");
//            return true
        }

        // First argument will be sub-command name
        StringBuilder permission = new StringBuilder();
        permission.append(TebexCommands.TEBEX_COMMAND_PREFIX);
        permission.append(".");
        permission.append(args[0]);

        if (!sender.hasPermission(permission.toString())) {
            //FIXME Show no permission
        }

        // Build the full command and list of arguments
        StringBuilder fullCommand = new StringBuilder(command.getName());
        for (String arg : args) {
            fullCommand.append(" ").append(arg);
        }

        // Parse arguments into a context and pass to common command handler
        CommandContext context = CommandContext.from(senderName, senderUUID, targetName, targetUUID, fullCommand.toString(), args);
        return TebexCommands.process(context);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1) {
            return TebexCommands.getCommands()
                    .keySet()
                    .stream()
                    .filter(s -> s.startsWith(args[0]))
                    .collect(Collectors.toList());
        }

        return ImmutableList.of();
    }
}
