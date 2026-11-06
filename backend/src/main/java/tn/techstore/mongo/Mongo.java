package tn.techstore.mongo;

import org.bson.types.Decimal128;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.regex.Pattern;

/** Petits utilitaires autour du driver MongoDB. */
public final class Mongo {

    private Mongo() {
    }

    /** Valeur numérique lue dans un résultat d'agrégation (Decimal128, Integer, Double…). */
    public static BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof Decimal128 d) return d.bigDecimalValue();
        if (value instanceof BigDecimal b) return b;
        if (value instanceof Number n) return new BigDecimal(n.toString());
        throw new IllegalArgumentException("Valeur non numérique : " + value);
    }

    public static long count(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    /** Convertit les paramètres Java en types BSON explicites (montants en Decimal128, dates en Date). */
    public static Object toBson(Object value) {
        if (value instanceof BigDecimal b) return new Decimal128(b);
        if (value instanceof Instant i) return Date.from(i);
        if (value instanceof Collection<?> c) return c.stream().map(Mongo::toBson).toList();
        return value;
    }

    /** Recherche « contient », insensible à la casse, sans interpréter les caractères spéciaux. */
    public static Pattern containsIgnoreCase(String text) {
        return Pattern.compile(Pattern.quote(text.trim()), Pattern.CASE_INSENSITIVE);
    }

    static String resource(String path) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Ressource introuvable : " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
