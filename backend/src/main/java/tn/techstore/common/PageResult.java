package tn.techstore.common;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int size, long total, int totalPages) {

    public static <T> PageResult<T> of(List<T> items, int page, int size, long total) {
        return new PageResult<>(items, page, size, total, (int) Math.ceil(total / (double) size));
    }
}
