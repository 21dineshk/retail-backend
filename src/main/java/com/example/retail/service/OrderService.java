package com.example.retail.service;

import com.example.retail.domain.*;
import com.example.retail.repository.CartRepository;
import com.example.retail.repository.OrderRepository;
import com.example.retail.repository.ProductRepository;
import com.example.retail.repository.UserRepository;
import com.example.retail.web.error.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final CartRepository cartRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    public OrderService(OrderRepository orderRepo, CartRepository cartRepo,
                        ProductRepository productRepo, UserRepository userRepo) {
        this.orderRepo = orderRepo;
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public Order checkout(Long userId, String overrideAddress) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> ApiException.notFound("User " + userId));
        Cart cart = cartRepo.findByUserId(userId)
            .orElseThrow(() -> ApiException.badRequest("Cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw ApiException.badRequest("Cart is empty");
        }

        List<Long> productIds = cart.getItems().stream().map(CartItem::getProductId).toList();
        Map<Long, Product> byId = new HashMap<>();
        for (Product p : productRepo.findAllById(productIds)) {
            byId.put(p.getId(), p);
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setPlacedAt(Instant.now());
        order.setStatus(OrderStatus.CONFIRMED);
        String address = (overrideAddress == null || overrideAddress.isBlank())
            ? user.getShippingAddress() : overrideAddress;
        if (address == null || address.isBlank()) {
            throw ApiException.badRequest("Shipping address required (none on profile, none in request)");
        }
        order.setShippingAddress(address);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem ci : cart.getItems()) {
            Product p = byId.get(ci.getProductId());
            if (p == null) {
                throw ApiException.conflict("Product " + ci.getProductId() + " is no longer available");
            }
            if (p.getStock() < ci.getQuantity()) {
                throw ApiException.conflict("Insufficient stock for " + p.getSku()
                    + " (requested " + ci.getQuantity() + ", available " + p.getStock() + ")");
            }
            p.setStock(p.getStock() - ci.getQuantity());

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProductId(p.getId());
            oi.setProductName(p.getName());
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(p.getPrice());
            order.getItems().add(oi);
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }
        order.setTotalAmount(total);

        Order saved = orderRepo.save(order);
        cart.getItems().clear();
        cart.setUpdatedAt(Instant.now());
        cartRepo.save(cart);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Order> listForUser(Long userId, int page, int size) {
        if (!userRepo.existsById(userId)) {
            throw ApiException.notFound("User " + userId);
        }
        return orderRepo.findByUserIdOrderByPlacedAtDesc(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Order getForUser(Long userId, Long orderId) {
        return orderRepo.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> ApiException.notFound("Order " + orderId + " for user " + userId));
    }

    @Transactional
    public Order cancel(Long userId, Long orderId) {
        Order order = getForUser(userId, orderId);
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw ApiException.conflict("Cannot cancel an order that has shipped");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }
        order.setStatus(OrderStatus.CANCELLED);
        for (OrderItem oi : order.getItems()) {
            productRepo.findById(oi.getProductId()).ifPresent(p -> p.setStock(p.getStock() + oi.getQuantity()));
        }
        return orderRepo.save(order);
    }
}
