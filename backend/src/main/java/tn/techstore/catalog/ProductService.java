package tn.techstore.catalog;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import tn.techstore.catalog.CatalogDtos.*;
import tn.techstore.common.ApiException;
import tn.techstore.common.Ids;
import tn.techstore.common.PageResult;
import tn.techstore.common.Slugs;
import tn.techstore.mongo.Mongo;
import tn.techstore.mongo.Pipelines;
import tn.techstore.orders.Order;
import tn.techstore.recommendations.ProductView;
import tn.techstore.reviews.Review;

import java.time.Instant;
import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

@ApplicationScoped
public class ProductService {

    private static final int MAX_PAGE_SIZE = 60;

    public PageResult<ProductSummary> search(ProductQuery f) {
        Bson filter = filter(f);
        int size = Math.clamp(f.size() <= 0 ? 12 : f.size(), 1, MAX_PAGE_SIZE);
        int page = Math.max(0, f.page());
        List<ProductSummary> items = Product.col().find(filter).sort(sortOf(f.sort()))
                .skip(page * size).limit(size)
                .map(ProductSummary::of).into(new ArrayList<>());
        return PageResult.of(items, page, size, Product.col().countDocuments(filter));
    }

    static Bson filter(ProductQuery f) {
        List<Bson> conditions = new ArrayList<>();
        if (!f.includeInactive()) conditions.add(eq("active", true));
        if (notBlank(f.categorySlug())) conditions.add(eq("category.slug", f.categorySlug().trim()));
        if (f.categoryId() != null) conditions.add(eq("category._id", f.categoryId()));
        List<String> brands = f.brands() == null ? List.of()
                : f.brands().stream().filter(ProductService::notBlank).map(String::trim).toList();
        if (!brands.isEmpty()) conditions.add(in("brand", brands));
        if (f.minPrice() != null) conditions.add(gte("price", new Decimal128(f.minPrice())));
        if (f.maxPrice() != null) conditions.add(lte("price", new Decimal128(f.maxPrice())));
        if (f.inStockOnly()) conditions.add(gt("stock", 0));
        if (f.maxStock() != null) conditions.add(lte("stock", f.maxStock()));
        if (f.promoOnly()) {
            conditions.add(ne("oldPrice", null));
            conditions.add(expr(new Document("$gt", List.of("$oldPrice", "$price"))));
        }
        if (f.minRating() != null) {
            conditions.add(gt("rating.count", 0));
            conditions.add(gte("rating.average", f.minRating()));
        }
        if (f.featuredOnly()) conditions.add(eq("featured", true));
        if (notBlank(f.q())) {
            var rx = Mongo.containsIgnoreCase(f.q());
            conditions.add(or(regex("name", rx), regex("brand", rx), regex("sku", rx)));
        }
        return conditions.isEmpty() ? new Document() : and(conditions);
    }

    public Facets facets(String categorySlug) {
        Document match = new Document("active", true);
        if (notBlank(categorySlug)) match.append("category.slug", categorySlug.trim());
        List<String> brands = Product.col().distinct("brand", match, String.class).into(new ArrayList<>());
        brands.removeIf(Objects::isNull);
        brands.sort(String.CASE_INSENSITIVE_ORDER);
        Document range = Product.col().withDocumentClass(Document.class)
                .aggregate(Pipelines.of("price-range", Map.of("match", match))).first();
        return range == null ? new Facets(brands, null, null)
                : new Facets(brands, Mongo.decimal(range.get("min")), Mongo.decimal(range.get("max")));
    }

    public ProductDetail getPublicBySlug(String slug) {
        return Product.bySlug(slug).filter(p -> p.active).map(ProductDetail::of)
                .orElseThrow(() -> ApiException.notFound("Produit"));
    }

    public ProductDetail getById(String id) {
        return ProductDetail.of(find(Ids.parse(id, "Produit")));
    }

    /** Recherche instantanée : produits, rayons et marques correspondant au texte saisi. */
    public SearchSuggestions suggest(String q) {
        if (!notBlank(q) || q.trim().length() < 2) {
            return new SearchSuggestions(List.of(), List.of(), List.of());
        }
        var rx = Mongo.containsIgnoreCase(q);
        List<ProductSummary> products = Product.col()
                .find(and(eq("active", true), or(regex("name", rx), regex("brand", rx), regex("sku", rx))))
                .sort(Sorts.orderBy(Sorts.descending("soldCount", "featured"), Sorts.ascending("_id")))
                .limit(6).map(ProductSummary::of).into(new ArrayList<>());
        List<CategoryDto> categories = Category.col().find(regex("name", rx)).sort(Sorts.ascending("name")).limit(3)
                .map(c -> new CategoryDto(Ids.str(c.id), c.name, c.slug, 0)).into(new ArrayList<>());
        List<String> brands = Product.col().distinct("brand", and(eq("active", true), regex("brand", rx)), String.class)
                .into(new ArrayList<>());
        brands.sort(String.CASE_INSENSITIVE_ORDER);
        return new SearchSuggestions(products, categories, brands.subList(0, Math.min(4, brands.size())));
    }

    /** Fiches complètes pour le comparateur (4 produits maximum, dans l'ordre demandé). */
    public List<ProductDetail> compare(List<ObjectId> ids) {
        List<ObjectId> wanted = ids.stream().distinct().limit(4).toList();
        if (wanted.isEmpty()) return List.of();
        Map<ObjectId, Product> byId = new HashMap<>();
        Product.col().find(and(eq("active", true), in("_id", wanted))).forEach(p -> byId.put(p.id, p));
        return wanted.stream().map(byId::get).filter(Objects::nonNull).map(ProductDetail::of).toList();
    }

    // ---------- Administration ----------

    public ProductDetail create(ProductForm form) {
        Product p = new Product();
        apply(p, form);
        p.slug = Slugs.unique(Slugs.slugify(form.name()), s -> Product.col().countDocuments(eq("slug", s)) > 0);
        p.soldCount = 0;
        p.rating = new Product.Rating();
        p.createdAt = p.updatedAt = Instant.now();
        p.persist();
        return ProductDetail.of(p);
    }

    public ProductDetail update(String id, ProductForm form) {
        Product p = find(Ids.parse(id, "Produit"));
        apply(p, form);
        p.updatedAt = Instant.now();
        p.update();
        return ProductDetail.of(p);
    }

    public ProductDetail updateStock(String id, int stock) {
        ObjectId oid = Ids.parse(id, "Produit");
        Product p = Product.col().findOneAndUpdate(eq("_id", oid),
                combine(set("stock", stock), set("updatedAt", new Date())),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
        if (p == null) throw ApiException.notFound("Produit");
        return ProductDetail.of(p);
    }

    /**
     * Supprime le produit, ses avis et ses consultations. Les commandes passées gardent le nom et le prix
     * de l'article ; leur lien vers le produit est simplement retiré.
     */
    public void delete(String id) {
        ObjectId oid = Ids.parse(id, "Produit");
        if (Product.col().deleteOne(eq("_id", oid)).getDeletedCount() == 0) {
            throw ApiException.notFound("Produit");
        }
        Review.col().deleteMany(eq("productId", oid));
        ProductView.col().deleteMany(eq("productId", oid));
        Order.col().updateMany(eq("items.productId", oid), set("items.$[line].productId", null),
                new UpdateOptions().arrayFilters(List.of(Filters.eq("line.productId", oid))));
    }

    private Product find(ObjectId id) {
        Product p = Product.findById(id);
        if (p == null) throw ApiException.notFound("Produit");
        return p;
    }

    private void apply(Product p, ProductForm form) {
        String sku = blankToNull(form.sku());
        if (sku != null) {
            Bson sameSku = p.id == null ? eq("sku", sku) : and(eq("sku", sku), ne("_id", p.id));
            if (Product.col().countDocuments(sameSku) > 0) {
                throw ApiException.conflict("La référence (SKU) " + sku + " existe déjà");
            }
        }
        if (form.oldPrice() != null && form.oldPrice().compareTo(form.price()) <= 0) {
            throw ApiException.badRequest("L'ancien prix doit être supérieur au prix de vente");
        }
        p.name = form.name().trim();
        p.sku = sku;
        p.brand = blankToNull(form.brand());
        p.description = blankToNull(form.description());
        p.price = form.price();
        p.oldPrice = form.oldPrice();
        p.stock = form.stock();
        p.active = form.active();
        p.featured = form.featured();

        ObjectId categoryId = Ids.parseOptional(form.categoryId(), "la catégorie");
        if (categoryId == null) {
            p.category = null;
        } else {
            Category c = Category.findById(categoryId);
            if (c == null) throw ApiException.badRequest("Catégorie inconnue");
            p.category = new Product.CategoryRef(c);
        }

        p.images = new ArrayList<>();
        if (form.images() != null) {
            form.images().stream().filter(ProductService::notBlank).map(String::trim).forEach(p.images::add);
        }
        p.specs = new ArrayList<>();
        if (form.specs() != null) {
            new TreeMap<>(form.specs()).forEach((k, v) -> {
                if (notBlank(k) && notBlank(v)) p.specs.add(new Product.Spec(k.trim(), v.trim()));
            });
        }
    }

    static Bson sortOf(String sort) {
        return switch (Objects.requireNonNullElse(sort, "popular")) {
            case "price_asc" -> Sorts.ascending("price", "_id");
            case "price_desc" -> Sorts.orderBy(Sorts.descending("price"), Sorts.ascending("_id"));
            case "newest" -> Sorts.descending("createdAt", "_id");
            case "name" -> Sorts.ascending("name", "_id");
            case "stock_asc" -> Sorts.ascending("stock", "_id");
            case "best_sellers" -> Sorts.orderBy(Sorts.descending("soldCount"), Sorts.ascending("_id"));
            case "rating" -> Sorts.orderBy(Sorts.descending("rating.average", "rating.count"), Sorts.ascending("_id"));
            default -> Sorts.descending("featured", "createdAt", "_id");
        };
    }

    static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String blankToNull(String s) {
        return notBlank(s) ? s.trim() : null;
    }
}
