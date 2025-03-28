package io.tebex.sdk.commands;

import java.util.function.Consumer;

public class Command {
    private final int numArgsRequired;
    private final String name;
    private final String usage;
    private final String description;
    private final Consumer<CommandContext> handler;

    Command (String name, String usage, String description, Consumer<CommandContext> handler) {
        this.name = name;
        this.usage = usage;
        this.description = description;
        this.handler = handler;

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

    public final Consumer<CommandContext> getHandler() {
        return handler;
    }
}
