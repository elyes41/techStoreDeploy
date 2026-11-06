package tn.techstore.orders;

public enum OrderStatus {
    PENDING,     // reçue, à confirmer par téléphone
    CONFIRMED,   // confirmée, en préparation
    SHIPPED,     // remise au livreur (e-mail envoyé au client)
    DELIVERED,   // livrée
    CANCELLED    // annulée (stock et code promo restitués)
}
