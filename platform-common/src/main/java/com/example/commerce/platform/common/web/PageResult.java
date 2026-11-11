package com.example.commerce.platform.common.web;

import java.util.List;

public record PageResult<T>(
        List<T> content,
        PageMetadata page
) {

    public static <T> PageResult<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResult<>(content, new PageMetadata(size, page, totalElements, totalPages));
    }

    public record PageMetadata(
            int size,
            int number,
            long totalElements,
            int totalPages
    ) {
    }
}
