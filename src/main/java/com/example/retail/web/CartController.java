package com.example.retail.web;

import com.example.retail.service.CartService;
import com.example.retail.web.dto.CartDto;
import com.example.retail.web.dto.CartItemQuantity;
import com.example.retail.web.dto.CartItemRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/cart")
@Tag(name = "Cart", description = "Per-user shopping cart. Carts auto-create on first read or write.")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Get the user's current cart",
        description = "Returns the cart and its line items with current product prices and stock. "
            + "Subtotal is computed at read time.")
    public CartDto get(@PathVariable Long userId) {
        return cartService.getOrCreate(userId);
    }

    @PostMapping("/items")
    @Operation(summary = "Add an item to the cart",
        description = "If the product is already in the cart, the quantity is added to the existing line.")
    public CartDto addItem(@PathVariable Long userId, @Valid @RequestBody CartItemRequest body) {
        return cartService.addItem(userId, body.productId(), body.quantity());
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Set the quantity of an existing cart line",
        description = "Replaces the line's quantity with the supplied value.")
    public CartDto updateItem(@PathVariable Long userId,
                              @PathVariable Long productId,
                              @Valid @RequestBody CartItemQuantity body) {
        return cartService.updateItem(userId, productId, body.quantity());
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove a line from the cart")
    public CartDto removeItem(@PathVariable Long userId, @PathVariable Long productId) {
        return cartService.removeItem(userId, productId);
    }

    @DeleteMapping
    @Operation(summary = "Empty the cart")
    public ResponseEntity<Void> clear(@PathVariable Long userId) {
        cartService.clear(userId);
        return ResponseEntity.noContent().build();
    }
}
