package com.example.retail.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Update an existing cart line's quantity")
public record CartItemQuantity(
    @NotNull @Min(1) @Schema(example = "3") Integer quantity
) {}
