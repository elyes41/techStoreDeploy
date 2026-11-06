package tn.techstore.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import tn.techstore.admin.AdminBootstrap;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Au démarrage : applique la structure de la base décrite dans resources/mongo
 * (validateurs $jsonSchema et index), puis crée le compte admin et les données de démo si besoin.
 * Toutes les opérations sont idempotentes : on peut redémarrer autant de fois que l'on veut.
 */
@ApplicationScoped
public class MongoSetup {

    @Inject
    MongoClient client;

    @ConfigProperty(name = "quarkus.mongodb.database")
    String databaseName;

    @Inject
    AdminBootstrap adminBootstrap;

    @Inject
    DemoData demoData;

    void onStart(@Observes StartupEvent event) {
        MongoDatabase db = client.getDatabase(databaseName);
        applyValidators(db);
        applyIndexes(db);
        adminBootstrap.ensureAdmin();
        demoData.seedIfEmpty();
    }

    private void applyValidators(MongoDatabase db) {
        Document schemas = Document.parse(Mongo.resource("mongo/validators.json"));
        Set<String> existing = db.listCollectionNames().into(new HashSet<>());
        for (String collection : schemas.keySet()) {
            Document validator = new Document("$jsonSchema", schemas.get(collection, Document.class));
            if (existing.contains(collection)) {
                db.runCommand(new Document("collMod", collection)
                        .append("validator", validator)
                        .append("validationLevel", "strict")
                        .append("validationAction", "error"));
            } else {
                db.createCollection(collection, new CreateCollectionOptions().validationOptions(new ValidationOptions()
                        .validator(validator)
                        .validationLevel(ValidationLevel.STRICT)
                        .validationAction(ValidationAction.ERROR)));
            }
        }
        Log.infof("MongoDB : validateurs appliqués sur %d collections", schemas.size());
    }

    private void applyIndexes(MongoDatabase db) {
        Document all = Document.parse(Mongo.resource("mongo/indexes.json"));
        int count = 0;
        for (String collection : all.keySet()) {
            for (Document index : all.getList(collection, Document.class)) {
                Document options = index.get("options", Document.class);
                IndexOptions opts = new IndexOptions();
                if (options != null) {
                    if (options.containsKey("name")) opts.name(options.getString("name"));
                    if (Boolean.TRUE.equals(options.getBoolean("unique"))) opts.unique(true);
                    if (options.containsKey("partialFilterExpression")) {
                        opts.partialFilterExpression(options.get("partialFilterExpression", Document.class));
                    }
                    if (options.containsKey("expireAfterSeconds")) {
                        opts.expireAfter(((Number) options.get("expireAfterSeconds")).longValue(), TimeUnit.SECONDS);
                    }
                }
                db.getCollection(collection).createIndex(index.get("keys", Document.class), opts);
                count++;
            }
        }
        Log.infof("MongoDB : %d index vérifiés", count);
    }
}
