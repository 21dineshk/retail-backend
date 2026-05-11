package com.example.retail.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "User shopping cart")
public record CartDto(
    Long id,
    Long userId,
    List<CartItemDto> items,
    @Schema(description = "Sum of (quantity * unit price) across all items, computed at read time") BigDecimal subtotal,
    Instant updatedAt
) {
    @Schema(description = "Single cart line")
    public record CartItemDto(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        Integer availableStock
    ) {}
}
