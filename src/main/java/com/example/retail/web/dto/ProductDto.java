package com.example.retail.web.dto;

import com.example.retail.domain.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Product as returned by the catalog endpoints")
public record ProductDto(
    @Schema(example = "42") Long id,
    @Schema(example = "SKU-00042") String sku,
    @Schema(example = "AeroCo Wireless Headphones") String name,
    String description,
    @Schema(example = "Electronics") String category,
    @Schema(example = "129.99") BigDecimal price,
    @Schema(example = "57", description = "Units currently in stock") Integer stock,
    @Schema(example = "4.6") Double rating,
    String imageUrl
) {
    public static ProductDto of(Product p) {
        return new ProductDto(p.getId(), p.getSku(), p.getName(), p.getDescription(),
            p.getCategory(), p.getPrice(), p.getStock(), p.getRating(), p.getImageUrl());
    }
}
