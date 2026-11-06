package tn.techstore.orders;

import com.mongodb.client.MongoCollection;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Collection « orders » : une commande est un seul document, avec tout ce qu'il faut pour l'afficher
 * sans jointure (les lignes figent le nom et le prix au moment de l'achat).
 * <pre>
 * {
 *   _id, reference: "CMD-20261106-K7P2Q", status: "SHIPPED",
 *   customer: { name, phone, email },
 *   delivery: { governorate, city, address, note },
 *   payment:  { method: "CASH_ON_DELIVERY", status: "UNPAID" },
 *   items:    [ { productId, name, unitPrice, quantity, lineTotal } ],
 *   amounts:  { subtotal, discount, shippingFee, fiscalStamp, total },
 *   couponCode,
 *   shipment: { carrier, trackingNumber, trackingUrl, shippedAt, emailSentAt, emailError },
 *   history:  [ { status, at } ],
 *   createdAt, updatedAt
 * }
 * </pre>
 */
@MongoEntity(collection = "orders")
public class Order extends PanacheMongoEntity {

    public String reference;
    public OrderStatus status = OrderStatus.PENDING;
    public Customer customer = new Customer();
    public Delivery delivery = new Delivery();
    public Payment payment = new Payment();
    public List<Item> items = new ArrayList<>();
    public Amounts amounts = new Amounts();
    public String couponCode;
    public Shipment shipment;
    public List<StatusChange> history = new ArrayList<>();
    public Instant createdAt;
    public Instant updatedAt;

    public static MongoCollection<Order> col() {
        return mongoCollection();
    }

    public static class Customer {
        public String name;
        public String phone;
        public String email;
    }

    public static class Delivery {
        public String governorate;
        public String city;
        public String address;
        public String note;
    }

    public static class Payment {
        public PaymentMethod method;
        public PaymentStatus status = PaymentStatus.UNPAID;
    }

    public static class Item {
        /** Devient null si le produit est supprimé du catalogue. */
        public ObjectId productId;
        public String name;
        public BigDecimal unitPrice;
        public int quantity;
        public BigDecimal lineTotal;
    }

    public static class Amounts {
        public BigDecimal subtotal;
        public BigDecimal discount = BigDecimal.ZERO;
        public BigDecimal shippingFee;
        public BigDecimal fiscalStamp;
        public BigDecimal total;
    }

    public static class Shipment {
        public String carrier;
        public String trackingNumber;
        public String trackingUrl;
        public Instant shippedAt;
        /** Date d'envoi de l'e-mail « votre colis est parti » (null = pas envoyé). */
        public Instant emailSentAt;
        public String emailError;
    }

    public static class StatusChange {
        public OrderStatus status;
        public Instant at;

        public StatusChange() {
        }

        public StatusChange(OrderStatus status, Instant at) {
            this.status = status;
            this.at = at;
        }
    }
}
