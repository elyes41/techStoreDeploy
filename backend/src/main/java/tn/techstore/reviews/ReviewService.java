package tn.techstore.reviews;

import com.mongodb.client.model.Sorts;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.types.ObjectId;
import tn.techstore.catalog.Product;
import tn.techstore.common.ApiException;
import tn.techstore.common.Ids;
import tn.techstore.common.PageResult;
import tn.techstore.mongo.Mongo;
import tn.techstore.mongo.Pipelines;
import tn.techstore.orders.Order;
import tn.techstore.orders.OrderStatus;
import tn.techstore.reviews.ReviewDtos.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

@ApplicationScoped
public class ReviewService {

    /** Un avis déposé est en attente de modération ; la note du produit ne change qu'à la publication. */
    public void submit(String slug, ReviewForm form) {
        Product p = Product.bySlug(slug).filter(x -> x.active).orElseThrow(() -> ApiException.notFound("Produit"));
        Review r = new Review();
        r.productId = p.id;
        r.authorName = form.authorName().trim();
        r.rating = form.rating();
        r.title = blankToNull(form.title());
        r.comment = blankToNull(form.comment());
        r.verifiedPurchase = isVerified(form.orderReference(), p.id);
        r.status = Review.Status.PENDING;
        r.createdAt = Instant.now();
        r.persist();
    }

    public ReviewPage publicReviews(String slug, int page, int size) {
        Product p = Product.bySlug(slug).orElseThrow(() -> ApiException.notFound("Produit"));
        int s = Math.clamp(size <= 0 ? 5 : size, 1, 20);
        int pg = Math.max(0, page);
        var filter = and(eq("productId", p.id), eq("status", Review.Status.APPROVED.name()));
        List<ReviewDto> items = Review.col().find(filter)
                .sort(Sorts.descending("verifiedPurchase", "createdAt")).skip(pg * s).limit(s)
                .map(r -> new ReviewDto(Ids.str(r.id), r.authorName, r.rating, r.title, r.comment, r.verifiedPurchase,
                        r.createdAt))
                .into(new ArrayList<>());
        long total = Review.col().countDocuments(filter);
        return new ReviewPage(summary(p.id), items, pg, (int) Math.ceil(total / (double) s));
    }

    // ---------- Administration ----------

    public PageResult<AdminReviewDto> list(Review.Status status, int page, int size) {
        int s = Math.clamp(size <= 0 ? 20 : size, 1, 100);
        int pg = Math.max(0, page);
        var filter = status == null ? new Document() : eq("status", status.name());
        List<Review> reviews = Review.col().find(filter).sort(Sorts.descending("createdAt"))
                .skip(pg * s).limit(s).into(new ArrayList<>());

        // Noms des produits concernés, en une seule requête
        Map<ObjectId, Product> products = new HashMap<>();
        Set<ObjectId> ids = new HashSet<>();
        reviews.forEach(r -> ids.add(r.productId));
        if (!ids.isEmpty()) Product.col().find(in("_id", ids)).forEach(p -> products.put(p.id, p));

        List<AdminReviewDto> items = reviews.stream().map(r -> {
            Product p = products.get(r.productId);
            return new AdminReviewDto(Ids.str(r.id), Ids.str(r.productId), p == null ? "(produit supprimé)" : p.name,
                    p == null ? null : p.slug, r.authorName, r.rating, r.title, r.comment, r.status,
                    r.verifiedPurchase, r.createdAt);
        }).toList();
        return PageResult.of(items, pg, s, Review.col().countDocuments(filter));
    }

    public AdminReviewDto moderate(String id, Review.Status status) {
        Review r = find(id);
        r.status = status;
        r.update();
        refreshProductRating(r.productId);
        Product p = Product.findById(r.productId);
        return new AdminReviewDto(Ids.str(r.id), Ids.str(r.productId), p == null ? null : p.name,
                p == null ? null : p.slug, r.authorName, r.rating, r.title, r.comment, r.status,
                r.verifiedPurchase, r.createdAt);
    }

    public void delete(String id) {
        Review r = find(id);
        r.delete();
        refreshProductRating(r.productId);
    }

    public long pendingCount() {
        return Review.col().countDocuments(eq("status", Review.Status.PENDING.name()));
    }

    /** Recalcule products.rating à partir des avis publiés (pipeline review-summary). */
    public void refreshProductRating(ObjectId productId) {
        ReviewSummary s = summary(productId);
        Product.col().updateOne(eq("_id", productId),
                combine(set("rating", new Document("average", s.average()).append("count", s.count())),
                        set("updatedAt", new Date())));
    }

    private ReviewSummary summary(ObjectId productId) {
        Document result = Review.col().withDocumentClass(Document.class)
                .aggregate(Pipelines.of("review-summary", Map.of("productId", productId))).first();
        Long[] dist = {0L, 0L, 0L, 0L, 0L};
        double average = 0;
        int count = 0;
        if (result != null) {
            List<Document> stats = result.getList("stats", Document.class);
            if (!stats.isEmpty()) {
                count = (int) Mongo.count(stats.get(0).get("count"));
                Object avg = stats.get(0).get("average");
                average = avg == null ? 0 : BigDecimal.valueOf(((Number) avg).doubleValue())
                        .setScale(2, RoundingMode.HALF_UP).doubleValue();
            }
            for (Document d : result.getList("distribution", Document.class)) {
                int stars = (int) Mongo.count(d.get("_id"));
                if (stars >= 1 && stars <= 5) dist[5 - stars] = Mongo.count(d.get("count"));
            }
        }
        return new ReviewSummary(average, count, List.of(dist));
    }

    private boolean isVerified(String orderReference, ObjectId productId) {
        if (orderReference == null || orderReference.isBlank()) return false;
        return Order.col().countDocuments(and(
                eq("reference", orderReference.trim().toUpperCase()),
                eq("status", OrderStatus.DELIVERED.name()),
                eq("items.productId", productId))) > 0;
    }

    private Review find(String id) {
        Review r = Review.findById(Ids.parse(id, "Avis"));
        if (r == null) throw ApiException.notFound("Avis");
        return r;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
