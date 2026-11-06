package tn.techstore.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import tn.techstore.common.ShopConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Envoi d'un e-mail transactionnel.
 * Par défaut : SMTP (Quarkus Mailer). Si {@code shop.brevo-api-key} est défini : API HTTP Brevo,
 * pour les hébergeurs qui bloquent les ports SMTP (Render Free, certains PaaS).
 */
@ApplicationScoped
public class MailGateway {

    private static final URI BREVO = URI.create("https://api.brevo.com/v3/smtp/email");
    private static final Pattern NAMED = Pattern.compile("^\\s*(.*?)\\s*<([^>]+)>\\s*$");

    @Inject
    Mailer mailer;

    @Inject
    ShopConfig config;

    @ConfigProperty(name = "quarkus.mailer.from")
    String from;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = new ObjectMapper();

    public void send(String to, String subject, String html, String text) {
        var apiKey = config.brevoApiKey().filter(k -> !k.isBlank());
        if (apiKey.isEmpty()) {
            mailer.send(Mail.withHtml(to, subject, html).setText(text));
            return;
        }
        Matcher m = NAMED.matcher(from);
        Map<String, String> sender = m.matches() ? Map.of("name", m.group(1), "email", m.group(2)) : Map.of("email", from.trim());
        try {
            String body = json.writeValueAsString(Map.of(
                    "sender", sender,
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", html,
                    "textContent", text));
            HttpRequest req = HttpRequest.newBuilder(BREVO)
                    .timeout(Duration.ofSeconds(20))
                    .header("api-key", apiKey.get())
                    .header("content-type", "application/json")
                    .header("accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                throw new IllegalStateException("Brevo HTTP " + res.statusCode() + " : " + res.body());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Brevo injoignable : " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envoi interrompu", e);
        }
    }
}
