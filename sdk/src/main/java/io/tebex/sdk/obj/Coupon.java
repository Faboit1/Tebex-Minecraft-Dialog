package io.tebex.sdk.obj;

import com.google.gson.JsonObject;
import io.tebex.sdk.util.GsonUtil;
import io.tebex.sdk.util.StringUtil;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class Coupon {
    private final int id;
    private final String code;
    private final Effective effective;
    private final Discount discount;
    private final Expiry expiry;
    private final EnumBasketType basketType;
    private final ZonedDateTime startDate;
    private final int userLimit;
    private final int minimum;
    private final String username;
    private final String note;

    @Data
    public static class Effective {
        private final EffectiveType type;
        private final List<Integer> packages;
        private final List<Integer> categories;

        public enum EffectiveType {
            PACKAGE,
            CATEGORY
        }
    }

    @Data
    public static class Discount {
        private final EnumDiscountType type;
        private final double percentage;
        private final int value;
    }

    @Data
    public static class Expiry {
        private final boolean redeemUnlimited;
        private final boolean neverExpires;
        private final int limit;
        private final ZonedDateTime date;
    }

    public static Coupon fromJsonObject(JsonObject jsonObject) {
        JsonObject effectiveJson = jsonObject.get("effective").getAsJsonObject();
        JsonObject discountJson = jsonObject.get("discount").getAsJsonObject();
        JsonObject expireJson = jsonObject.get("expire").getAsJsonObject();

        return new Coupon(
                jsonObject.get("id").getAsInt(),
                jsonObject.get("code").getAsString(),
                new Effective(
                        Effective.EffectiveType.valueOf(effectiveJson.get("type").getAsString().toUpperCase()),
                        GsonUtil.arrayToList(effectiveJson.get("packages").getAsJsonArray()),
                        GsonUtil.arrayToList(effectiveJson.get("categories").getAsJsonArray())
                ),
                new Discount(
                        EnumDiscountType.valueOf(discountJson.get("type").getAsString().toUpperCase()),
                        discountJson.get("percentage").getAsInt(),
                        discountJson.get("value").getAsInt()
                ),
                new Expiry(
                        expireJson.get("redeem_unlimited").getAsString().equals("true"),
                        expireJson.get("expire_never").getAsString().equals("true"),
                        expireJson.get("limit").getAsInt(),
                        StringUtil.toModernDate(expireJson.get("date").getAsString())
                ),
                EnumBasketType.valueOf(jsonObject.get("basket_type").getAsString().toUpperCase()),
                StringUtil.toModernDate(jsonObject.get("start_date").getAsString()),
                jsonObject.get("user_limit").getAsInt(),
                jsonObject.get("minimum").getAsInt(),
                jsonObject.get("username").getAsString(),
                jsonObject.get("note").getAsString()
        );
    }
}
