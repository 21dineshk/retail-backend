package com.example.retail.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Add or update a cart line")
public record CartItemRequest(
    @NotNull @Schema(example = "42") Long productId,
    @NotNull @Min(1) @Schema(example = "2") Integer quantity
) {}
