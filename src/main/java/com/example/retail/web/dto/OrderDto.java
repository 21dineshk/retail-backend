package com.example.retail.web.dto;

import com.example.retail.domain.Order;
import com.example.retail.domain.OrderItem;
import com.example.retail.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "A placed order")
public record OrderDto(
    Long id,
    Long userId,
    Instant placedAt,
    OrderStatus status,
    BigDecimal totalAmount,
    String shippingAddress,
    List<OrderItemDto> items
) {
    @Schema(description = "Snapshotted line on a placed order")
    public record OrderItemDto(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
    ) {
        public static OrderItemDto of(OrderItem oi) {
            return new OrderItemDto(oi.getProductId(), oi.getProductName(), oi.getQuantity(),
                oi.getUnitPrice(), oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity())));
        }
    }

    public static OrderDto of(Order o) {
        return new OrderDto(o.getId(), o.getUserId(), o.getPlacedAt(), o.getStatus(),
            o.getTotalAmount(), o.getShippingAddress(),
            o.getItems().stream().map(OrderItemDto::of).toList());
    }
}
