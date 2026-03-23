package io.tebex.sdk.obj;

import io.tebex.sdk.platform.PluginPlatform;
import lombok.Data;

@Data
public class QueuedCommand {
    private final int id;
    private final String command;
    private final Integer payment;
    private final Integer packageId; // may be null
    private final Integer delay;
    private final Integer requiredSlots;
    private final QueuedPlayer player;
    private final boolean online;

    public String getParsedCommand() {
        return getParsedCommand(null);
    }

    public String getParsedCommand(PluginPlatform platform) {
        String parsedCommand = command;

        if (player != null) {
            String commandPlayerId = platform == null
                    ? player.getDefaultCommandIdentifier()
                    : platform.resolveCommandPlayerId(player);
            if (commandPlayerId == null) {
                commandPlayerId = player.getDefaultCommandIdentifier();
            }

            parsedCommand = parsedCommand.replace("{username}", player.getName());
            parsedCommand = parsedCommand.replace("{name}", player.getName());
            parsedCommand = parsedCommand.replace("{id}", commandPlayerId);
            parsedCommand = parsedCommand.replace("{uuid}", commandPlayerId);
        }

        return parsedCommand;
    }
}
