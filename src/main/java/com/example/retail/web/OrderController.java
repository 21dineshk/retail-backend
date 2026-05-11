package com.example.retail.web;

import com.example.retail.service.OrderService;
import com.example.retail.web.dto.CheckoutRequest;
import com.example.retail.web.dto.OrderDto;
import com.example.retail.web.dto.PageResponse;
import com.example.retail.web.error.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/orders")
@Tag(name = "Orders", description = "Order placement and history per user. Payment is intentionally out of scope.")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    @Operation(summary = "Convert the user's cart into an order",
        description = "Creates a new CONFIRMED order from every item currently in the user's cart, "
            + "decrements stock, and clears the cart. No payment is processed.")
    public OrderDto checkout(@PathVariable Long userId, @Valid @RequestBody(required = false) CheckoutRequest body) {
        String addr = body == null ? null : body.shippingAddress();
        return OrderDto.of(orderService.checkout(userId, addr));
    }

    @GetMapping
    @Operation(summary = "List the user's orders, most recent first")
    public PageResponse<OrderDto> list(@PathVariable Long userId,
                                       @Parameter(description = "Zero-based page index")
                                       @RequestParam(defaultValue = "0") int page,
                                       @Parameter(description = "Page size (max 50)")
                                       @RequestParam(defaultValue = "10") int size) {
        if (size < 1 || size > 50) throw ApiException.badRequest("size must be 1..50");
        return PageResponse.from(orderService.listForUser(userId, page, size), OrderDto::of);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get a single order by id, scoped to the user")
    public OrderDto get(@PathVariable Long userId, @PathVariable Long orderId) {
        return OrderDto.of(orderService.getForUser(userId, orderId));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order",
        description = "Only PENDING or CONFIRMED orders can be cancelled. Stock is restored to inventory.")
    public OrderDto cancel(@PathVariable Long userId, @PathVariable Long orderId) {
        return OrderDto.of(orderService.cancel(userId, orderId));
    }
}
