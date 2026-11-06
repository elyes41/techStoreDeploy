package tn.techstore.catalog;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tn.techstore.catalog.CatalogDtos.*;
import tn.techstore.common.PageResult;
import tn.techstore.common.ShopConfig;

import java.math.BigDecimal;
import java.util.List;

/** API publique de la boutique (sans authentification). */
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class CatalogResource {

    @Inject
    ProductService products;

    @Inject
    CategoryService categories;

    @Inject
    ShopConfig config;

    @GET
    @Path("/categories")
    public List<CategoryDto> categories() {
        return categories.list(true);
    }

    /**
     * Exemple : /api/products?category=smartphones&brand=Samsung&brand=Xiaomi&minPrice=500&maxPrice=2000
     * &inStock=true&promo=true&minRating=4&q=5g&sort=price_asc&page=0&size=12
     */
    @GET
    @Path("/products")
    public PageResult<ProductSummary> search(
            @QueryParam("q") String q,
            @QueryParam("category") String category,
            @QueryParam("brand") List<String> brands,
            @QueryParam("minPrice") BigDecimal minPrice,
            @QueryParam("maxPrice") BigDecimal maxPrice,
            @QueryParam("inStock") boolean inStock,
            @QueryParam("promo") boolean promo,
            @QueryParam("featured") boolean featured,
            @QueryParam("minRating") Double minRating,
            @QueryParam("sort") String sort,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("12") int size) {
        return products.search(new ProductQuery(q, category, null, brands, minPrice, maxPrice,
                inStock, promo, featured, false, null, minRating, sort, page, size));
    }

    @GET
    @Path("/products/facets")
    public Facets facets(@QueryParam("category") String category) {
        return products.facets(category);
    }

    @GET
    @Path("/products/{slug}")
    public ProductDetail product(@PathParam("slug") String slug) {
        return products.getPublicBySlug(slug);
    }

    @GET
    @Path("/settings")
    public ShopSettings settings() {
        return new ShopSettings(config.shippingFee(), config.freeShippingThreshold(), config.fiscalStamp(),
                config.whatsappNumber().orElse(null));
    }
}
