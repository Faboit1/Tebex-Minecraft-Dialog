package io.tebex.plugin.gui;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public class TebexItemBuilder {
    private String displayName;
    private final Item material;
    private List<String> lore;
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

        if (displayName != null) {
            stack.setHoverName(Component.nullToEmpty(displayName));
        }

        if (lore != null && !lore.isEmpty()) {
            ListTag loreTag = new ListTag();
            for (String loreEntry : lore) {
                loreTag.add(StringTag.valueOf(Component.Serializer.toJson(Component.nullToEmpty(loreEntry))));
            }
            stack.getOrCreateTagElement("display").put("Lore", loreTag);
        }

        if (isEnchanted) {
            stack.enchant(Enchantments.UNBREAKING, 1);
        }

        return stack;
    }

    public void enchant() {
        isEnchanted = true;
    }

    public TebexItemBuilder hideFlags(Object... ignored) {
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
