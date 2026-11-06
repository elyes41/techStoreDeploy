package tn.techstore.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import tn.techstore.orders.OrderDtos.OrderSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record LoginResponse(String token, String email, String fullName, Instant expiresAt) {
    }

    public record DailySales(String day, long orders, BigDecimal revenue) {
    }

    public record LowStockItem(String id, String name, String sku, int stock) {
    }

    public record BestSeller(String id, String name, int soldCount, double ratingAvg) {
    }

    public record DashboardStats(
            long productCount,
            long activeProductCount,
            long outOfStockCount,
            long lowStockCount,
            long orderCount,
            long pendingOrderCount,
            long pendingReviewCount,
            BigDecimal revenue,
            BigDecimal revenueLast30Days,
            List<DailySales> last7Days,
            List<OrderSummary> recentOrders,
            List<LowStockItem> lowStock,
            List<BestSeller> bestSellers) {
    }
}
