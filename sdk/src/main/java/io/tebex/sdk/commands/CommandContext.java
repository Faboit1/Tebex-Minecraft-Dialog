package io.tebex.sdk.commands;

import java.util.UUID;

public class CommandContext {
    public final String runnerUsername;
    public final UUID runnerUUID;
    public final String targetUsername;
    public final UUID targetUUID;
    public final String fullCommand;
    public final String commandName;
    public final String[] arguments;

    private CommandContext(String runnerUsername, UUID runnerUUID, String targetUsername, UUID targetUUID, String command, String[] arguments){
        this.runnerUsername = runnerUsername == null ? "" : runnerUsername;
        this.runnerUUID = runnerUUID;
        this.targetUsername = targetUsername == null ? "" : targetUsername;
        this.targetUUID = targetUUID;
        this.fullCommand = command;
        this.arguments = arguments;

        if (arguments.length > 0) {
            this.commandName = arguments[0];
        } else {
            this.commandName = "";
        }
    }

    public static CommandContext from(String runnerUsername, UUID runnerUUID, String targetUsername, UUID targetUUID, String command, String[] arguments) {
        return new CommandContext(runnerUsername, runnerUUID, targetUsername, targetUUID, command, arguments);
    }

    public void tellSender(String message) {
        TebexCommands.getPlatform().sendPlayerMessage(this.runnerUsername, message);
    }

    public void tellTarget(String message) {
        TebexCommands.getPlatform().sendPlayerMessage(this.targetUsername, message);
    }
}
