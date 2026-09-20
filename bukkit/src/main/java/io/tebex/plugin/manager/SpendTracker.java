package io.tebex.plugin.manager;

import io.tebex.plugin.BukkitPluginPlatform;
import io.tebex.sdk.obj.PlayerLookupInfo;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
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

    public SpendTracker(BukkitPluginPlatform platform) {
        this.platform = platform;
    }

    /**
     * The cached totals for a player, refreshing in the background when stale.
     * Never blocks, so it is safe to call from a placeholder request.
     */
    public Spend get(String playerName) {
        if (playerName == null || playerName.isEmpty()) return Spend.ZERO;

        String key = playerName.toLowerCase(Locale.ROOT);
        Entry entry = cache.get(key);

        if (entry == null) {
            refresh(playerName);
            return Spend.ZERO;
        }

        if (System.currentTimeMillis() - entry.fetchedAt >= cacheTtlMillis()) {
            refresh(playerName, entry);
        }

        return entry.spend;
    }

    public void refresh(String playerName) {
        refresh(playerName, null);
    }

    private void refresh(String playerName, Entry existing) {
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

        try {
            platform.getSDK().getPlayerLookupInfo(playerName)
                    .thenAccept(info -> {
                        cache.put(key, new Entry(summarise(info), System.currentTimeMillis()));
                        if (existing != null) existing.refreshing.set(false);
                    })
                    .exceptionally(throwable -> {
                        platform.debug("Failed to look up spend for " + playerName + ": " + throwable.getMessage());
                        // Keep the stale figure but let it be retried after the TTL.
                        cache.put(key, new Entry(existing != null ? existing.spend : Spend.ZERO,
                                System.currentTimeMillis()));
                        if (existing != null) existing.refreshing.set(false);
                        return null;
                    });
        } catch (Throwable e) {
            if (existing != null) existing.refreshing.set(false);
            platform.debug("Failed to start spend lookup for " + playerName + ": " + e);
        }
    }

    /** Drops a player's cached figures, so the next read fetches them again. */
    public void invalidate(String playerName) {
        if (playerName == null) return;
        cache.remove(playerName.toLowerCase(Locale.ROOT));
    }

    public void clear() {
        cache.clear();
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
