package io.tebex.plugin.manager;

import io.tebex.plugin.BukkitPluginPlatform;
import io.tebex.sdk.obj.PlayerLookupInfo;
import io.tebex.sdk.util.UUIDUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks how much each player has spent on the webstore, for the
 * {@code %tebex_money_spent%} placeholders.
 *
 * <p>Figures come from the Tebex {@code /user/<name>} lookup, which returns the real
 * payment history, so they survive restarts and include purchases made before this
 * plugin was installed. PlaceholderAPI resolves placeholders on the main thread and
 * cannot wait for HTTP, so lookups run in the background and every read is served from
 * this cache; a cold entry returns zero and schedules a fetch.</p>
 */
public class SpendTracker {
    /** One player's totals, as of the moment they were fetched. */
    public static class Spend {
        public static final Spend ZERO = new Spend(0, 0, 0, 0);

        private final double total;
        private final double month;
        private final double week;
        private final double today;

        Spend(double total, double month, double week, double today) {
            this.total = total;
            this.month = month;
            this.week = week;
            this.today = today;
        }

        public double getTotal() {
            return total;
        }

        public double getThisMonth() {
            return month;
        }

        public double getThisWeek() {
            return week;
        }

        public double getToday() {
            return today;
        }
    }

    private static class Entry {
        final Spend spend;
        final long fetchedAt;
        final AtomicBoolean refreshing = new AtomicBoolean();

        Entry(Spend spend, long fetchedAt) {
            this.spend = spend;
            this.fetchedAt = fetchedAt;
        }
    }

    private final BukkitPluginPlatform platform;
    private final Map<String, Entry> cache = new ConcurrentHashMap<>();
    /** Players already warned about, so a missing history is reported once, not every refresh. */
    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    public SpendTracker(BukkitPluginPlatform platform) {
        this.platform = platform;
    }

    /**
     * The cached totals for a player, refreshing in the background when stale.
     * Never blocks, so it is safe to call from a placeholder request.
     */
    public Spend get(String playerName) {
        return get(playerName, null);
    }

    /**
     * @param uuid the player's UUID when known. Worth passing: it is the identifier an
     *             online-mode store is keyed by, and it is available for offline players
     *             where a name-to-UUID lookup is not.
     */
    public Spend get(String playerName, UUID uuid) {
        if (playerName == null || playerName.isEmpty()) return Spend.ZERO;

        String key = playerName.toLowerCase(Locale.ROOT);
        Entry entry = cache.get(key);

        if (entry == null) {
            refresh(playerName, uuid, null);
            return Spend.ZERO;
        }

        if (System.currentTimeMillis() - entry.fetchedAt >= cacheTtlMillis()) {
            refresh(playerName, uuid, entry);
        }

        return entry.spend;
    }

    public void refresh(String playerName) {
        refresh(playerName, null, null);
    }

    public void refresh(String playerName, UUID uuid) {
        refresh(playerName, uuid, null);
    }

    private void refresh(String playerName, UUID uuid, Entry existing) {
        if (playerName == null || playerName.isEmpty()) return;
        if (!platform.isSetup()) return;
        if (!platform.getPlugin().getConfig().getBoolean("money-spent.enabled", true)) return;

        // One in-flight lookup per player: placeholders can be resolved many times a
        // tick (every scoreboard line, every tab entry), and each miss would otherwise
        // start its own request.
        if (existing != null && !existing.refreshing.compareAndSet(false, true)) return;

        String key = playerName.toLowerCase(Locale.ROOT);
        if (existing == null && cache.putIfAbsent(key, new Entry(Spend.ZERO, System.currentTimeMillis())) != null) {
            return;
        }

        List<String> identifiers = identifiersFor(playerName, uuid);
        logLookup("Looking up spend for " + playerName + " as " + identifiers);

        try {
            tryNext(playerName, key, identifiers, 0, existing);
        } catch (Throwable e) {
            finish(key, existing, existing != null ? existing.spend : Spend.ZERO);
            platform.debug("Failed to start spend lookup for " + playerName + ": " + e);
        }
    }

    /**
     * Walks the candidate identifiers until one returns a payment history.
     *
     * <p>The lookup endpoint is documented as {@code /user/<uuid>} and an online-mode
     * store will not resolve a username, while a Geyser or offline store will not
     * resolve a UUID. Rather than pick one and be silently wrong for half of all
     * servers, the likely form for this store is tried first and the others after.</p>
     */
    private void tryNext(String playerName, String key, List<String> identifiers, int index, Entry existing) {
        if (index >= identifiers.size()) {
            warnOnce(playerName, identifiers);
            finish(key, existing, existing != null ? existing.spend : Spend.ZERO);
            return;
        }

        String identifier = identifiers.get(index);
        CompletableFuture<PlayerLookupInfo> future;
        try {
            future = platform.getSDK().getPlayerLookupInfo(identifier);
        } catch (Throwable e) {
            platform.debug("Spend lookup for " + identifier + " failed to start: " + e);
            tryNext(playerName, key, identifiers, index + 1, existing);
            return;
        }

        if (future == null) {
            tryNext(playerName, key, identifiers, index + 1, existing);
            return;
        }

        future.thenAccept(info -> {
            // A miss comes back as null rather than an error, so an unresolved
            // identifier is indistinguishable from a real zero without this check.
            if (info == null || info.getPayments() == null || info.getPayments().isEmpty()) {
                boolean hasTotals = info != null && info.getPurchaseTotals() != null
                        && !info.getPurchaseTotals().isEmpty();
                if (!hasTotals) {
                    logLookup("  " + identifier + " -> no payment history");
                    tryNext(playerName, key, identifiers, index + 1, existing);
                    return;
                }
            }

            Spend spend = summarise(info);
            logLookup("  " + identifier + " -> total=" + spend.getTotal()
                    + " month=" + spend.getThisMonth()
                    + " week=" + spend.getThisWeek()
                    + " today=" + spend.getToday()
                    + " (" + info.getPayments().size() + " payments)");
            warned.remove(key);
            finish(key, existing, spend);
        }).exceptionally(throwable -> {
            platform.debug("Spend lookup for " + identifier + " failed: " + throwable.getMessage());
            tryNext(playerName, key, identifiers, index + 1, existing);
            return null;
        });
    }

    private void finish(String key, Entry existing, Spend spend) {
        cache.put(key, new Entry(spend, System.currentTimeMillis()));
        if (existing != null) existing.refreshing.set(false);
    }

    /**
     * Identifiers to try, most likely first.
     *
     * <p>Which one a store knows a player by is decided per player rather than per
     * server, because the two can disagree. An offline or cracked server derives a
     * player's UUID locally from their name, and such a UUID is a version 3 (name-based)
     * one that Tebex may never have seen; a real Mojang account has a version 4 (random)
     * UUID. A server can carry both at once — cracked players alongside proxy-
     * authenticated or Floodgate ones — so reading the version of the UUID in hand beats
     * any server-wide online-mode flag, which is a single answer for every player.</p>
     *
     * <p>Nothing is ever dropped, only ordered: an offline store can still have recorded
     * the locally-derived UUID, so it is tried after the username rather than skipped.
     * {@code money-spent.lookup-by} forces the order when the guess is wrong.</p>
     */
    private List<String> identifiersFor(String playerName, UUID uuid) {
        List<String> identifiers = new ArrayList<>();

        if (uuid == null) {
            try {
                // Only resolves online players, so it is a fallback for callers that
                // could not supply one rather than the primary source.
                uuid = platform.getPlayerUniqueId(playerName);
            } catch (Throwable ignored) {
                // An offline player with no cached profile simply has no UUID to try.
            }
        }

        boolean hasUuid = uuid != null && !UUIDUtil.EMPTY_UUID.equals(uuid);
        if (!hasUuid) {
            identifiers.add(playerName);
            return identifiers;
        }

        // Tebex returns and expects Mojang-style UUIDs, which carry no dashes.
        String undashed = uuid.toString().replace("-", "");

        if (uuidFirst(uuid)) {
            identifiers.add(undashed);
            identifiers.add(uuid.toString());
            identifiers.add(playerName);
        } else {
            identifiers.add(playerName);
            identifiers.add(undashed);
            identifiers.add(uuid.toString());
        }

        return identifiers;
    }

    private boolean uuidFirst(UUID uuid) {
        String mode = platform.getPlugin().getConfig().getString("money-spent.lookup-by", "auto");
        if (mode != null) {
            String normalised = mode.trim().toLowerCase(Locale.ROOT);
            if (normalised.equals("uuid")) return true;
            if (normalised.equals("username") || normalised.equals("name")) return false;
        }

        // Version 3 is a name-based UUID, which is how an offline server manufactures
        // one, so the store is far more likely to know this player by name.
        if (uuid.version() == 3) return false;

        return platform.isOnlineMode() && !platform.isGeyser();
    }

    private void warnOnce(String playerName, List<String> identifiers) {
        String key = playerName.toLowerCase(Locale.ROOT);
        if (!warned.add(key)) return;

        platform.warning("No Tebex payment history found for " + playerName
                        + " (tried " + identifiers + "), so the money-spent placeholders will show 0.",
                "If this player has bought something, run /tebex lookup " + playerName
                        + " to check the store agrees, and set money-spent.log-lookups: true for detail.");
    }

    private void logLookup(String message) {
        if (platform.getPlugin().getConfig().getBoolean("money-spent.log-lookups", false)) {
            platform.info(message);
        } else {
            platform.debug(message);
        }
    }

    /** Drops a player's cached figures, so the next read fetches them again. */
    public void invalidate(String playerName) {
        if (playerName == null) return;
        String key = playerName.toLowerCase(Locale.ROOT);
        cache.remove(key);
        warned.remove(key);
    }

    public void clear() {
        cache.clear();
        warned.clear();
    }

    Spend summarise(PlayerLookupInfo info) {
        if (info == null || info.getPayments() == null) return Spend.ZERO;

        long now = System.currentTimeMillis();
        long startOfDay = startOf(Calendar.DAY_OF_MONTH, now);
        long startOfWeek = startOfWeek(now);
        long startOfMonth = startOf(Calendar.MONTH, now);

        double total = 0, month = 0, week = 0, today = 0;

        for (PlayerLookupInfo.Payment payment : info.getPayments()) {
            if (payment == null || !payment.countsTowardsSpend()) continue;

            double price = payment.getPrice();
            total += price;

            // The API reports payment times in seconds since the epoch.
            long millis = payment.getTime() * 1000L;
            if (millis >= startOfMonth) month += price;
            if (millis >= startOfWeek) week += price;
            if (millis >= startOfDay) today += price;
        }

        // Some stores return a truncated payment list alongside a complete total.
        if (total == 0 && info.getPurchaseTotals() != null && !info.getPurchaseTotals().isEmpty()) {
            for (Double value : info.getPurchaseTotals().values()) {
                if (value != null) total += value;
            }
        }

        return new Spend(total, month, week, today);
    }

    private long cacheTtlMillis() {
        int seconds = platform.getPlugin().getConfig().getInt("money-spent.cache-seconds", 300);
        return Math.max(30, seconds) * 1000L;
    }

    private Calendar calendar(long now) {
        String zone = platform.getPlugin().getConfig().getString("money-spent.timezone", "");
        Calendar calendar = zone == null || zone.isEmpty()
                ? Calendar.getInstance()
                : Calendar.getInstance(TimeZone.getTimeZone(zone));

        calendar.setTimeInMillis(now);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private long startOf(int field, long now) {
        Calendar calendar = calendar(now);
        if (field == Calendar.MONTH) calendar.set(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTimeInMillis();
    }

    /**
     * Midnight on the most recent first-day-of-week. Which day that is comes from the
     * configured locale by default, so a server set to a region where the week starts on
     * Sunday gets a Sunday boundary; {@code money-spent.week-starts-monday} forces it.
     */
    private long startOfWeek(long now) {
        Calendar calendar = calendar(now);

        if (platform.getPlugin().getConfig().getBoolean("money-spent.week-starts-monday", true)) {
            calendar.setFirstDayOfWeek(Calendar.MONDAY);
        }

        int firstDay = calendar.getFirstDayOfWeek();
        int daysSinceFirst = (calendar.get(Calendar.DAY_OF_WEEK) - firstDay + 7) % 7;
        calendar.add(Calendar.DAY_OF_MONTH, -daysSinceFirst);
        return calendar.getTimeInMillis();
    }
}
