package tn.techstore.catalog;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import tn.techstore.catalog.CatalogDtos.*;
import tn.techstore.common.ApiException;
import tn.techstore.common.Ids;
import tn.techstore.common.PageResult;
import tn.techstore.common.ShopConfig;

import java.util.List;
import java.util.Map;

/** Gestion du catalogue depuis le dashboard : produits, catégories, photos. */
@Path("/api/admin")
@RolesAllowed("admin")
@Produces(MediaType.APPLICATION_JSON)
public class AdminCatalogResource {

    @Inject
    ProductService products;

    @Inject
    CategoryService categories;

    @Inject
    StorageService storage;

    @Inject
    ShopConfig config;

    // ---------- Produits ----------

    @GET
    @Path("/products")
    public PageResult<ProductSummary> list(
            @QueryParam("q") String q,
            @QueryParam("categoryId") String categoryId,
            @QueryParam("status") String status,
            @QueryParam("sort") @DefaultValue("newest") String sort,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        // status : all (défaut) | low_stock | promo
        boolean lowStock = "low_stock".equals(status);
        return products.search(new ProductQuery(q, null, Ids.parseOptional(categoryId, "la catégorie"), null, null, null,
                false, "promo".equals(status), false, true,
                lowStock ? config.lowStockThreshold() : null, null,
                lowStock ? "stock_asc" : sort, page, size));
    }

    @GET
    @Path("/products/{id}")
    public ProductDetail get(@PathParam("id") String id) {
        return products.getById(id);
    }

    @POST
    @Path("/products")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid ProductForm form) {
        return Response.status(Response.Status.CREATED).entity(products.create(form)).build();
    }

    @PUT
    @Path("/products/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ProductDetail update(@PathParam("id") String id, @Valid ProductForm form) {
        return products.update(id, form);
    }

    @PATCH
    @Path("/products/{id}/stock")
    @Consumes(MediaType.APPLICATION_JSON)
    public ProductDetail updateStock(@PathParam("id") String id, @Valid StockUpdate body) {
        return products.updateStock(id, body.stock());
    }

    @DELETE
    @Path("/products/{id}")
    public Response delete(@PathParam("id") String id) {
        products.delete(id);
        return Response.noContent().build();
    }

    /** Téléverse une image ; renvoie {"url": "/uploads/..."} à ajouter dans images[] du produit. */
    @POST
    @Path("/uploads")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Map<String, String> upload(@RestForm("file") FileUpload file) {
        if (file == null) {
            throw ApiException.badRequest("Aucun fichier reçu (champ « file »)");
        }
        return Map.of("url", storage.storeImage(file.uploadedFile(), file.contentType()));
    }

    // ---------- Catégories ----------

    @GET
    @Path("/categories")
    public List<CategoryDto> categories() {
        return categories.list(false);
    }

    @POST
    @Path("/categories")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createCategory(@Valid CategoryForm form) {
        return Response.status(Response.Status.CREATED).entity(categories.create(form.name())).build();
    }

    @PUT
    @Path("/categories/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CategoryDto renameCategory(@PathParam("id") String id, @Valid CategoryForm form) {
        return categories.rename(id, form.name());
    }

    @DELETE
    @Path("/categories/{id}")
    public Response deleteCategory(@PathParam("id") String id) {
        categories.delete(id);
        return Response.noContent().build();
    }
}
