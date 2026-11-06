package tn.techstore.catalog;

import com.mongodb.client.MongoCollection;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

/**
 * Collection « products ».
 * <pre>
 * {
 *   _id, name, slug, sku, brand, description,
 *   price: Decimal128, oldPrice: Decimal128, stock, active, featured, soldCount,
 *   category: { _id, name, slug },          // copie de la catégorie (lecture sans jointure)
 *   images: [ "/uploads/…" ],
 *   specs:  [ { name: "RAM", value: "8 Go" } ],   // « attribute pattern »
 *   rating: { average: 4.5, count: 12 },     // recalculé à chaque modération d'avis
 *   createdAt, updatedAt
 * }
 * </pre>
 */
@MongoEntity(collection = "products")
public class Product extends PanacheMongoEntity {

    public String name;
    public String slug;
    public String sku;
    public String brand;
    public String description;
    public BigDecimal price;
    public BigDecimal oldPrice;
    public int stock;
    public boolean active = true;
    public boolean featured;
    public int soldCount;
    public CategoryRef category;
    public List<String> images = new ArrayList<>();
    public List<Spec> specs = new ArrayList<>();
    public Rating rating = new Rating();
    public Instant createdAt;
    public Instant updatedAt;

    public static MongoCollection<Product> col() {
        return mongoCollection();
    }

    public static Optional<Product> bySlug(String slug) {
        return Optional.ofNullable(col().find(eq("slug", slug)).first());
    }

    /** Copie de la catégorie stockée dans le produit. */
    public static class CategoryRef {
        @BsonId
        public ObjectId id;
        public String name;
        public String slug;

        public CategoryRef() {
        }

        public CategoryRef(Category c) {
            this.id = c.id;
            this.name = c.name;
            this.slug = c.slug;
        }
    }

    public static class Spec {
        public String name;
        public String value;

        public Spec() {
        }

        public Spec(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class Rating {
        public double average;
        public int count;
    }
}
