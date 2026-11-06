package tn.techstore.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

public final class Slugs {

    private Slugs() {
    }

    /** "Écouteurs 6,7\" 256 Go" -> "ecouteurs-6-7-256-go" */
    public static String slugify(String input) {
        String s = Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (s.length() > 200) {
            s = s.substring(0, 200).replaceAll("-+$", "");
        }
        return s.isEmpty() ? "item" : s;
    }

    /** Ajoute -2, -3... tant que le slug est déjà pris. */
    public static String unique(String base, Predicate<String> taken) {
        String candidate = base;
        int i = 2;
        while (taken.test(candidate)) {
            candidate = base + "-" + i++;
        }
        return candidate;
    }
}
