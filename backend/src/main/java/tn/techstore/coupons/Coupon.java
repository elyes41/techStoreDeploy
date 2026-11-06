package tn.techstore.coupons;

import com.mongodb.client.MongoCollection;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

/** Collection « coupons ». Le code est toujours enregistré en majuscules (index unique). */
@MongoEntity(collection = "coupons")
public class Coupon extends PanacheMongoEntity {

    public enum Type { PERCENT, FIXED }

    public String code;
    public Type type;
    /** Pourcentage (ex. 10) ou montant fixe en DT. */
    public BigDecimal value;
    public BigDecimal minOrder = BigDecimal.ZERO;
    /** null = illimité. */
    public Integer maxUses;
    public int usedCount;
    public Instant startsAt;
    public Instant endsAt;
    public boolean active = true;
    public Instant createdAt;

    public static MongoCollection<Coupon> col() {
        return mongoCollection();
    }

    public static Optional<Coupon> byCode(String code) {
        return Optional.ofNullable(col().find(eq("code", normalize(code))).first());
    }

    public static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    /** Remise pour ce sous-total, jamais supérieure au sous-total. */
    public BigDecimal discountFor(BigDecimal subtotal) {
        BigDecimal d = type == Type.PERCENT
                ? subtotal.multiply(value).divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP)
                : value;
        return d.min(subtotal);
    }

    /** Message d'erreur si le code n'est pas utilisable, sinon null. */
    public String unusableReason(BigDecimal subtotal, Instant now) {
        if (!active) return "Ce code promo n'est plus actif";
        if (startsAt != null && now.isBefore(startsAt)) return "Ce code promo n'est pas encore valable";
        if (endsAt != null && now.isAfter(endsAt)) return "Ce code promo a expiré";
        if (maxUses != null && usedCount >= maxUses) return "Ce code promo a atteint son nombre d'utilisations";
        if (subtotal.compareTo(minOrder) < 0) {
            return "Ce code promo s'applique dès " + minOrder.stripTrailingZeros().toPlainString() + " DT d'achat";
        }
        return null;
    }
}
