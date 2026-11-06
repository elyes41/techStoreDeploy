package tn.techstore.orders;

public enum PaymentMethod {
    CASH_ON_DELIVERY, // paiement à la livraison
    CARD,             // carte bancaire (passerelle ClicToPay / SMT à brancher)
    E_DINAR,          // e-Dinar La Poste
    MOBILE_WALLET     // Flouci, Konnect...
}
