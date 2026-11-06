package tn.techstore.reviews;

import com.mongodb.client.MongoCollection;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.time.Instant;

/**
 * Collection « reviews », séparée des produits car elle grossit sans limite.
 * La note moyenne et le nombre d'avis publiés sont recopiés dans products.rating.
 */
@MongoEntity(collection = "reviews")
public class Review extends PanacheMongoEntity {

    public enum Status { PENDING, APPROVED, REJECTED }

    public ObjectId productId;
    public String authorName;
    public int rating;
    public String title;
    public String comment;
    public Status status = Status.PENDING;
    /** Vrai si l'auteur a donné la référence d'une commande livrée contenant ce produit. */
    public boolean verifiedPurchase;
    public Instant createdAt;

    public static MongoCollection<Review> col() {
        return mongoCollection();
    }
}
