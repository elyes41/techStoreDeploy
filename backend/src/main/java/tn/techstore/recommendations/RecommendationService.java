package tn.techstore.recommendations;

import com.mongodb.client.model.Sorts;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import tn.techstore.catalog.CatalogDtos.ProductSummary;
import tn.techstore.catalog.Product;
import tn.techstore.common.ApiException;
import tn.techstore.mongo.Mongo;
import tn.techstore.mongo.Pipelines;
import tn.techstore.orders.Order;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;

/**
 * Recommandations « à la Amazon », calculées par des agrégations MongoDB (resources/mongo/pipelines)
 * à partir du catalogue, des commandes et des consultations. Aucune donnée inventée : un bloc sans
 * historique suffisant revient vide et n'est pas affiché.
 */
@ApplicationScoped
public class RecommendationService {

    private static final Pattern VISITOR_ID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");
    private static final Duration VIEW_DEDUP = Duration.ofMinutes(30);
    private static final Duration HISTORY = Duration.ofDays(90);
    private static final BigDecimal NEAR_LOW = new BigDecimal("0.7");
    private static final BigDecimal NEAR_HIGH = new BigDecimal("1.3");
    private static final BigDecimal BAND_LOW = new BigDecimal("0.6");
    private static final BigDecimal BAND_HIGH = new BigDecimal("1.4");

    public record ProductRecommendations(List<ProductSummary> similar, List<ProductSummary> boughtTogether,
                                         List<ProductSummary> alsoViewed, long viewsLast24h) {
    }

    // ---------- Consultations ----------

    /** Enregistre une consultation (une seule par visiteur et produit toutes les 30 min). */
    public long recordView(String slug, String visitorId) {
        Product p = Product.bySlug(slug).filter(x -> x.active).orElseThrow(() -> ApiException.notFound("Produit"));
        if (visitorId != null && VISITOR_ID.matcher(visitorId).matches()) {
            Instant now = Instant.now();
            boolean recent = ProductView.col().countDocuments(and(eq("productId", p.id), eq("visitorId", visitorId),
                    gt("viewedAt", Date.from(now.minus(VIEW_DEDUP))))) > 0;
            if (!recent) {
                ProductView v = new ProductView();
                v.productId = p.id;
                v.visitorId = visitorId;
                v.viewedAt = now;
                v.persist();
            }
        }
        return uniqueVisitors(p.id, Instant.now().minus(Duration.ofHours(24)));
    }

    // ---------- Fiche produit ----------

    public ProductRecommendations forProduct(String slug) {
        Product p = Product.bySlug(slug).orElseThrow(() -> ApiException.notFound("Produit"));
        return new ProductRecommendations(similar(p, 8), boughtTogether(p.id, 3), alsoViewed(p.id, 8),
                uniqueVisitors(p.id, Instant.now().minus(Duration.ofHours(24))));
    }

    /** Même catégorie ou même marque, classés par proximité de prix, stock, ventes et note. */
    public List<ProductSummary> similar(Product p, int limit) {
        List<ObjectId> categoryIds = p.category == null ? List.of() : List.of(p.category.id);
        List<String> brands = p.brand == null ? List.of() : List.of(p.brand);
        Map<String, Object> params = new HashMap<>();
        params.put("productId", p.id);
        params.put("categoryIds", categoryIds);
        params.put("brands", brands);
        params.put("minPrice", p.price.multiply(NEAR_LOW));
        params.put("maxPrice", p.price.multiply(NEAR_HIGH));
        params.put("limit", limit);
        return Product.col().aggregate(Pipelines.of("similar-products", params))
                .map(ProductSummary::of).into(new ArrayList<>());
    }

    /** Produits présents dans les mêmes commandes (hors commandes annulées). */
    public List<ProductSummary> boughtTogether(ObjectId productId, int limit) {
        List<ObjectId> ranked = rankedIds(Order.col().withDocumentClass(Document.class)
                .aggregate(Pipelines.of("bought-together", Map.of("productId", productId, "limit", limit * 3))));
        return load(ranked, true, limit);
    }

    /** « Les clients ayant consulté ce produit ont aussi regardé » (90 derniers jours). */
    public List<ProductSummary> alsoViewed(ObjectId productId, int limit) {
        List<ObjectId> ranked = rankedIds(ProductView.col().withDocumentClass(Document.class)
                .aggregate(Pipelines.of("also-viewed", Map.of("productId", productId,
                        "since", Instant.now().minus(HISTORY), "limit", limit * 2))));
        return load(ranked, false, limit);
    }

    // ---------- Accueil ----------

    /** Tendances : unités vendues (x5) + visiteurs uniques des 7 derniers jours. */
    public List<ProductSummary> trending(int limit) {
        return load(trendingIds(limit, Set.of()), true, limit);
    }

    /** « Recommandé pour vous » à partir des produits récemment consultés par le visiteur. */
    public List<ProductSummary> forYou(List<ObjectId> seedIds, int limit) {
        List<ObjectId> wanted = seedIds.stream().distinct().limit(20).toList();
        List<Product> seeds = wanted.isEmpty() ? List.of() : Product.col().find(in("_id", wanted)).into(new ArrayList<>());
        if (seeds.isEmpty()) {
            return trending(limit);
        }
        List<ObjectId> seedCategories = new ArrayList<>();
        List<String> seedBrands = new ArrayList<>();
        List<Document> bands = new ArrayList<>();
        for (Product s : seeds) {
            if (s.category != null) seedCategories.add(s.category.id);
            if (s.brand != null) seedBrands.add(s.brand);
            bands.add(new Document("min", new Decimal128(s.price.multiply(BAND_LOW)))
                    .append("max", new Decimal128(s.price.multiply(BAND_HIGH))));
        }
        Map<String, Object> params = new HashMap<>();
        params.put("excludeIds", wanted);
        params.put("seedCategoryIds", seedCategories);
        params.put("seedBrands", seedBrands);
        params.put("seedPriceBands", bands);
        params.put("limit", limit);
        List<ProductSummary> result = Product.col().aggregate(Pipelines.of("for-you", params))
                .map(ProductSummary::of).into(new ArrayList<>());

        if (result.size() < limit) {
            Set<ObjectId> exclude = new HashSet<>(wanted);
            result.forEach(r -> exclude.add(new ObjectId(r.id())));
            result.addAll(load(trendingIds(limit - result.size(), exclude), true, limit - result.size()));
        }
        return result;
    }

    // ---------- Utilitaires ----------

    private List<ObjectId> trendingIds(int limit, Set<ObjectId> exclude) {
        Instant since = Instant.now().minus(Duration.ofDays(7));
        Map<ObjectId, Long> score = new HashMap<>();
        Order.col().withDocumentClass(Document.class)
                .aggregate(Pipelines.of("trending-sales", Map.of("since", since)))
                .forEach(d -> score.merge(d.getObjectId("_id"), 5 * Mongo.count(d.get("quantity")), Long::sum));
        ProductView.col().withDocumentClass(Document.class)
                .aggregate(Pipelines.of("trending-views", Map.of("since", since)))
                .forEach(d -> score.merge(d.getObjectId("_id"), Mongo.count(d.get("visitors")), Long::sum));

        List<ObjectId> ranked = score.entrySet().stream()
                .filter(e -> !exclude.contains(e.getKey()))
                .sorted(Map.Entry.<ObjectId, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey).limit(limit * 3L).toList();

        // Complète avec les produits phares et les meilleures ventes quand l'activité récente est faible
        List<ObjectId> result = new ArrayList<>(ranked);
        if (result.size() < limit * 3) {
            Set<ObjectId> skip = new HashSet<>(exclude);
            skip.addAll(result);
            Product.col().find(and(eq("active", true), gt("stock", 0), nin("_id", skip)))
                    .sort(Sorts.orderBy(Sorts.descending("featured", "soldCount"), Sorts.ascending("_id")))
                    .limit(limit * 3 - result.size())
                    .forEach(p -> result.add(p.id));
        }
        return result;
    }

    private long uniqueVisitors(ObjectId productId, Instant since) {
        Document d = ProductView.col().withDocumentClass(Document.class)
                .aggregate(Pipelines.of("unique-visitors", Map.of("productId", productId, "since", since))).first();
        return d == null ? 0 : Mongo.count(d.get("visitors"));
    }

    private static List<ObjectId> rankedIds(Iterable<Document> rows) {
        List<ObjectId> ids = new ArrayList<>();
        rows.forEach(d -> {
            if (d.get("_id") instanceof ObjectId id) ids.add(id);
        });
        return ids;
    }

    /** Charge les produits actifs en conservant l'ordre du classement. */
    private static List<ProductSummary> load(List<ObjectId> ids, boolean inStockOnly, int limit) {
        if (ids.isEmpty()) return List.of();
        Map<ObjectId, Product> byId = new HashMap<>();
        var filter = inStockOnly ? and(in("_id", ids), eq("active", true), gt("stock", 0))
                : and(in("_id", ids), eq("active", true));
        Product.col().find(filter).forEach(p -> byId.put(p.id, p));
        return ids.stream().map(byId::get).filter(Objects::nonNull).limit(limit).map(ProductSummary::of).toList();
    }
}
