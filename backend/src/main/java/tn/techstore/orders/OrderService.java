package tn.techstore.orders;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import tn.techstore.catalog.Product;
import tn.techstore.common.ApiException;
import tn.techstore.common.Ids;
import tn.techstore.common.PageResult;
import tn.techstore.common.ShopConfig;
import tn.techstore.coupons.CouponService;
import tn.techstore.mail.ShippingNotifier;
import tn.techstore.mongo.Mongo;
import tn.techstore.orders.OrderDtos.*;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

@ApplicationScoped
public class OrderService {

    public static final ZoneId SHOP_ZONE = ZoneId.of("Africa/Tunis");
    private static final String REF_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject
    ShopConfig config;

    @Inject
    Inventory inventory;

    @Inject
    CouponService coupons;

    @Inject
    ShippingNotifier notifier;

    /**
     * Passe une commande. Le stock et le code promo sont réservés par des mises à jour atomiques ;
     * si une étape échoue, tout ce qui a déjà été réservé est restitué avant de renvoyer l'erreur.
     */
    public OrderDetail checkout(CheckoutRequest req) {
        Map<ObjectId, Integer> quantities = new LinkedHashMap<>();
        for (CheckoutItem item : req.items()) {
            if (!ObjectId.isValid(item.productId())) {
                throw ApiException.badRequest("Un produit de votre panier n'est plus disponible");
            }
            quantities.merge(new ObjectId(item.productId()), item.quantity(), Integer::sum);
        }

        Order order = new Order();
        Map<ObjectId, Integer> reserved = new LinkedHashMap<>();
        String redeemedCoupon = null;
        try {
            BigDecimal subtotal = BigDecimal.ZERO;
            for (var entry : quantities.entrySet()) {
                int qty = entry.getValue();
                Product p = inventory.reserve(entry.getKey(), qty);
                if (p == null) {
                    Product current = Product.findById(entry.getKey());
                    if (current == null || !current.active) {
                        throw ApiException.badRequest("Un produit de votre panier n'est plus disponible");
                    }
                    throw ApiException.conflict("Stock insuffisant pour « " + current.name + " » (reste " + current.stock + ")");
                }
                reserved.put(p.id, qty);

                Order.Item line = new Order.Item();
                line.productId = p.id;
                line.name = p.name;
                line.unitPrice = p.price;
                line.quantity = qty;
                line.lineTotal = p.price.multiply(BigDecimal.valueOf(qty));
                order.items.add(line);
                subtotal = subtotal.add(line.lineTotal);
            }

            BigDecimal discount = BigDecimal.ZERO;
            if (req.couponCode() != null && !req.couponCode().isBlank()) {
                discount = coupons.redeem(req.couponCode(), subtotal);
                redeemedCoupon = req.couponCode();
                order.couponCode = req.couponCode().trim().toUpperCase();
            }
            BigDecimal afterDiscount = subtotal.subtract(discount);
            BigDecimal shipping = afterDiscount.compareTo(config.freeShippingThreshold()) >= 0
                    ? BigDecimal.ZERO : config.shippingFee();

            Instant now = Instant.now();
            order.status = OrderStatus.PENDING;
            order.customer.name = req.customerName().trim();
            order.customer.phone = req.phone().trim();
            order.customer.email = blankToNull(req.email());
            order.delivery.governorate = req.governorate().trim();
            order.delivery.city = blankToNull(req.city());
            order.delivery.address = req.address().trim();
            order.delivery.note = blankToNull(req.note());
            order.payment.method = req.paymentMethod();
            order.payment.status = PaymentStatus.UNPAID;
            order.amounts.subtotal = subtotal;
            order.amounts.discount = discount;
            order.amounts.shippingFee = shipping;
            order.amounts.fiscalStamp = config.fiscalStamp();
            order.amounts.total = afterDiscount.add(shipping).add(config.fiscalStamp());
            order.history.add(new Order.StatusChange(OrderStatus.PENDING, now));
            order.createdAt = order.updatedAt = now;
            insertWithUniqueReference(order);

            // Point d'intégration : pour CARD / E_DINAR / MOBILE_WALLET, créer ici la session de paiement
            // auprès de la passerelle choisie et renvoyer son URL de redirection au client.
            return OrderDetail.of(order);
        } catch (RuntimeException e) {
            reserved.forEach(inventory::release);
            if (redeemedCoupon != null) coupons.release(redeemedCoupon);
            throw e;
        }
    }

    private void insertWithUniqueReference(Order order) {
        for (int attempt = 0; ; attempt++) {
            order.id = null;
            order.reference = newReference();
            try {
                order.persist();
                return;
            } catch (MongoWriteException e) {
                if (e.getError().getCategory() != ErrorCategory.DUPLICATE_KEY || attempt >= 4) throw e;
            }
        }
    }

    /** Suivi par le client : référence + numéro de téléphone (les 8 derniers chiffres suffisent). */
    public OrderDetail track(String reference, String phone) {
        String digits = digits(phone);
        Order o = Order.col().find(eq("reference", reference.trim().toUpperCase())).first();
        if (o == null || digits.length() < 8 || !digits(o.customer.phone).endsWith(digits)) {
            throw ApiException.notFound("Commande");
        }
        return OrderDetail.of(o);
    }

    // ---------- Administration ----------

    public PageResult<OrderSummary> list(OrderStatus status, String q, int page, int size) {
        List<Bson> filters = new ArrayList<>();
        if (status != null) filters.add(eq("status", status.name()));
        if (q != null && !q.isBlank()) {
            var rx = Mongo.containsIgnoreCase(q);
            filters.add(or(regex("reference", rx), regex("customer.name", rx), regex("customer.phone", rx)));
        }
        Bson filter = filters.isEmpty() ? new Document() : and(filters);
        int s = Math.clamp(size <= 0 ? 20 : size, 1, 100);
        int p = Math.max(0, page);
        List<OrderSummary> items = Order.col().find(filter).sort(Sorts.descending("createdAt", "_id"))
                .skip(p * s).limit(s).map(OrderSummary::of).into(new ArrayList<>());
        return PageResult.of(items, p, s, Order.col().countDocuments(filter));
    }

    public OrderDetail get(String id) {
        return OrderDetail.of(find(Ids.parse(id, "Commande")));
    }

    /**
     * Change le statut et/ou le paiement. Le changement de statut est conditionné au statut lu
     * (« compare-and-set ») : deux administrateurs ne peuvent pas annuler deux fois la même commande.
     */
    public OrderDetail updateStatus(String id, StatusUpdate u) {
        ObjectId oid = Ids.parse(id, "Commande");
        Order o = find(oid);
        Date now = new Date();

        if (u.status() != null && u.status() != o.status) {
            OrderStatus target = u.status();
            if (o.status == OrderStatus.CANCELLED) {
                throw ApiException.badRequest("Une commande annulée ne peut pas être réactivée");
            }
            List<Bson> changes = new ArrayList<>(List.of(
                    set("status", target.name()),
                    push("history", new Document("status", target.name()).append("at", now)),
                    set("updatedAt", now)));
            if (target == OrderStatus.DELIVERED && o.payment.method == PaymentMethod.CASH_ON_DELIVERY) {
                changes.add(set("payment.status", PaymentStatus.PAID.name()));
            }
            if (target == OrderStatus.SHIPPED) {
                changes.add(set("shipment", shipmentDocument(o.shipment, u, now)));
            }
            Order updated = Order.col().findOneAndUpdate(
                    and(eq("_id", oid), eq("status", o.status.name())), combine(changes),
                    new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
            if (updated == null) {
                throw ApiException.conflict("La commande a été modifiée entre-temps : rechargez la page");
            }
            if (target == OrderStatus.CANCELLED) {
                updated.items.forEach(i -> inventory.release(i.productId, i.quantity));
                coupons.release(updated.couponCode);
            }
            if (target == OrderStatus.SHIPPED) {
                notifier.sendAsync(oid);
            }
            o = updated;
        } else if (o.status == OrderStatus.SHIPPED && hasTracking(u)) {
            // Correction du transporteur / suivi après expédition, sans renvoyer l'e-mail
            List<Bson> changes = new ArrayList<>();
            if (u.carrier() != null) changes.add(set("shipment.carrier", blankToNull(u.carrier())));
            if (u.trackingNumber() != null) changes.add(set("shipment.trackingNumber", blankToNull(u.trackingNumber())));
            if (u.trackingUrl() != null) changes.add(set("shipment.trackingUrl", blankToNull(u.trackingUrl())));
            changes.add(set("updatedAt", now));
            Order.col().updateOne(eq("_id", oid), combine(changes));
        }

        if (u.paymentStatus() != null && u.paymentStatus() != o.payment.status) {
            Order.col().updateOne(eq("_id", oid),
                    combine(set("payment.status", u.paymentStatus().name()), set("updatedAt", now)));
        }
        return OrderDetail.of(find(oid));
    }

    private static Document shipmentDocument(Order.Shipment previous, StatusUpdate u, Date now) {
        Document d = new Document("shippedAt", now);
        putIfPresent(d, "carrier", u.carrier() != null ? blankToNull(u.carrier()) : previous == null ? null : previous.carrier);
        putIfPresent(d, "trackingNumber", u.trackingNumber() != null ? blankToNull(u.trackingNumber())
                : previous == null ? null : previous.trackingNumber);
        putIfPresent(d, "trackingUrl", u.trackingUrl() != null ? blankToNull(u.trackingUrl())
                : previous == null ? null : previous.trackingUrl);
        return d;
    }

    private static void putIfPresent(Document d, String key, String value) {
        if (value != null) d.append(key, value);
    }

    private static boolean hasTracking(StatusUpdate u) {
        return u.carrier() != null || u.trackingNumber() != null || u.trackingUrl() != null;
    }

    Order find(ObjectId id) {
        Order o = Order.findById(id);
        if (o == null) throw ApiException.notFound("Commande");
        return o;
    }

    private String newReference() {
        StringBuilder sb = new StringBuilder("CMD-")
                .append(LocalDate.now(SHOP_ZONE).format(DateTimeFormatter.BASIC_ISO_DATE)).append('-');
        for (int i = 0; i < 5; i++) {
            sb.append(REF_CHARS.charAt(RANDOM.nextInt(REF_CHARS.length())));
        }
        return sb.toString();
    }

    private static String digits(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
