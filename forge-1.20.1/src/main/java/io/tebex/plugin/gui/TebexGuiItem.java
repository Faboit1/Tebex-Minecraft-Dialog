package io.tebex.plugin.gui;

import net.minecraft.world.item.ItemStack;

public class TebexGuiItem {
    private final TebexGuiAction<TebexBuyScreenHandler> action;
    private final ItemStack stack;

    public TebexGuiItem(ItemStack stack, TebexGuiAction<TebexBuyScreenHandler> action) {
        this.action = action;
        this.stack = stack;
    }

    public ItemStack getStack() {
        return stack;
    }

    public TebexGuiAction<TebexBuyScreenHandler> getAction() {
        return action;
    }

    @Override
    public String toString() {
        return stack.toString();
    }
}
