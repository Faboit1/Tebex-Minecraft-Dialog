package io.tebex.plugin.placeholder;

import io.tebex.plugin.BukkitPluginPlatform;
import io.tebex.sdk.obj.Category;
import io.tebex.sdk.obj.CategoryPackage;
import io.tebex.sdk.obj.SubCategory;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.List;

public class TebexPlaceholderExpansion extends PlaceholderExpansion {
    private final BukkitPluginPlatform platform;

    public TebexPlaceholderExpansion(BukkitPluginPlatform platform) {
        this.platform = platform;
    }

    @Override
    public String getIdentifier() {
        return "tebex";
    }

    @Override
    public String getAuthor() {
        return "Tebex";
    }

    @Override
    public String getVersion() {
        return platform.getPlugin().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params.equalsIgnoreCase("free_in_store")) {
            return hasFreeItems() ? "true" : "false";
        }
        if (params.equalsIgnoreCase("free_marker")) {
            if (hasFreeItems()) {
                String marker = platform.getPlugin().getConfig()
                        .getString("gui.dialog.free-marker", "&c[FREE]&r ");
                return marker.replace("&", "§");
            }
            return "";
        }
        return null;
    }

    private boolean hasFreeItems() {
        List<Category> categories = platform.getStoreCategories();
        if (categories == null) return false;
        for (Category category : categories) {
            if (category.hasFreePackage()) return true;
        }
        return false;
    }
}
