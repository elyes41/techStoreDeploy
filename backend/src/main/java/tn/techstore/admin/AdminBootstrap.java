package tn.techstore.admin;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tn.techstore.common.ShopConfig;

import java.time.Instant;

/** Crée le premier compte administrateur si la collection admin_users est vide. */
@ApplicationScoped
public class AdminBootstrap {

    @Inject
    ShopConfig config;

    public void ensureAdmin() {
        if (AdminUser.col().countDocuments() > 0) {
            return;
        }
        AdminUser admin = new AdminUser();
        admin.email = config.admin().email().trim().toLowerCase();
        admin.fullName = config.admin().fullName();
        admin.passwordHash = BcryptUtil.bcryptHash(config.admin().password());
        admin.createdAt = Instant.now();
        admin.persist();
        Log.infof("Compte administrateur créé : %s", admin.email);
        if ("ChangeMoi123!".equals(config.admin().password())) {
            Log.warn("Mot de passe admin par défaut utilisé : définissez ADMIN_PASSWORD avant la mise en production.");
        }
    }
}
