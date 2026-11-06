package tn.techstore.common;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import io.quarkus.logging.Log;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.Map;

public class ErrorMappers {

    @ServerExceptionMapper
    public Response mapApiException(ApiException e) {
        return json(e.status(), e.getMessage());
    }

    /** Index unique (slug, SKU, code promo…) ou validateur $jsonSchema de la collection. */
    @ServerExceptionMapper
    public Response mapMongoWrite(MongoWriteException e) {
        if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
            return json(409, "Cette valeur existe déjà (" + duplicateKey(e.getMessage()) + ")");
        }
        if (e.getError().getCode() == 121) {
            Log.warnf("Document refusé par le validateur MongoDB : %s", e.getMessage());
            return json(400, "Données invalides : le document ne respecte pas le schéma de la base");
        }
        Log.error("Erreur d'écriture MongoDB", e);
        return json(500, "Erreur interne, réessayez dans un instant");
    }

    private static String duplicateKey(String message) {
        int i = message == null ? -1 : message.indexOf("dup key:");
        return i < 0 ? "doublon" : message.substring(i + 8).trim();
    }

    private static Response json(int status, String message) {
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(Map.of("message", message)).build();
    }
}
