package tn.techstore.orders;

import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.types.ObjectId;
import tn.techstore.catalog.Product;

import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

/**
 * Stock des produits. Chaque réservation est une seule opération atomique MongoDB :
 * elle ne réussit que s'il reste assez de stock, ce qui empêche la survente sans verrou ni transaction.
 */
@ApplicationScoped
public class Inventory {

    /** @return le produit après réservation, ou null si inactif, introuvable ou en stock insuffisant */
    public Product reserve(ObjectId productId, int quantity) {
        return Product.col().findOneAndUpdate(
                and(eq("_id", productId), eq("active", true), gte("stock", quantity)),
                combine(inc("stock", -quantity), inc("soldCount", quantity), set("updatedAt", new Date())),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
    }

    /**
     * Remet en stock (commande annulée ou échouée). Sans effet si le produit a été supprimé.
     * Mise à jour par pipeline pour que soldCount ne descende jamais sous 0 (le validateur l'interdit).
     */
    public void release(ObjectId productId, int quantity) {
        if (productId == null) return;
        Product.col().updateOne(eq("_id", productId), List.of(new Document("$set", new Document()
                .append("stock", new Document("$add", List.of("$stock", quantity)))
                .append("soldCount", new Document("$max", List.of(0,
                        new Document("$subtract", List.of("$soldCount", quantity)))))
                .append("updatedAt", new Date()))));
    }
}
