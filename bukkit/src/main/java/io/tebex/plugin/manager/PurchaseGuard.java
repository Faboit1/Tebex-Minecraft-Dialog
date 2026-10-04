package io.tebex.plugin.manager;

import io.tebex.plugin.BukkitPluginPlatform;
import io.tebex.sdk.obj.Category;
import io.tebex.sdk.obj.CategoryPackage;
import io.tebex.sdk.obj.SubCategory;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The single authorisation point for starting a checkout.
 *
 * <p>Every path that can begin a purchase goes through here: the dialog shop, the chest
 * GUI, and the {@code /buy package <id>} command a player can type by hand. Checks that
 * live only in the code that draws a button are not checks at all — the button's command
 * is public, so anything the button can do, a player can do directly and repeatedly.</p>
 *
 * <p>It enforces three things:</p>
 * <ol>
 *     <li>the package is one the store actually published, so an arbitrary or guessed ID
 *         cannot be turned into a checkout link;</li>
 *     <li>a free package on cooldown is refused rather than merely displayed as priced;</li>
 *     <li>a short per-player interval, so a double click or a held-down macro cannot
 *         start several checkouts before the first has recorded anything.</li>
 * </ol>
 */
public class PurchaseGuard {
    public enum Decision {
        /** Checkout may proceed. The claim, if any, has been recorded. */
        ALLOWED,
        /** No such package in the published listing. */
        UNKNOWN_PACKAGE,
        /** A free package the player has already claimed within its cooldown. */
        ON_COOLDOWN,
        /** Another purchase attempt from this player arrived moments ago. */
        TOO_FAST,
        /** The listing has not been fetched yet, so nothing can be validated. */
        LISTING_UNAVAILABLE
    }

    private final BukkitPluginPlatform platform;
    private final Map<String, Long> lastAttempt = new ConcurrentHashMap<>();

    public PurchaseGuard(BukkitPluginPlatform platform) {
        this.platform = platform;
    }

    /**
     * Decides whether this player may start a checkout for this package, and records the
     * free-package claim when they may.
     *
     * <p>Deliberately not split into a separate "check" and "record" pair: a caller that
     * checked and then acted a tick later would reopen the race this exists to close.</p>
     */
    public synchronized Decision authorise(int packageId, String playerName) {
        if (playerName == null || playerName.isEmpty()) return Decision.UNKNOWN_PACKAGE;

        List<Category> categories = platform.getStoreCategories();
        if (categories == null || categories.isEmpty()) return Decision.LISTING_UNAVAILABLE;

        CategoryPackage pkg = findPackage(categories, packageId);
        if (pkg == null) return Decision.UNKNOWN_PACKAGE;

        String key = playerName.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        long interval = purchaseIntervalMillis();
        Long previous = lastAttempt.get(key);
        if (previous != null && now - previous < interval) {
            return Decision.TOO_FAST;
        }

        FreePackageTracker tracker = platform.getFreePackageTracker();
        if (enforceCooldown() && pkg.isFree() && !tracker.isClaimable(pkg, playerName)) {
            // Still rate-limit a refused attempt, so a macro cannot hammer the database.
            lastAttempt.put(key, now);
            return Decision.ON_COOLDOWN;
        }

        lastAttempt.put(key, now);

        // Recorded here rather than on delivery because this is the only point the plugin
        // is told about. The trade is deliberate: a player who abandons checkout burns a
        // cooldown, which is an annoyance, where the other direction is an unlimited
        // claim, which is a dupe.
        tracker.recordClaim(pkg, playerName);

        return Decision.ALLOWED;
    }

    /** The package as published in the listing, or {@code null} if the store has no such ID. */
    public CategoryPackage findPackage(int packageId) {
        List<Category> categories = platform.getStoreCategories();
        return categories == null ? null : findPackage(categories, packageId);
    }

    private CategoryPackage findPackage(List<Category> categories, int packageId) {
        for (Category category : categories) {
            CategoryPackage found = findIn(category.getPackages(), packageId);
            if (found != null) return found;

            if (category.getSubCategories() != null) {
                for (SubCategory subCategory : category.getSubCategories()) {
                    found = findIn(subCategory.getPackages(), packageId);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private CategoryPackage findIn(List<CategoryPackage> packages, int packageId) {
        if (packages == null) return null;
        for (CategoryPackage pkg : packages) {
            if (pkg.getId() == packageId) return pkg;
        }
        return null;
    }

    /** Message to show the player for a refusal, or empty to stay silent. */
    public String messageFor(Decision decision) {
        switch (decision) {
            case ON_COOLDOWN:
                return cfg("messages.package-on-cooldown",
                        "<red>You have already claimed this. Check back later.");
            case UNKNOWN_PACKAGE:
                return cfg("messages.package-unknown",
                        "<red>That package is not available.");
            case LISTING_UNAVAILABLE:
                return cfg("messages.listing-unavailable",
                        "<red>The store is still loading. Please try again in a moment.");
            case TOO_FAST:
                return cfg("messages.purchase-too-fast",
                        "<red>Slow down a moment, then try again.");
            default:
                return "";
        }
    }

    /** Forgets a player's rate-limit entry, so a reconnect is not held back by it. */
    public void clear(String playerName) {
        if (playerName == null) return;
        lastAttempt.remove(playerName.toLowerCase(Locale.ROOT));
    }

    private boolean enforceCooldown() {
        return platform.getPlugin().getConfig().getBoolean("free-packages.enforce-cooldown", true);
    }

    private long purchaseIntervalMillis() {
        return Math.max(0, platform.getPlugin().getConfig()
                .getInt("free-packages.purchase-interval-ms", 500));
    }

    private String cfg(String path, String fallback) {
        String value = platform.getPlugin().getConfig().getString(path, fallback);
        return value == null ? "" : value;
    }
}
