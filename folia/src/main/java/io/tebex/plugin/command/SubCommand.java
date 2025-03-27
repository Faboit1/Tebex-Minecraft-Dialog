package io.tebex.plugin.command;

import io.tebex.plugin.FoliaPlatform;
import io.tebex.plugin.TebexPlugin;
import org.bukkit.command.CommandSender;

public abstract class SubCommand {
    private final FoliaPlatform platform;
    private final String name;
    private final String permission;

    public SubCommand(FoliaPlatform platform, String name, String permission) {
        this.platform = platform;
        this.name = name;
        this.permission = permission;
    }

    public abstract void execute(final CommandSender sender, final String[] args);

    public FoliaPlatform getPlatform() {
        return platform;
    }

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }

    public abstract String getDescription();
    public String getUsage() {
        return "";
    }
}
