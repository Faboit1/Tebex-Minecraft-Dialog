package io.tebex.sdk.obj;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class PlayerLookupInfo {
    public final Player player;
    public final int banCount;
    public final int chargebackRate;
    public final List<Payment> payments;
    public final Map<String, Double> purchaseTotals;

    @Data
    public static class Player {
        public final String id;
        public final String username;
        public final String meta;
        public final int pluginUsernameId;
    }


    @Data
    public static class Payment {
        public final String txnId;
        public final long time;
        public final double price;
        public final String currency;

        /**
         * The payment status exactly as the API returned it. Kept as text because the
         * documented values are words ("complete", "refund", "chargeback") while some
         * responses carry a number, and parsing one shape as the other throws.
         */
        public final String status;

        /**
         * Whether this payment should count towards what a player has spent.
         *
         * <p>Reversed payments are named explicitly rather than completed ones being
         * matched, so an unrecognised status still counts: under-reporting a total is
         * the worse failure, and refunds and chargebacks are the only things that must
         * be excluded.</p>
         */
        public boolean countsTowardsSpend() {
            if (status == null) return true;

            String normalised = status.trim().toLowerCase(java.util.Locale.ROOT);
            return !normalised.contains("refund")
                    && !normalised.contains("chargeback")
                    && !normalised.contains("dispute")
                    && !normalised.contains("denied");
        }
    }

    public static PlayerLookupInfo fromJsonObject(JsonObject jsonObject) {
        JsonObject playerJson = jsonObject.get("player").getAsJsonObject();
        Player player = new Player(str(playerJson, "id"), str(playerJson, "username"),
                str(playerJson, "meta"), intOf(playerJson, "plugin_username_id"));
        int banCount = intOf(jsonObject, "banCount");
        int chargebackRate = intOf(jsonObject, "chargebackRate");

        List<Payment> payments = new ArrayList<>();
        JsonElement paymentsJson = jsonObject.get("payments");
        if (paymentsJson != null && paymentsJson.isJsonArray()) {
            JsonArray paymentsJsonArray = paymentsJson.getAsJsonArray();
            for (JsonElement paymentElement : paymentsJsonArray) {
                if (!paymentElement.isJsonObject()) continue;
                JsonObject paymentJson = paymentElement.getAsJsonObject();

                payments.add(new Payment(
                        str(paymentJson, "txn_id"),
                        longOf(paymentJson, "time"),
                        doubleOf(paymentJson, "price"),
                        str(paymentJson, "currency"),
                        str(paymentJson, "status")
                ));
            }
        }

        Map<String, Double> purchaseTotals = new HashMap<>();
        JsonElement purchaseTotalsJson = jsonObject.get("purchaseTotals");
        if (purchaseTotalsJson != null && purchaseTotalsJson.isJsonObject()) { // empty arrives as []
            JsonObject purchaseTotalsObj = purchaseTotalsJson.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : purchaseTotalsObj.entrySet()) {
                try {
                    purchaseTotals.put(entry.getKey(), entry.getValue().getAsDouble());
                } catch (RuntimeException ignored) {
                    // A total we cannot read is skipped rather than failing the lookup.
                }
            }
        }

        return new PlayerLookupInfo(player, banCount, chargebackRate, payments, purchaseTotals);
    }

    private static String str(JsonObject source, String key) {
        JsonElement element = source.get(key);
        if (element == null || element.isJsonNull()) return "";
        try {
            return element.getAsString();
        } catch (RuntimeException e) {
            return element.toString();
        }
    }

    private static int intOf(JsonObject source, String key) {
        return (int) longOf(source, key);
    }

    private static long longOf(JsonObject source, String key) {
        JsonElement element = source.get(key);
        if (element == null || element.isJsonNull()) return 0L;
        try {
            return (long) element.getAsDouble();
        } catch (RuntimeException e) {
            return 0L;
        }
    }

    private static double doubleOf(JsonObject source, String key) {
        JsonElement element = source.get(key);
        if (element == null || element.isJsonNull()) return 0D;
        try {
            return element.getAsDouble();
        } catch (RuntimeException e) {
            return 0D;
        }
    }
}
