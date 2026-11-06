package tn.techstore.coupons;

import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;
import tn.techstore.common.ApiException;
import tn.techstore.common.Ids;
import tn.techstore.coupons.CouponDtos.CouponDto;
import tn.techstore.coupons.CouponDtos.CouponForm;
import tn.techstore.coupons.CouponDtos.CouponQuote;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.inc;

@ApplicationScoped
public class CouponService {

    /** Vérifie un code depuis le panier, sans le consommer. */
    public CouponQuote quote(String code, BigDecimal subtotal) {
        Coupon c = Coupon.byCode(code).orElseThrow(() -> ApiException.badRequest("Code promo inconnu"));
        String reason = c.unusableReason(subtotal, Instant.now());
        if (reason != null) {
            throw ApiException.badRequest(reason);
        }
        return new CouponQuote(c.code, c.discountFor(subtotal), describe(c));
    }

    /**
     * Consomme une utilisation du code de façon atomique : toutes les conditions (actif, dates, minimum d'achat,
     * nombre d'utilisations) sont vérifiées par MongoDB dans la même opération que l'incrément.
     *
     * @return la remise accordée
     */
    public BigDecimal redeem(String code, BigDecimal subtotal) {
        Date now = new Date();
        Bson usable = and(
                eq("code", Coupon.normalize(code)),
                eq("active", true),
                lte("minOrder", new Decimal128(subtotal)),
                or(eq("maxUses", null), expr(new Document("$lt", List.of("$usedCount", "$maxUses")))),
                or(eq("startsAt", null), lte("startsAt", now)),
                or(eq("endsAt", null), gte("endsAt", now)));
        Coupon c = Coupon.col().findOneAndUpdate(usable, inc("usedCount", 1),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
        if (c == null) {
            // Refusé : on relit le code pour expliquer pourquoi
            Coupon current = Coupon.byCode(code).orElseThrow(() -> ApiException.badRequest("Code promo inconnu"));
            String reason = current.unusableReason(subtotal, now.toInstant());
            throw ApiException.badRequest(reason != null ? reason : "Ce code promo n'est plus utilisable");
        }
        return c.discountFor(subtotal);
    }

    /** Rend une utilisation (commande annulée ou échouée). */
    public void release(String code) {
        if (code == null) return;
        Coupon.col().updateOne(and(eq("code", Coupon.normalize(code)), gt("usedCount", 0)), inc("usedCount", -1));
    }

    // ---------- Administration ----------

    public List<CouponDto> list() {
        return Coupon.col().find().sort(Sorts.descending("createdAt")).map(CouponDto::of).into(new ArrayList<>());
    }

    public CouponDto create(CouponForm form) {
        String code = Coupon.normalize(form.code());
        if (Coupon.byCode(code).isPresent()) {
            throw ApiException.conflict("Le code " + code + " existe déjà");
        }
        Coupon c = new Coupon();
        c.code = code;
        c.usedCount = 0;
        c.createdAt = Instant.now();
        apply(c, form);
        c.persist();
        return CouponDto.of(c);
    }

    public CouponDto update(String id, CouponForm form) {
        Coupon c = find(id);
        String code = Coupon.normalize(form.code());
        if (!code.equals(c.code) && Coupon.byCode(code).isPresent()) {
            throw ApiException.conflict("Le code " + code + " existe déjà");
        }
        c.code = code;
        apply(c, form);
        c.update();
        return CouponDto.of(c);
    }

    public void delete(String id) {
        find(id).delete();
    }

    private void apply(Coupon c, CouponForm form) {
        if (form.type() == Coupon.Type.PERCENT && form.value().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw ApiException.badRequest("Un pourcentage ne peut pas dépasser 100");
        }
        if (form.startsAt() != null && form.endsAt() != null && form.endsAt().isBefore(form.startsAt())) {
            throw ApiException.badRequest("La date de fin doit être après la date de début");
        }
        c.type = form.type();
        c.value = form.value();
        c.minOrder = form.minOrder() == null ? BigDecimal.ZERO : form.minOrder();
        c.maxUses = form.maxUses();
        c.startsAt = form.startsAt();
        c.endsAt = form.endsAt();
        c.active = form.active();
    }

    private Coupon find(String id) {
        Coupon c = Coupon.findById(Ids.parse(id, "Code promo"));
        if (c == null) throw ApiException.notFound("Code promo");
        return c;
    }

    private static String describe(Coupon c) {
        String v = c.value.stripTrailingZeros().toPlainString();
        return c.type == Coupon.Type.PERCENT ? "-" + v + " % sur votre commande" : "-" + v + " DT sur votre commande";
    }
}
