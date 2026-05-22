package io.tebex.plugin.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ListingGui {
    private Container inventory;
    private String title;
    private final int rows;
    private final HashMap<Integer, TebexGuiItem> guiItems;
    private final MenuType<ChestMenu> containerScreenHandler;
    private final ServerPlayer player;

    public ListingGui(int rows, MenuType<ChestMenu> screenHandlerType, ServerPlayer player) {
        this.rows = rows;
        this.inventory = new SimpleContainer(rows * 9);
        this.guiItems = new HashMap<>();
        this.containerScreenHandler = screenHandlerType;
        this.player = player;
    }

    public ListingGui setTitle(String title) {
        this.title = title;
        return this;
    }

    public int getRows() {
        return rows;
    }

    public void addItem(TebexGuiItem guiItem) {
        int nextSlot = 0;
        while (guiItems.containsKey(nextSlot) && nextSlot < rows * 9) {
            nextSlot++;
        }
        guiItems.put(nextSlot, guiItem);
    }

    public void addItem(int index, TebexGuiItem guiItem) {
        guiItems.put(index, guiItem);
    }

    public void open() {
        inventory.clearContent();

        for (Map.Entry<Integer, TebexGuiItem> entry : guiItems.entrySet()) {
            ItemStack stack = entry.getValue().getStack();
            inventory.setItem(entry.getKey(), stack);
        }

        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, playerEntity) -> new TebexBuyScreenHandler(containerScreenHandler, syncId, inv, inventory, rows, guiItems),
                Component.literal(title)
        ));
    }

    public void close() {
        player.closeContainer();
    }
}
