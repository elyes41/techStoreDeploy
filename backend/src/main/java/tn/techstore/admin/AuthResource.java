package tn.techstore.admin;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tn.techstore.admin.AdminDtos.LoginRequest;
import tn.techstore.admin.AdminDtos.LoginResponse;
import tn.techstore.common.ApiException;
import tn.techstore.common.ShopConfig;

import java.time.Instant;
import java.util.Set;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    ShopConfig config;

    @POST
    @Path("/login")
    public LoginResponse login(@Valid LoginRequest req) {
        AdminUser user = AdminUser.byEmail(req.email())
                .filter(u -> BcryptUtil.matches(req.password(), u.passwordHash))
                .orElseThrow(() -> new ApiException(401, "E-mail ou mot de passe incorrect"));

        Instant expiresAt = Instant.now().plus(config.tokenTtl());
        String token = Jwt.issuer(config.issuer())
                .upn(user.email)
                .subject(user.id.toHexString())
                .groups(Set.of("admin"))
                .claim("name", user.fullName == null ? user.email : user.fullName)
                .expiresAt(expiresAt)
                .sign();
        return new LoginResponse(token, user.email, user.fullName, expiresAt);
    }
}
