package tn.techstore.catalog;

import com.mongodb.client.MongoCollection;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

import java.time.Instant;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

/** Collection « categories ». Le nom et le slug sont recopiés dans chaque produit (voir Product.category). */
@MongoEntity(collection = "categories")
public class Category extends PanacheMongoEntity {

    public String name;
    public String slug;
    public Instant createdAt;

    public static MongoCollection<Category> col() {
        return mongoCollection();
    }

    public static Optional<Category> bySlug(String slug) {
        return Optional.ofNullable(col().find(eq("slug", slug)).first());
    }
}
