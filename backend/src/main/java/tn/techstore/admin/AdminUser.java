package tn.techstore.admin;

import com.mongodb.client.MongoCollection;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

import java.time.Instant;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

/** Collection « admin_users ». Mot de passe haché en bcrypt, e-mail unique en minuscules. */
@MongoEntity(collection = "admin_users")
public class AdminUser extends PanacheMongoEntity {

    public String email;
    public String passwordHash;
    public String fullName;
    public Instant createdAt;

    public static MongoCollection<AdminUser> col() {
        return mongoCollection();
    }

    public static Optional<AdminUser> byEmail(String email) {
        return Optional.ofNullable(col().find(eq("email", email.trim().toLowerCase())).first());
    }
}
