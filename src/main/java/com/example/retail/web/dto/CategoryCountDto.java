package com.example.retail.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Category with the number of products in it")
public record CategoryCountDto(
    @Schema(example = "Electronics") String category,
    @Schema(example = "667") long count
) {}
