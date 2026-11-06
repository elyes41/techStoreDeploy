package tn.techstore.admin;

import com.mongodb.client.model.Sorts;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.techstore.admin.AdminDtos.*;
import tn.techstore.catalog.Product;
import tn.techstore.common.Ids;
import tn.techstore.common.ShopConfig;
import tn.techstore.mongo.Mongo;
import tn.techstore.mongo.Pipelines;
import tn.techstore.orders.Order;
import tn.techstore.orders.OrderDtos.OrderSummary;
import tn.techstore.orders.OrderService;
import tn.techstore.orders.OrderStatus;
import tn.techstore.reviews.ReviewService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static com.mongodb.client.model.Filters.*;

/** Indicateurs du tableau de bord, calculés par agrégation sur la collection orders. */
@ApplicationScoped
public class StatsService {

    @Inject
    ShopConfig config;

    @Inject
    ReviewService reviews;

    public DashboardStats dashboard() {
        int threshold = config.lowStockThreshold();
        LocalDate today = LocalDate.now(OrderService.SHOP_ZONE);
        Instant since30 = today.minusDays(29).atStartOfDay(OrderService.SHOP_ZONE).toInstant();
        Instant since7 = today.minusDays(6).atStartOfDay(OrderService.SHOP_ZONE).toInstant();

        // Ventes des 7 derniers jours, regroupées par jour à l'heure de Tunis ; les jours sans vente valent 0
        Map<String, Document> byDay = new HashMap<>();
        Order.col().withDocumentClass(Document.class)
                .aggregate(Pipelines.of("sales-by-day", Map.of("since", since7, "timezone", OrderService.SHOP_ZONE.getId())))
                .forEach(d -> byDay.put(d.getString("_id"), d));
        List<DailySales> last7 = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String day = today.minusDays(i).toString();
            Document d = byDay.get(day);
            last7.add(new DailySales(day, d == null ? 0 : Mongo.count(d.get("orders")),
                    d == null ? BigDecimal.ZERO : Mongo.decimal(d.get("revenue"))));
        }

        List<LowStockItem> lowStock = Product.col().find(and(eq("active", true), lte("stock", threshold)))
                .sort(Sorts.ascending("stock", "_id")).limit(8)
                .map(p -> new LowStockItem(Ids.str(p.id), p.name, p.sku, p.stock)).into(new ArrayList<>());

        List<BestSeller> bestSellers = Product.col().find(gt("soldCount", 0))
                .sort(Sorts.descending("soldCount")).limit(5)
                .map(p -> new BestSeller(Ids.str(p.id), p.name, p.soldCount, p.rating == null ? 0 : p.rating.average))
                .into(new ArrayList<>());

        List<OrderSummary> recentOrders = Order.col().find().sort(Sorts.descending("createdAt")).limit(6)
                .map(OrderSummary::of).into(new ArrayList<>());

        var products = Product.col();
        return new DashboardStats(
                products.countDocuments(),
                products.countDocuments(eq("active", true)),
                products.countDocuments(and(eq("active", true), eq("stock", 0))),
                products.countDocuments(and(eq("active", true), gt("stock", 0), lte("stock", threshold))),
                Order.col().countDocuments(),
                Order.col().countDocuments(eq("status", OrderStatus.PENDING.name())),
                reviews.pendingCount(),
                revenueSince(Instant.EPOCH),
                revenueSince(since30),
                last7,
                recentOrders,
                lowStock,
                bestSellers);
    }

    private BigDecimal revenueSince(Instant since) {
        Document d = Order.col().withDocumentClass(Document.class)
                .aggregate(Pipelines.of("revenue", Map.of("since", since))).first();
        return d == null ? BigDecimal.ZERO : Mongo.decimal(d.get("revenue"));
    }
}
