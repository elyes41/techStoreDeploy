package tn.techstore.orders;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static tn.techstore.common.Ids.str;

/** Format JSON de l'API : volontairement « à plat » pour le frontend, quelle que soit la forme du document. */
public final class OrderDtos {

    private OrderDtos() {
    }

    public record CheckoutItem(@NotBlank String productId, @Min(1) @Max(20) int quantity) {
    }

    public record CheckoutRequest(
            @NotBlank @Size(max = 150) String customerName,
            @NotBlank @Pattern(regexp = "^(\\+?216)?[\\s.-]?\\d{2}[\\s.-]?\\d{3}[\\s.-]?\\d{3}$",
                    message = "numéro de téléphone tunisien invalide") String phone,
            @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 60) String governorate,
            @Size(max = 100) String city,
            @NotBlank @Size(max = 300) String address,
            @Size(max = 500) String note,
            @NotNull PaymentMethod paymentMethod,
            @Size(max = 40) String couponCode,
            @NotEmpty @Size(max = 50) List<@Valid CheckoutItem> items) {
    }

    public record OrderItemDto(String productId, String productName, BigDecimal unitPrice, int quantity,
                               BigDecimal lineTotal) {
    }

    public record HistoryDto(OrderStatus status, Instant at) {
    }

    public record OrderDetail(
            String id, String reference, String customerName, String phone, String email,
            String governorate, String city, String address, String note,
            PaymentMethod paymentMethod, PaymentStatus paymentStatus, OrderStatus status,
            BigDecimal subtotal, String couponCode, BigDecimal discount, BigDecimal shippingFee,
            BigDecimal fiscalStamp, BigDecimal total, Instant createdAt, Instant updatedAt,
            String carrier, String trackingNumber, String trackingUrl, Instant shippedAt,
            Instant shippingEmailSentAt, String shippingEmailError,
            List<OrderItemDto> items, List<HistoryDto> history) {

        public static OrderDetail of(Order o) {
            var s = o.shipment;
            return new OrderDetail(str(o.id), o.reference, o.customer.name, o.customer.phone, o.customer.email,
                    o.delivery.governorate, o.delivery.city, o.delivery.address, o.delivery.note,
                    o.payment.method, o.payment.status, o.status,
                    o.amounts.subtotal, o.couponCode, o.amounts.discount, o.amounts.shippingFee,
                    o.amounts.fiscalStamp, o.amounts.total, o.createdAt, o.updatedAt,
                    s == null ? null : s.carrier, s == null ? null : s.trackingNumber,
                    s == null ? null : s.trackingUrl, s == null ? null : s.shippedAt,
                    s == null ? null : s.emailSentAt, s == null ? null : s.emailError,
                    o.items.stream().map(i -> new OrderItemDto(str(i.productId), i.name, i.unitPrice, i.quantity,
                            i.lineTotal)).toList(),
                    o.history.stream().map(h -> new HistoryDto(h.status, h.at)).toList());
        }
    }

    public record OrderSummary(
            String id, String reference, String customerName, String phone, String governorate,
            BigDecimal total, OrderStatus status, PaymentMethod paymentMethod, PaymentStatus paymentStatus,
            int itemCount, Instant createdAt) {

        public static OrderSummary of(Order o) {
            return new OrderSummary(str(o.id), o.reference, o.customer.name, o.customer.phone, o.delivery.governorate,
                    o.amounts.total, o.status, o.payment.method, o.payment.status,
                    o.items.stream().mapToInt(i -> i.quantity).sum(), o.createdAt);
        }
    }

    /** Pour passer en SHIPPED, le transporteur et le numéro de suivi sont repris dans l'e-mail au client. */
    public record StatusUpdate(
            OrderStatus status,
            PaymentStatus paymentStatus,
            @Size(max = 80) String carrier,
            @Size(max = 80) String trackingNumber,
            @Size(max = 300) @Pattern(regexp = "^(https?://.*)?$", message = "le lien de suivi doit commencer par http(s)://")
            String trackingUrl) {
    }

    public record NotifyResult(boolean sent, String message, OrderDetail order) {
    }
}
