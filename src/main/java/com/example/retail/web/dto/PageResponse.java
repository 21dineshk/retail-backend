package com.example.retail.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Schema(description = "Paginated list response")
public record PageResponse<T>(
    List<T> items,
    @Schema(example = "0") int page,
    @Schema(example = "20") int size,
    @Schema(example = "1") int totalPages,
    @Schema(example = "10000") long totalItems
) {
    public static <S, T> PageResponse<T> from(Page<S> p, Function<S, T> mapper) {
        return new PageResponse<>(p.map(mapper).getContent(),
            p.getNumber(), p.getSize(), p.getTotalPages(), p.getTotalElements());
    }
}
