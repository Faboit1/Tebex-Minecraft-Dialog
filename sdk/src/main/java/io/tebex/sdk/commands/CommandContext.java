package io.tebex.sdk.commands;

import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;

public class CommandContext {
    private boolean senderIsConsole;
    private String senderUsername;
    private UUID senderUUID;
    private String targetUsername;
    private UUID targetUUID;
    private String fullCommand;
    private String commandName;
    private String[] arguments;

    private CommandContext(boolean senderIsConsole, String senderUsername, UUID senderUUID, String targetUsername, UUID targetUUID, String command, String[] arguments){
        this.senderIsConsole = senderIsConsole;
        this.senderUsername = senderUsername == null ? "" : senderUsername;
        this.senderUUID = senderUUID;
        this.targetUsername = targetUsername == null ? "" : targetUsername;
        this.targetUUID = targetUUID;
        this.fullCommand = command;


        if (arguments.length > 0) {
            this.commandName = arguments[0];
            this.arguments = Arrays.copyOfRange(arguments, 1, arguments.length);
        } else {
            this.commandName = "";
            this.arguments = new String[0];
        }
    }

    public static CommandContext from(boolean isConsole, String senderUsername, UUID senderUUID, String fullCommand, String[] allArguments) {
        return new CommandContext(isConsole, senderUsername,
                senderUUID, "",
                new UUID(0L,0L), fullCommand, allArguments);
    }

    public CommandContext withSenderUsername(String username) {
        this.senderUsername = username;
        return this;
    }

    public CommandContext withSenderUUID(UUID uuid) {
        this.senderUUID = uuid;
        return this;
    }

    public CommandContext asConsole() {
        this.senderIsConsole = true;
        return this;
    }

    public boolean isFromConsole() {
        return this.senderIsConsole;
    }

    public CommandContext withArgs(String[] args) {
        this.arguments = args;
        return this;
    }

    public CommandContext withTarget(String username, UUID uuid) {
        this.targetUsername = username;
        this.targetUUID = uuid;
        return this;
    }

    public String getFullCommand() {
        return fullCommand;
    }

    public String getCommandName() {
        return commandName;
    }

    public String[] getArgs() {
        return arguments;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public UUID getSenderUUID() {
        return senderUUID;
    }

    public void tellSender(String message) {
        if (this.senderIsConsole) {
            //FIXME proper response for RCON
            TebexCommands.getPlatform().log(Level.INFO, message);
        } else {
            TebexCommands.getPlatform().sendPlayerMessage(senderUsername, message);
        }
    }

    public void tellTarget(String message) {
        TebexCommands.getPlatform().sendPlayerMessage(targetUsername, message);
    }

    public String getSenderUsername() {
        return senderUsername;
    }
}
