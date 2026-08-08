package io.tebex.plugin.placeholder;

import io.tebex.plugin.BukkitPluginPlatform;
import io.tebex.plugin.manager.CooldownManager;
import io.tebex.plugin.util.MiniMessageUtil;
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
        String playerName = player != null ? player.getName() : null;

        if (params.equalsIgnoreCase("free_in_store")) {
            return hasFreeForPlayer(playerName) ? "true" : "false";
        }
        if (params.equalsIgnoreCase("free_marker")) {
            if (hasFreeForPlayer(playerName)) {
                String marker = platform.getPlugin().getConfig()
                        .getString("gui.dialog.free-marker", "<red>[FREE]<reset> ");
                return MiniMessageUtil.toSection(marker);
            }
            return "";
        }
        return null;
    }

    private boolean hasFreeForPlayer(String playerName) {
        List<Category> categories = platform.getStoreCategories();
        if (categories == null) return false;
        for (Category category : categories) {
            if (hasFreeAvailable(category.getPackages(), playerName)) return true;
            if (category.getSubCategories() != null) {
                for (SubCategory sub : category.getSubCategories()) {
                    if (hasFreeAvailable(sub.getPackages(), playerName)) return true;
                }
            }
        }
        return false;
    }

    private boolean hasFreeAvailable(List<CategoryPackage> packages, String playerName) {
        CooldownManager cm = platform.getCooldownManager();
        for (CategoryPackage pkg : packages) {
            if (pkg.isFree()) {
                if (!pkg.hasCooldown()) return true;
                if (playerName == null) return true;
                if (cm == null || !cm.isOnCooldown(playerName, pkg.getId(), pkg.getCooldownSeconds())) {
                    return true;
                }
            }
        }
        return false;
    }
}
