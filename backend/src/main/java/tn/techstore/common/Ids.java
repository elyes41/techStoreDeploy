package tn.techstore.common;

import org.bson.types.ObjectId;

/** Conversion entre les identifiants MongoDB (ObjectId) et les chaînes exposées par l'API. */
public final class Ids {

    private Ids() {
    }

    /** Identifiant présent dans l'URL : un format invalide est traité comme « introuvable ». */
    public static ObjectId parse(String id, String what) {
        if (id == null || !ObjectId.isValid(id)) {
            throw ApiException.notFound(what);
        }
        return new ObjectId(id);
    }

    /** Identifiant envoyé dans un corps de requête : null autorisé, format invalide = 400. */
    public static ObjectId parseOptional(String id, String field) {
        if (id == null || id.isBlank()) {
            return null;
        }
        if (!ObjectId.isValid(id)) {
            throw ApiException.badRequest("Identifiant invalide pour " + field);
        }
        return new ObjectId(id);
    }

    public static String str(ObjectId id) {
        return id == null ? null : id.toHexString();
    }
}
