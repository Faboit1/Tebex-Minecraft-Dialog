package io.tebex.plugin.gui;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;

public class TebexItemBuilder {
    private String displayName;
    private final Item material;
    private List<String> lore;
    private DataComponentType[] hideFlags;
    private boolean isEnchanted;

    private TebexItemBuilder(Item material) {
        this.material = material;
    }

    public static TebexItemBuilder from(Item material) {
        return new TebexItemBuilder(material);
    }

    public TebexGuiItem asGuiItem(TebexGuiAction<TebexBuyScreenHandler> clickAction) {
        return new TebexGuiItem(buildItemStack(), clickAction);
    }

    public ItemStack buildItemStack() {
        ItemStack stack = new ItemStack(material);

        if (lore != null) {
            ItemLore itemLore = ItemLore.EMPTY;
            for (String loreEntry : lore) {
                itemLore = itemLore.withLineAdded(Component.nullToEmpty(loreEntry));
            }
            stack.set(DataComponents.LORE, itemLore);
        }

        stack.set(DataComponents.CUSTOM_NAME, Component.nullToEmpty(displayName));

        if (isEnchanted) {
            stack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        }

        return stack;
    }

    public void enchant() {
        isEnchanted = true;
    }

    public TebexItemBuilder hideFlags(DataComponentType... itemFlags) {
        hideFlags = itemFlags;
        return this;
    }

    public TebexItemBuilder name(String name) {
        displayName = name;
        return this;
    }

    public TebexItemBuilder lore(List<String> lore) {
        this.lore = lore;
        return this;
    }
}
