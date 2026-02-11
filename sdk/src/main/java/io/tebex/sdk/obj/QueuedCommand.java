package io.tebex.sdk.obj;

import io.tebex.sdk.util.UUIDUtil;
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
        String parsedCommand = command;

        if (player != null) {
            parsedCommand = parsedCommand.replace("{username}", player.getName());
            parsedCommand = parsedCommand.replace("{name}", player.getName());

            if (isMissingUuid(player.getUuid())) {
                // Offline/unauthenticated players: fall back to username for both placeholders
                parsedCommand = parsedCommand.replace("{id}", player.getName());
                parsedCommand = parsedCommand.replace("{uuid}", player.getName());
            } else {
                parsedCommand = parsedCommand.replace("{id}", player.getUuid());
                parsedCommand = parsedCommand.replace("{uuid}", player.getUuid());
            }
        }

        return parsedCommand;
    }

    private boolean isMissingUuid(String uuid) {
        if (uuid == null) return true;

        String trimmed = uuid.trim();
        return trimmed.isEmpty()
                || trimmed.equalsIgnoreCase("null")
                || trimmed.equalsIgnoreCase(UUIDUtil.EMPTY_UUID.toString());
    }
}
