package tn.techstore.mongo;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pipelines d'agrégation stockés en JSON dans {@code resources/mongo/pipelines}.
 * <p>
 * Un paramètre s'écrit {@code {"@param": "nom"}} dans le fichier et est remplacé par sa valeur à l'exécution.
 * Les mêmes fichiers peuvent ainsi être relus, testés dans mongosh ou Compass, et versionnés comme du schéma.
 */
public final class Pipelines {

    private static final String PARAM = "@param";
    private static final Map<String, List<Document>> CACHE = new ConcurrentHashMap<>();

    private Pipelines() {
    }

    public static List<Bson> of(String name, Map<String, ?> params) {
        List<Document> stages = CACHE.computeIfAbsent(name, Pipelines::read);
        List<Bson> result = new ArrayList<>(stages.size());
        for (Document stage : stages) {
            result.add((Document) substitute(stage, params, name));
        }
        return result;
    }

    private static List<Document> read(String name) {
        String json = Mongo.resource("mongo/pipelines/" + name + ".json");
        return Document.parse("{\"stages\": " + json + "}").getList("stages", Document.class);
    }

    private static Object substitute(Object node, Map<String, ?> params, String pipeline) {
        if (node instanceof Document d) {
            if (d.size() == 1 && d.containsKey(PARAM)) {
                String key = d.getString(PARAM);
                if (!params.containsKey(key)) {
                    throw new IllegalArgumentException("Paramètre « " + key + " » manquant pour le pipeline " + pipeline);
                }
                return Mongo.toBson(params.get(key));
            }
            Document copy = new Document();
            d.forEach((k, v) -> copy.append(k, substitute(v, params, pipeline)));
            return copy;
        }
        if (node instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object v : list) {
                copy.add(substitute(v, params, pipeline));
            }
            return copy;
        }
        return node;
    }
}
