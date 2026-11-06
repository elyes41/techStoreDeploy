package tn.techstore.mail;

import io.quarkus.logging.Log;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import tn.techstore.common.ShopConfig;
import tn.techstore.orders.Order;
import tn.techstore.orders.PaymentMethod;
import tn.techstore.orders.PaymentStatus;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.*;

/** Envoie au client l'e-mail « votre colis est en route » quand la commande passe en « expédiée ». */
@ApplicationScoped
public class ShippingNotifier {

    private static final ZoneId SHOP_ZONE = ZoneId.of("Africa/Tunis");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH);

    @Inject
    MailGateway mail;

    @Inject
    ShopConfig config;

    @Location("mail/order-shipped.html")
    Template htmlTemplate;

    @Location("mail/order-shipped.txt")
    Template textTemplate;

    /** Envoi en arrière-plan : l'administrateur n'attend pas le serveur SMTP. */
    public void sendAsync(ObjectId orderId) {
        Infrastructure.getDefaultWorkerPool().execute(() -> send(orderId));
    }

    /**
     * Envoie l'e-mail et enregistre le résultat dans shipment.emailSentAt / shipment.emailError.
     *
     * @return null si envoyé, sinon le motif de l'échec
     */
    public String send(ObjectId orderId) {
        Order o;
        try {
            o = Order.findById(orderId);
        } catch (RuntimeException e) {
            Log.errorf(e, "Lecture de la commande %s impossible", orderId);
            return "Erreur interne";
        }
        if (o == null) return "Commande introuvable";
        if (o.customer.email == null || o.customer.email.isBlank()) {
            return record(orderId, false, "Le client n'a pas indiqué d'adresse e-mail");
        }

        try {
            ShippedMail data = toMail(o);
            mail.send(o.customer.email, "Votre commande " + o.reference + " est en route",
                    htmlTemplate.data("m", data).render(), textTemplate.data("m", data).render());
            Log.infof("E-mail d'expédition envoyé à %s (commande %s)", o.customer.email, o.reference);
            return record(orderId, true, null);
        } catch (RuntimeException e) {
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            if (reason.length() > 200) reason = reason.substring(0, 200) + "…";
            Log.errorf(e, "Échec de l'envoi de l'e-mail d'expédition (commande %s)", o.reference);
            return record(orderId, false, "Échec de l'envoi : " + reason);
        }
    }

    private String record(ObjectId orderId, boolean sent, String error) {
        try {
            Order.col().updateOne(eq("_id", orderId), sent
                    ? combine(set("shipment.emailSentAt", new Date()), unset("shipment.emailError"))
                    : set("shipment.emailError", error));
        } catch (RuntimeException e) {
            Log.errorf(e, "Statut de l'e-mail non enregistré pour la commande %s", orderId);
        }
        return error;
    }

    private ShippedMail toMail(Order o) {
        String base = config.publicUrl().replaceAll("/+$", "");
        var items = o.items.stream()
                .map(i -> new ShippedMail.Line(i.name, i.quantity, money(i.lineTotal)))
                .toList();
        String payment;
        if (o.payment.status == PaymentStatus.PAID) {
            payment = "Commande déjà payée : rien à régler à la réception.";
        } else if (o.payment.method == PaymentMethod.CASH_ON_DELIVERY) {
            payment = "À régler au livreur : " + money(o.amounts.total) + " (prévoyez si possible l'appoint).";
        } else {
            payment = "Montant de la commande : " + money(o.amounts.total) + ".";
        }
        var d = o.delivery;
        String address = d.address + (d.city == null ? "" : ", " + d.city) + ", " + d.governorate;
        LocalDate today = LocalDate.now(SHOP_ZONE);
        String window = capitalize(DAY.format(today.plusDays(1))) + " – " + DAY.format(today.plusDays(3));
        var s = o.shipment;

        return new ShippedMail(
                firstName(o.customer.name),
                o.reference,
                s == null ? null : s.carrier,
                s == null ? null : s.trackingNumber,
                s == null ? null : s.trackingUrl,
                base + "/commande/" + o.reference,
                "TechStore",
                base,
                address,
                window,
                items,
                money(o.amounts.total),
                payment);
    }

    static String money(BigDecimal v) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.FRENCH);
        sym.setGroupingSeparator(' ');
        sym.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.###", sym).format(v) + " DT";
    }

    private static String firstName(String full) {
        String t = full == null ? "" : full.trim();
        int sp = t.indexOf(' ');
        return sp > 0 ? t.substring(0, sp) : t;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
