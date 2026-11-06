package tn.techstore.coupons;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

import static tn.techstore.common.Ids.str;

public final class CouponDtos {

    private CouponDtos() {
    }

    public record CouponCheck(@NotBlank @Size(max = 40) String code, @NotNull @DecimalMin("0") BigDecimal subtotal) {
    }

    public record CouponQuote(String code, BigDecimal discount, String description) {
    }

    public record CouponForm(
            @NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "lettres, chiffres, - et _ uniquement")
            String code,
            @NotNull Coupon.Type type,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal value,
            @DecimalMin("0") BigDecimal minOrder,
            @Min(1) Integer maxUses,
            Instant startsAt,
            Instant endsAt,
            boolean active) {
    }

    public record CouponDto(String id, String code, Coupon.Type type, BigDecimal value, BigDecimal minOrder,
                            Integer maxUses, int usedCount, Instant startsAt, Instant endsAt, boolean active,
                            Instant createdAt) {

        public static CouponDto of(Coupon c) {
            return new CouponDto(str(c.id), c.code, c.type, c.value, c.minOrder, c.maxUses, c.usedCount, c.startsAt,
                    c.endsAt, c.active, c.createdAt);
        }
    }
}
