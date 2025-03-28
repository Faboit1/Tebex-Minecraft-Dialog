package io.tebex.sdk.commands;

import java.util.function.Consumer;

public class Command {
    private final int numArgsRequired;
    private final String name;
    private final String usage;
    private final String description;
    private final String permission;
    private final Consumer<CommandContext> handler;

    Command (String name, String usage, String description, Consumer<CommandContext> handler) {
        this.name = name;
        this.usage = usage;
        this.description = description;
        this.handler = handler;
        this.permission = TebexCommands.TEBEX_COMMAND_PREFIX + "." + name;

        // Number of arguments are occurrences of opening '<' tags in the usage string
        this.numArgsRequired = (int) usage.chars().filter(ch -> ch == '<').count();
    }

    public int getNumArgsRequired() {
        return numArgsRequired;
    }

    public String getName() {
        return name;
    }

    public String getUsage() {
        return usage;
    }

    public String getDescription() {
        return description;
    }

    public String getPermission() {
        return permission;
    }

    public final Consumer<CommandContext> getHandler() {
        return handler;
    }
}
