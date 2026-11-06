package tn.techstore.catalog;

import com.mongodb.client.MongoClient;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import tn.techstore.common.ApiException;
import tn.techstore.common.ShopConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;

/**
 * Stockage des images produits téléversées depuis l'admin ; MongoDB ne garde que l'URL dans products.images.
 * <ul>
 *   <li>{@code shop.storage=disk}  : fichiers dans {@code shop.upload-dir} (volume Docker sur un VPS)</li>
 *   <li>{@code shop.storage=mongo} : GridFS (bucket « uploads ») dans la base, pour les hébergeurs sans disque persistant</li>
 * </ul>
 */
@ApplicationScoped
public class StorageService {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif");
    private static final Pattern SAFE_NAME = Pattern.compile("^[a-f0-9-]{36}\\.(jpg|png|webp|gif)$");
    private static final long MAX_BYTES = 5 * 1024 * 1024;

    @Inject
    ShopConfig config;

    @Inject
    MongoClient mongo;

    @ConfigProperty(name = "quarkus.mongodb.database")
    String database;

    public String storeImage(Path tempFile, String contentType) {
        String ext = EXTENSIONS.get(contentType == null ? "" : contentType.toLowerCase());
        if (ext == null) {
            throw ApiException.badRequest("Format d'image non accepté (JPEG, PNG, WebP ou GIF)");
        }
        try {
            if (Files.size(tempFile) > MAX_BYTES) {
                throw ApiException.badRequest("Image trop lourde (5 Mo maximum)");
            }
            String name = UUID.randomUUID() + "." + ext;
            if (inMongo()) {
                try (InputStream in = Files.newInputStream(tempFile)) {
                    bucket().uploadFromStream(name, in,
                            new GridFSUploadOptions().metadata(new Document("contentType", contentType.toLowerCase())));
                }
            } else {
                Path dir = root();
                Files.createDirectories(dir);
                Files.copy(tempFile, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + name;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Contenu de l'image (5 Mo maximum, lu en mémoire), ou vide si le nom est invalide ou inconnu. */
    public Optional<byte[]> read(String name) {
        if (name == null || !SAFE_NAME.matcher(name).matches()) {
            return Optional.empty();
        }
        try {
            if (inMongo()) {
                GridFSFile file = bucket().find(eq("filename", name)).first();
                if (file == null) return Optional.empty();
                try (InputStream in = bucket().openDownloadStream(file.getObjectId())) {
                    return Optional.of(in.readAllBytes());
                }
            }
            Path file = root().resolve(name);
            return Files.isRegularFile(file) ? Optional.of(Files.readAllBytes(file)) : Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String contentTypeOf(String name) {
        String ext = name.substring(name.lastIndexOf('.') + 1);
        return EXTENSIONS.entrySet().stream()
                .filter(e -> e.getValue().equals(ext))
                .map(Map.Entry::getKey)
                .findFirst().orElse("application/octet-stream");
    }

    private boolean inMongo() {
        return "mongo".equalsIgnoreCase(config.storage());
    }

    private GridFSBucket bucket() {
        return GridFSBuckets.create(mongo.getDatabase(database), "uploads");
    }

    private Path root() {
        return Path.of(config.uploadDir()).toAbsolutePath().normalize();
    }
}
