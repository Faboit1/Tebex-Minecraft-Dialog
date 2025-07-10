package io.tebex.plugin.command;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.plugin.FabricPlatform;
import io.tebex.plugin.TebexFabricPlugin;
import io.tebex.plugin.gui.BuyGUI;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class BuyCommand {
    private final FabricPlatform platform;
    public BuyCommand(FabricPlatform platform) {
        this.platform = platform;
    }

    public int execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource source = context.getSource();

        try {
            ServerPlayerEntity player = source.getPlayer();
            new BuyGUI(platform).open(player);
        } catch (Exception e) {
            e.printStackTrace();
            source.sendMessage(Text.of("§b[Tebex] §7You must be a player to run this command!"));
        }

        return 1;
    }
}
