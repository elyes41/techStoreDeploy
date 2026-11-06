package tn.techstore.catalog;

import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.util.List;

/**
 * Critères de recherche du catalogue.
 *
 * @param sort popular (défaut) | price_asc | price_desc | newest | name | stock_asc | best_sellers | rating
 */
public record ProductQuery(
        String q,
        String categorySlug,
        ObjectId categoryId,
        List<String> brands,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean inStockOnly,
        boolean promoOnly,
        boolean featuredOnly,
        boolean includeInactive,
        Integer maxStock,
        Double minRating,
        String sort,
        int page,
        int size) {
}
