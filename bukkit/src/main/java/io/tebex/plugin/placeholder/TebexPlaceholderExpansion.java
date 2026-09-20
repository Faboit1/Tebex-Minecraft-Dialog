package io.tebex.plugin.placeholder;

import io.tebex.plugin.BukkitPluginPlatform;
import io.tebex.plugin.manager.CooldownManager;
import io.tebex.plugin.manager.SpendTracker;
import io.tebex.plugin.util.MiniMessageUtil;
import io.tebex.plugin.util.MoneyUtil;
import io.tebex.sdk.obj.Category;
import io.tebex.sdk.obj.CategoryPackage;
import io.tebex.sdk.obj.SubCategory;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Locale;

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
        FileConfiguration config = platform.getPlugin().getConfig();

        if (params.equalsIgnoreCase("free_in_store")) {
            return hasFreeForPlayer(playerName) ? "true" : "false";
        }
        if (params.equalsIgnoreCase("free_marker")) {
            if (hasFreeForPlayer(playerName)) {
                String marker = config.getString("gui.dialog.free-marker", "<red>[FREE]<reset> ");
                return MiniMessageUtil.toSection(marker);
            }
            return "";
        }
        if (params.equalsIgnoreCase("currency")) {
            return MoneyUtil.currency(config);
        }

        String spend = moneySpent(playerName, params, config);
        if (spend != null) return spend;

        return null;
    }

    /**
     * Handles the {@code money_spent} family. Figures are served from the cache the
     * tracker keeps, because PlaceholderAPI resolves on the main thread and a lookup
     * over HTTP would stall the server.
     */
    private String moneySpent(String playerName, String params, FileConfiguration config) {
        String key = params.toLowerCase(Locale.ROOT);
        if (!key.startsWith("money_spent")) return null;

        // "_raw" gives the unformatted number, for comparisons and maths in other plugins.
        boolean raw = key.endsWith("_raw");
        if (raw) key = key.substring(0, key.length() - "_raw".length());

        SpendTracker tracker = platform.getSpendTracker();
        if (tracker == null) return null;

        SpendTracker.Spend spend = tracker.get(playerName);

        Double amount;
        if (key.equals("money_spent")) {
            amount = spend.getTotal();
        } else if (key.equals("money_spent_this_month")) {
            amount = spend.getThisMonth();
        } else if (key.equals("money_spent_this_week")) {
            amount = spend.getThisWeek();
        } else if (key.equals("money_spent_today")) {
            amount = spend.getToday();
        } else {
            return null;
        }

        if (raw) return String.valueOf(amount);
        return MoneyUtil.format(config, amount);
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
