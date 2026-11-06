package tn.techstore.mongo;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.techstore.catalog.Category;
import tn.techstore.catalog.Product;
import tn.techstore.common.ShopConfig;
import tn.techstore.common.Slugs;
import tn.techstore.coupons.Coupon;
import tn.techstore.reviews.Review;
import tn.techstore.reviews.ReviewService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Données de démonstration (resources/mongo/demo-data.json), insérées seulement si la collection
 * « products » est vide et que shop.demo-data=true. Désactivez-les avant la mise en production (DEMO_DATA=false).
 */
@ApplicationScoped
public class DemoData {

    @Inject
    ShopConfig config;

    @Inject
    ReviewService reviews;

    public void seedIfEmpty() {
        if (!config.demoData() || Product.col().countDocuments() > 0) {
            return;
        }
        Document demo = Document.parse(Mongo.resource("mongo/demo-data.json"));
        Instant now = Instant.now();

        Map<String, Category> categories = new HashMap<>();
        for (Document d : demo.getList("categories", Document.class)) {
            Category c = Category.bySlug(d.getString("slug")).orElseGet(() -> {
                Category n = new Category();
                n.name = d.getString("name");
                n.slug = d.getString("slug");
                n.createdAt = now;
                n.persist();
                return n;
            });
            categories.put(c.slug, c);
        }

        Map<String, Product> bySku = new HashMap<>();
        for (Document d : demo.getList("products", Document.class)) {
            Product p = new Product();
            p.sku = d.getString("sku");
            p.name = d.getString("name");
            p.slug = Slugs.slugify(p.name);
            p.brand = d.getString("brand");
            p.description = d.getString("description");
            p.price = new BigDecimal(d.getString("price"));
            p.oldPrice = d.containsKey("oldPrice") ? new BigDecimal(d.getString("oldPrice")) : null;
            p.stock = d.getInteger("stock");
            p.active = true;
            p.featured = d.getBoolean("featured", false);
            p.soldCount = 0;
            Category c = categories.get(d.getString("category"));
            p.category = c == null ? null : new Product.CategoryRef(c);
            p.images = new ArrayList<>();
            p.specs = new ArrayList<>();
            Document specs = d.get("specs", Document.class);
            if (specs != null) specs.forEach((k, v) -> p.specs.add(new Product.Spec(k, String.valueOf(v))));
            p.rating = new Product.Rating();
            p.createdAt = p.updatedAt = now;
            p.persist();
            bySku.put(p.sku, p);
        }

        Set<Product> rated = new HashSet<>();
        for (Document d : demo.getList("reviews", Document.class)) {
            Product p = bySku.get(d.getString("sku"));
            if (p == null) continue;
            Review r = new Review();
            r.productId = p.id;
            r.authorName = d.getString("author");
            r.rating = d.getInteger("rating");
            r.title = d.getString("title");
            r.comment = d.getString("comment");
            r.status = Review.Status.APPROVED;
            r.verifiedPurchase = d.getBoolean("verified", false);
            r.createdAt = now;
            r.persist();
            rated.add(p);
        }
        rated.forEach(p -> reviews.refreshProductRating(p.id));

        for (Document d : demo.getList("coupons", Document.class)) {
            if (Coupon.byCode(d.getString("code")).isPresent()) continue;
            Coupon c = new Coupon();
            c.code = Coupon.normalize(d.getString("code"));
            c.type = Coupon.Type.valueOf(d.getString("type"));
            c.value = new BigDecimal(d.getString("value"));
            c.minOrder = new BigDecimal(d.getString("minOrder"));
            c.usedCount = 0;
            c.active = true;
            c.createdAt = now;
            c.persist();
        }
        Log.infof("Données de démonstration insérées : %d catégories, %d produits", categories.size(), bySku.size());
    }
}
