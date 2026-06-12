package io.tebex.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.fixes.ItemIdFix;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ItemUtil {
    private static final Map<String, Item> ITEM_CACHE = new HashMap<>();

    public static Optional<Item> fromString(String material) {
        if (material == null || material.isBlank()) {
            return Optional.empty();
        }

        String cacheKey = material.trim();
        if (ITEM_CACHE.containsKey(cacheKey)) {
            return Optional.of(ITEM_CACHE.get(cacheKey));
        }

        ResourceLocation id = ResourceLocation.tryParse(toMinecraftIdentifier(cacheKey));
        if (id == null) {
            return Optional.empty();
        }

        Optional<Item> item = resolveItem(id);
        if (item.isEmpty()) {
            item = toCompatibleIdentifier(cacheKey).flatMap(ItemUtil::resolveItem);
        }

        item.ifPresent(value -> ITEM_CACHE.put(cacheKey, value));
        return item;
    }

    private static Optional<Item> resolveItem(ResourceLocation id) {
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
        if (item.isPresent()) {
            return item;
        }

        Optional<Block> blockItem = BuiltInRegistries.BLOCK.getOptional(id);
        if (blockItem.isPresent()) {
            return Optional.of(blockItem.get().asItem());
        }

        return Optional.empty();
    }

    private static String toMinecraftIdentifier(String material) {
        String normalized = material.toLowerCase().replace(' ', '_');
        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }

    private static Optional<ResourceLocation> toCompatibleIdentifier(String material) {
        String normalized = material.toLowerCase().replace(' ', '_');
        if (normalized.contains(":")) {
            String[] split = normalized.split(":", 2);
            if (split[0].matches("\\d+")) {
                return fromLegacyNumericId(split[0]);
            }

            if (split[0].equals("minecraft")) {
                return Optional.ofNullable(ResourceLocation.tryParse("minecraft:" + split[1]));
            }

            return Optional.empty();
        }

        if (normalized.matches("\\d+")) {
            return fromLegacyNumericId(normalized);
        }

        return Optional.ofNullable(ResourceLocation.tryParse("minecraft:" + normalized));
    }

    private static Optional<ResourceLocation> fromLegacyNumericId(String id) {
        try {
            String itemId = ItemIdFix.getItem(Integer.parseInt(id));
            if ("minecraft:air".equals(itemId)) {
                return Optional.empty();
            }

            return Optional.ofNullable(ResourceLocation.tryParse(itemId));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
