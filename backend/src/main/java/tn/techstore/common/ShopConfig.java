package tn.techstore.common;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@ConfigMapping(prefix = "shop")
public interface ShopConfig {

    String issuer();

    Duration tokenTtl();

    BigDecimal shippingFee();

    /** Livraison offerte à partir de ce sous-total (après remise). */
    BigDecimal freeShippingThreshold();

    BigDecimal fiscalStamp();

    int lowStockThreshold();

    String uploadDir();

    /**
     * Où sont rangées les images téléversées depuis l'admin :
     * « disk » (dossier upload-dir, VPS / Docker) ou « mongo » (GridFS dans la base, hébergeurs sans disque persistant comme Render).
     */
    @WithDefault("disk")
    String storage();

    /** Clé API Brevo : si renseignée, les e-mails partent par l'API HTTP de Brevo au lieu du SMTP (bloqué sur Render Free). */
    Optional<String> brevoApiKey();

    /** Adresse publique du site, utilisée dans les liens des e-mails (ex. https://techstore.tn). */
    String publicUrl();

    /** Numéro WhatsApp au format international sans + (ex. 21622123456). Vide = bouton masqué. */
    Optional<String> whatsappNumber();

    /** Insère les données de démonstration au premier démarrage si la base est vide. */
    @WithDefault("true")
    boolean demoData();

    Admin admin();

    interface Admin {
        String email();

        String password();

        String fullName();
    }
}
