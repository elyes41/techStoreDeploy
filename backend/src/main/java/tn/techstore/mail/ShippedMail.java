package tn.techstore.mail;

import io.quarkus.qute.TemplateData;

import java.util.List;

/** Données de l'e-mail « votre colis est parti » (montants déjà formatés pour le gabarit). */
@TemplateData
public record ShippedMail(
        String customerName,
        String reference,
        String carrier,
        String trackingNumber,
        String trackingUrl,
        String orderUrl,
        String shopName,
        String shopUrl,
        String address,
        String deliveryWindow,
        List<Line> items,
        String total,
        String paymentInfo) {

    @TemplateData
    public record Line(String name, int quantity, String total) {
    }
}
