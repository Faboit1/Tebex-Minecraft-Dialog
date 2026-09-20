package io.tebex.plugin.util;

import org.bukkit.configuration.file.FileConfiguration;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Renders monetary amounts for the {@code %tebex_money_spent%} placeholders.
 *
 * <p>Separators are configured rather than taken from the server's locale, because the
 * locale of a Minecraft server rarely matches the audience: a store pricing in euros
 * wants {@code 4,5} whatever the JVM happens to be set to.</p>
 */
public final class MoneyUtil {
    private MoneyUtil() {
    }

    public static String currency(FileConfiguration config) {
        String currency = config.getString("money-spent.currency", "€");
        return currency == null ? "" : currency;
    }

    /**
     * Formats an amount using the {@code money-spent} settings, applying the configured
     * template so the currency symbol can sit on either side, or be left out entirely.
     */
    public static String format(FileConfiguration config, double amount) {
        String template = config.getString("money-spent.format", "%amount%");
        if (template == null) template = "%amount%";

        return template
                .replace("%amount%", formatAmount(config, amount))
                .replace("%currency%", currency(config));
    }

    /** The number on its own, with no currency symbol. */
    public static String formatAmount(FileConfiguration config, double amount) {
        int decimals = Math.max(0, Math.min(8, config.getInt("money-spent.decimals", 2)));
        boolean trim = config.getBoolean("money-spent.trim-trailing-zeros", true);
        boolean grouping = config.getBoolean("money-spent.group-thousands", false);

        StringBuilder pattern = new StringBuilder(grouping ? "#,##0" : "0");
        if (decimals > 0) {
            pattern.append('.');
            for (int i = 0; i < decimals; i++) {
                pattern.append(trim ? '#' : '0');
            }
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(charOf(config, "money-spent.decimal-separator", ','));
        symbols.setGroupingSeparator(charOf(config, "money-spent.thousands-separator", '.'));

        DecimalFormat format = new DecimalFormat(pattern.toString(), symbols);
        format.setGroupingUsed(grouping);
        // Half-up matches how a shop total reads to a human; the JVM default rounds
        // half to even, which would show 0,125 as 0,12.
        format.setRoundingMode(RoundingMode.HALF_UP);

        return format.format(amount);
    }

    private static char charOf(FileConfiguration config, String path, char fallback) {
        String value = config.getString(path, String.valueOf(fallback));
        return value == null || value.isEmpty() ? fallback : value.charAt(0);
    }
}
