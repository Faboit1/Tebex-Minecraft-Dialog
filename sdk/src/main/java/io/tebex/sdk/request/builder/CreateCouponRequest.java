package io.tebex.sdk.request.builder;

import io.tebex.sdk.obj.EnumBasketType;
import io.tebex.sdk.obj.EnumDiscountType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter @RequiredArgsConstructor
public class CreateCouponRequest {
    private final String code;
    private final EffectiveOn effectiveOn;
    private final List<Integer> effectiveIds;
    private final EnumDiscountType discountType;
    private final int discountValue;
    private final LocalDate startDate;

    private int minimum;
    private DiscountMethod discountMethod = DiscountMethod.EACH_PACKAGE;
    private boolean redeemUnlimited = false;
    private boolean canExpire;
    private LocalDate expiryDate;
    private int expiryLimit;
    private final EnumBasketType basketType;
    private String username;
    private String note;

    public enum EffectiveOn {
        PACKAGE,
        CATEGORY,
        CART
    }

    @Getter @AllArgsConstructor
    public enum DiscountMethod {
        EACH_PACKAGE(0),
        BASKET_BEFORE_SALES(1),
        BASKET_AFTER_SALES(2);

        private final int value;
    }
}
