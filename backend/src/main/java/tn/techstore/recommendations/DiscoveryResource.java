package tn.techstore.recommendations;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import tn.techstore.catalog.CatalogDtos.ProductDetail;
import tn.techstore.catalog.CatalogDtos.ProductSummary;
import tn.techstore.catalog.CatalogDtos.SearchSuggestions;
import tn.techstore.catalog.Product;
import tn.techstore.catalog.ProductService;
import tn.techstore.common.ApiException;
import tn.techstore.coupons.CouponDtos.CouponCheck;
import tn.techstore.coupons.CouponDtos.CouponQuote;
import tn.techstore.coupons.CouponService;
import tn.techstore.recommendations.RecommendationService.ProductRecommendations;
import tn.techstore.reviews.ReviewDtos.ReviewForm;
import tn.techstore.reviews.ReviewDtos.ReviewPage;
import tn.techstore.reviews.ReviewService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Recommandations, recherche instantanée, comparateur, avis et codes promo (API publique). */
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @Inject
    RecommendationService recommendations;

    @Inject
    ProductService products;

    @Inject
    ReviewService reviews;

    @Inject
    CouponService coupons;

    public record ViewRequest(String visitorId) {
    }

    /** Appelé à l'ouverture d'une fiche ; renvoie le nombre de visiteurs uniques sur 24 h. */
    @POST
    @Path("/products/{slug}/view")
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, Long> view(@PathParam("slug") String slug, ViewRequest body) {
        return Map.of("viewsLast24h", recommendations.recordView(slug, body == null ? null : body.visitorId()));
    }

    @GET
    @Path("/products/{slug}/recommendations")
    public ProductRecommendations forProduct(@PathParam("slug") String slug) {
        return recommendations.forProduct(slug);
    }

    @GET
    @Path("/products/{slug}/related")
    public List<ProductSummary> related(@PathParam("slug") String slug, @QueryParam("limit") @DefaultValue("4") int limit) {
        Product p = Product.bySlug(slug).orElseThrow(() -> ApiException.notFound("Produit"));
        return recommendations.similar(p, Math.clamp(limit, 1, 12));
    }

    @GET
    @Path("/products/trending")
    public List<ProductSummary> trending(@QueryParam("limit") @DefaultValue("8") int limit) {
        return recommendations.trending(Math.clamp(limit, 1, 24));
    }

    /** @param ids produits récemment consultés par le visiteur, ex. ids=6a1f…,6a20… */
    @GET
    @Path("/recommendations/for-you")
    public List<ProductSummary> forYou(@QueryParam("ids") String ids, @QueryParam("limit") @DefaultValue("8") int limit) {
        return recommendations.forYou(parseIds(ids), Math.clamp(limit, 1, 24));
    }

    @GET
    @Path("/products/compare")
    public List<ProductDetail> compare(@QueryParam("ids") String ids) {
        return products.compare(parseIds(ids));
    }

    @GET
    @Path("/search/suggest")
    public SearchSuggestions suggest(@QueryParam("q") String q) {
        return products.suggest(q);
    }

    @GET
    @Path("/products/{slug}/reviews")
    public ReviewPage reviews(@PathParam("slug") String slug,
                              @QueryParam("page") @DefaultValue("0") int page,
                              @QueryParam("size") @DefaultValue("5") int size) {
        return reviews.publicReviews(slug, page, size);
    }

    @POST
    @Path("/products/{slug}/reviews")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response submitReview(@PathParam("slug") String slug, @Valid ReviewForm form) {
        reviews.submit(slug, form);
        return Response.status(Response.Status.ACCEPTED)
                .entity(Map.of("message", "Merci ! Votre avis sera publié après vérification."))
                .build();
    }

    @POST
    @Path("/coupons/check")
    @Consumes(MediaType.APPLICATION_JSON)
    public CouponQuote checkCoupon(@Valid CouponCheck body) {
        return coupons.quote(body.code(), body.subtotal());
    }

    private static List<ObjectId> parseIds(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(ObjectId::isValid)
                .map(ObjectId::new)
                .distinct()
                .limit(20)
                .toList();
    }
}
