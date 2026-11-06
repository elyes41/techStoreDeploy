package tn.techstore.catalog;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

/** Sert les images produits téléversées depuis le dashboard (URL stockée dans products.images). */
@Path("/uploads")
public class UploadsResource {

    @Inject
    StorageService storage;

    @GET
    @Path("/{name}")
    public Response get(@PathParam("name") String name) {
        byte[] content = storage.read(name).orElseThrow(NotFoundException::new);
        return Response.ok(content, StorageService.contentTypeOf(name))
                .header("Cache-Control", "public, max-age=31536000, immutable")
                .build();
    }
}
