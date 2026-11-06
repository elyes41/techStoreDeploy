package tn.techstore.reviews;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public final class ReviewDtos {

    private ReviewDtos() {
    }

    public record ReviewForm(
            @NotBlank @Size(max = 80) String authorName,
            @Min(1) @Max(5) int rating,
            @Size(max = 120) String title,
            @Size(max = 2000) String comment,
            /* Référence d'une commande livrée : donne le badge « Achat vérifié ». */
            @Size(max = 40) String orderReference) {
    }

    public record ReviewDto(String id, String authorName, int rating, String title, String comment,
                            boolean verifiedPurchase, Instant createdAt) {
    }

    /** distribution[0] = nombre d'avis 5 étoiles … distribution[4] = 1 étoile. */
    public record ReviewSummary(double average, int count, List<Long> distribution) {
    }

    public record ReviewPage(ReviewSummary summary, List<ReviewDto> items, int page, int totalPages) {
    }

    public record AdminReviewDto(String id, String productId, String productName, String productSlug, String authorName,
                                 int rating, String title, String comment, Review.Status status,
                                 boolean verifiedPurchase, Instant createdAt) {
    }

    public record ReviewStatusUpdate(@NotNull Review.Status status) {
    }
}
