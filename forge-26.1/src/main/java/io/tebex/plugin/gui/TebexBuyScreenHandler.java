package io.tebex.plugin.gui;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;

import java.util.HashMap;

public class TebexBuyScreenHandler extends ChestMenu {
    private boolean cancelled = false;
    private final HashMap<Integer, TebexGuiItem> guiItems;

    public TebexBuyScreenHandler(MenuType<?> type, int syncId, Inventory playerInventory, Container inventory, int rows, HashMap<Integer, TebexGuiItem> guiItems) {
        super(type, syncId, playerInventory, inventory, rows);
        this.guiItems = guiItems;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (cancelled || input != ContainerInput.PICKUP) {
            return;
        }

        if (slotId >= 0 && slotId < getContainer().getContainerSize()) {
            TebexGuiItem item = guiItems.get(slotId);
            if (item != null && item.getAction() != null) {
                item.getAction().execute(this);
            }
        }

        super.clicked(slotId, button, input, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public void setCancelled(boolean value) {
        cancelled = value;
    }
}
