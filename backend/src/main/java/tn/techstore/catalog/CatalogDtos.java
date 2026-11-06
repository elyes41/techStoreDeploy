package tn.techstore.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static tn.techstore.common.Ids.str;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CategoryDto(String id, String name, String slug, long productCount) {
    }

    public record ProductSummary(
            String id, String name, String slug, String sku, String brand,
            BigDecimal price, BigDecimal oldPrice, int stock, boolean active, boolean featured,
            double ratingAvg, int ratingCount, int soldCount, String image,
            String categoryId, String categoryName, String categorySlug) {

        public static ProductSummary of(Product p) {
            var c = p.category;
            return new ProductSummary(str(p.id), p.name, p.slug, p.sku, p.brand, p.price, p.oldPrice, p.stock,
                    p.active, p.featured, p.rating == null ? 0 : p.rating.average, p.rating == null ? 0 : p.rating.count,
                    p.soldCount, p.images.isEmpty() ? null : p.images.get(0),
                    c == null ? null : str(c.id), c == null ? null : c.name, c == null ? null : c.slug);
        }
    }

    public record ProductDetail(
            String id, String name, String slug, String sku, String brand, String description,
            BigDecimal price, BigDecimal oldPrice, int stock, boolean active, boolean featured,
            double ratingAvg, int ratingCount, int soldCount,
            List<String> images, Map<String, String> specs,
            String categoryId, String categoryName, String categorySlug,
            Instant createdAt, Instant updatedAt) {

        public static ProductDetail of(Product p) {
            var c = p.category;
            Map<String, String> specs = new LinkedHashMap<>();
            p.specs.forEach(s -> specs.put(s.name, s.value));
            return new ProductDetail(str(p.id), p.name, p.slug, p.sku, p.brand, p.description, p.price, p.oldPrice,
                    p.stock, p.active, p.featured, p.rating == null ? 0 : p.rating.average,
                    p.rating == null ? 0 : p.rating.count, p.soldCount, List.copyOf(p.images), specs,
                    c == null ? null : str(c.id), c == null ? null : c.name, c == null ? null : c.slug,
                    p.createdAt, p.updatedAt);
        }
    }

    /** Valeurs disponibles pour construire les filtres du catalogue. */
    public record Facets(List<String> brands, BigDecimal minPrice, BigDecimal maxPrice) {
    }

    public record ShopSettings(BigDecimal shippingFee, BigDecimal freeShippingThreshold, BigDecimal fiscalStamp,
                               String whatsappNumber) {
    }

    public record SearchSuggestions(List<ProductSummary> products, List<CategoryDto> categories, List<String> brands) {
    }

    public record ProductForm(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 64) String sku,
            @Size(max = 100) String brand,
            String description,
            @NotNull @DecimalMin("0") BigDecimal price,
            @DecimalMin("0") BigDecimal oldPrice,
            @Min(0) int stock,
            boolean active,
            boolean featured,
            String categoryId,
            @Size(max = 10) List<@NotBlank @Size(max = 500) String> images,
            Map<String, String> specs) {
    }

    public record CategoryForm(@NotBlank @Size(max = 120) String name) {
    }

    public record StockUpdate(@Min(0) int stock) {
    }
}
