package tn.techstore.recommendations;

import com.mongodb.client.MongoCollection;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.time.Instant;

/**
 * Collection « product_views » : une consultation de fiche par un visiteur anonyme.
 * Un index TTL supprime automatiquement les vues de plus de 90 jours.
 */
@MongoEntity(collection = "product_views")
public class ProductView extends PanacheMongoEntity {

    public ObjectId productId;
    public String visitorId;
    public Instant viewedAt;

    public static MongoCollection<ProductView> col() {
        return mongoCollection();
    }
}
