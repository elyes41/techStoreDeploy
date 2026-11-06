package tn.techstore.catalog;

import com.mongodb.client.model.Sorts;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.types.ObjectId;
import tn.techstore.catalog.CatalogDtos.CategoryDto;
import tn.techstore.common.ApiException;
import tn.techstore.common.Ids;
import tn.techstore.common.Slugs;
import tn.techstore.mongo.Mongo;
import tn.techstore.mongo.Pipelines;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

@ApplicationScoped
public class CategoryService {

    /** @param activeOnly ne compter que les produits visibles en boutique */
    public List<CategoryDto> list(boolean activeOnly) {
        Document match = activeOnly ? new Document("active", true) : new Document();
        Map<ObjectId, Long> counts = new HashMap<>();
        Product.col().withDocumentClass(Document.class)
                .aggregate(Pipelines.of("category-counts", Map.of("match", match)))
                .forEach(d -> {
                    if (d.get("_id") instanceof ObjectId id) counts.put(id, Mongo.count(d.get("count")));
                });
        return Category.col().find().sort(Sorts.ascending("name"))
                .map(c -> new CategoryDto(Ids.str(c.id), c.name, c.slug, counts.getOrDefault(c.id, 0L)))
                .into(new java.util.ArrayList<>());
    }

    public CategoryDto create(String name) {
        Category c = new Category();
        c.name = name.trim();
        c.slug = Slugs.unique(Slugs.slugify(name), s -> Category.col().countDocuments(eq("slug", s)) > 0);
        c.createdAt = Instant.now();
        c.persist();
        return new CategoryDto(Ids.str(c.id), c.name, c.slug, 0);
    }

    /** Renomme la catégorie et met à jour la copie de son nom dans tous les produits. */
    public CategoryDto rename(String id, String name) {
        Category c = find(id);
        c.name = name.trim();
        c.update();
        Product.col().updateMany(eq("category._id", c.id),
                combine(set("category.name", c.name), set("updatedAt", new java.util.Date())));
        return new CategoryDto(Ids.str(c.id), c.name, c.slug, Product.col().countDocuments(eq("category._id", c.id)));
    }

    public void delete(String id) {
        Category c = find(id);
        long used = Product.col().countDocuments(eq("category._id", c.id));
        if (used > 0) {
            throw ApiException.conflict("Impossible de supprimer : " + used + " produit(s) utilisent cette catégorie");
        }
        c.delete();
    }

    private Category find(String id) {
        Category c = Category.findById(Ids.parse(id, "Catégorie"));
        if (c == null) throw ApiException.notFound("Catégorie");
        return c;
    }
}
