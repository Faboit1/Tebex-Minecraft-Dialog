package io.tebex.plugin.manager;

import io.tebex.plugin.BukkitPluginPlatform;
import io.tebex.sdk.obj.Category;
import io.tebex.sdk.obj.CategoryPackage;
import io.tebex.sdk.obj.SubCategory;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * Decides how long a free package stays on cooldown for a player, and tracks which
 * players still have something free waiting for them.
 *
 * <p>The cooldown for a package is resolved in this order:
 * <ol>
 *     <li>{@code free-packages.cooldowns.<packageId>} in config.yml</li>
 *     <li>{@code meta.cooldown_seconds} from the Tebex API, when the store exposes it</li>
 *     <li>{@code free-packages.default-cooldown} in config.yml</li>
 * </ol>
 * The config is checked first because the Tebex plugin API does not return package
 * meta for every store, so relying on it alone leaves free packages permanently
 * claimable.
 */
public class FreePackageTracker {
    private final BukkitPluginPlatform platform;

    public FreePackageTracker(BukkitPluginPlatform platform) {
        this.platform = platform;
    }

    /**
     * @return The cooldown in seconds for this package, or 0 when it has no cooldown.
     */
    public int resolveCooldownSeconds(CategoryPackage pkg) {
        ConfigurationSection cooldowns = platform.getPlugin().getConfig()
                .getConfigurationSection("free-packages.cooldowns");

        if (cooldowns != null) {
            String key = String.valueOf(pkg.getId());
            if (cooldowns.contains(key)) {
                return Math.max(0, cooldowns.getInt(key));
            }
        }

        if (pkg.getCooldownSeconds() > 0) {
            return pkg.getCooldownSeconds();
        }

        return Math.max(0, platform.getPlugin().getConfig().getInt("free-packages.default-cooldown", 0));
    }

    /**
     * @return Whether this player can claim the package for free right now.
     */
    public boolean isClaimable(CategoryPackage pkg, String playerName) {
        if (!pkg.isFree()) return false;

        int cooldownSeconds = resolveCooldownSeconds(pkg);
        if (cooldownSeconds <= 0) return true;

        CooldownManager cooldowns = platform.getCooldownManager();
        if (cooldowns == null) return true;

        return !cooldowns.isOnCooldown(playerName, pkg.getId(), cooldownSeconds);
    }

    /**
     * Starts the cooldown for a free package the player has just claimed.
     */
    public void recordClaim(CategoryPackage pkg, String playerName) {
        if (!pkg.isFree()) return;

        int cooldownSeconds = resolveCooldownSeconds(pkg);
        if (cooldownSeconds <= 0) return;

        CooldownManager cooldowns = platform.getCooldownManager();
        if (cooldowns == null) return;

        cooldowns.recordClaim(playerName, pkg.getId(), cooldownSeconds);
    }

    /**
     * @return Whether the player has any free package available across the whole store.
     */
    public boolean hasClaimable(String playerName) {
        List<Category> categories = platform.getStoreCategories();
        if (categories == null) return false;

        for (Category category : categories) {
            if (anyClaimable(category.getPackages(), playerName)) return true;

            if (category.getSubCategories() != null) {
                for (SubCategory subCategory : category.getSubCategories()) {
                    if (anyClaimable(subCategory.getPackages(), playerName)) return true;
                }
            }
        }

        return false;
    }

    /**
     * @return Whether any package in this list is claimable for free by the player.
     */
    public boolean anyClaimable(List<CategoryPackage> packages, String playerName) {
        if (packages == null) return false;

        for (CategoryPackage pkg : packages) {
            if (isClaimable(pkg, playerName)) return true;
        }

        return false;
    }
}
