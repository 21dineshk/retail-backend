package com.example.retail.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Convert the user's cart into an order. Payment is out of scope.")
public record CheckoutRequest(
    @Size(max = 400)
    @Schema(description = "Shipping address. Falls back to the user's profile address if blank.",
            example = "742 Evergreen Terrace, Springfield, OR 97477")
    String shippingAddress
) {}
