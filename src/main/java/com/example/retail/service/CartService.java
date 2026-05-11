package com.example.retail.service;

import com.example.retail.domain.Cart;
import com.example.retail.domain.CartItem;
import com.example.retail.domain.Product;
import com.example.retail.repository.CartRepository;
import com.example.retail.repository.ProductRepository;
import com.example.retail.repository.UserRepository;
import com.example.retail.web.dto.CartDto;
import com.example.retail.web.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private final CartRepository cartRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;

    public CartService(CartRepository cartRepo, UserRepository userRepo, ProductRepository productRepo) {
        this.cartRepo = cartRepo;
        this.userRepo = userRepo;
        this.productRepo = productRepo;
    }

    @Transactional
    public CartDto getOrCreate(Long userId) {
        return toDto(loadOrCreate(userId));
    }

    @Transactional
    public CartDto addItem(Long userId, Long productId, int quantity) {
        Cart cart = loadOrCreate(userId);
        Product product = productRepo.findById(productId)
            .orElseThrow(() -> ApiException.notFound("Product " + productId));
        if (product.getStock() < quantity) {
            throw ApiException.conflict("Insufficient stock for product " + productId
                + " (requested " + quantity + ", available " + product.getStock() + ")");
        }
        CartItem existing = findItem(cart, productId);
        if (existing == null) {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProductId(productId);
            item.setQuantity(quantity);
            cart.getItems().add(item);
        } else {
            int newQty = existing.getQuantity() + quantity;
            if (product.getStock() < newQty) {
                throw ApiException.conflict("Insufficient stock for product " + productId
                    + " (requested total " + newQty + ", available " + product.getStock() + ")");
            }
            existing.setQuantity(newQty);
        }
        cart.setUpdatedAt(Instant.now());
        return toDto(cartRepo.save(cart));
    }

    @Transactional
    public CartDto updateItem(Long userId, Long productId, int quantity) {
        Cart cart = loadOrCreate(userId);
        CartItem existing = findItem(cart, productId);
        if (existing == null) {
            throw ApiException.notFound("Cart item for product " + productId);
        }
        Product product = productRepo.findById(productId)
            .orElseThrow(() -> ApiException.notFound("Product " + productId));
        if (product.getStock() < quantity) {
            throw ApiException.conflict("Insufficient stock for product " + productId
                + " (requested " + quantity + ", available " + product.getStock() + ")");
        }
        existing.setQuantity(quantity);
        cart.setUpdatedAt(Instant.now());
        return toDto(cartRepo.save(cart));
    }

    @Transactional
    public CartDto removeItem(Long userId, Long productId) {
        Cart cart = loadOrCreate(userId);
        boolean removed = cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        if (!removed) {
            throw ApiException.notFound("Cart item for product " + productId);
        }
        cart.setUpdatedAt(Instant.now());
        return toDto(cartRepo.save(cart));
    }

    @Transactional
    public void clear(Long userId) {
        Cart cart = loadOrCreate(userId);
        cart.getItems().clear();
        cart.setUpdatedAt(Instant.now());
        cartRepo.save(cart);
    }

    private Cart loadOrCreate(Long userId) {
        if (!userRepo.existsById(userId)) {
            throw ApiException.notFound("User " + userId);
        }
        return cartRepo.findByUserId(userId).orElseGet(() -> {
            Cart c = new Cart();
            c.setUserId(userId);
            return cartRepo.save(c);
        });
    }

    private CartItem findItem(Cart cart, Long productId) {
        for (CartItem i : cart.getItems()) {
            if (i.getProductId().equals(productId)) return i;
        }
        return null;
    }

    private CartDto toDto(Cart cart) {
        List<Long> productIds = cart.getItems().stream().map(CartItem::getProductId).toList();
        Map<Long, Product> byId = new HashMap<>();
        for (Product p : productRepo.findAllById(productIds)) {
            byId.put(p.getId(), p);
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        List<CartDto.CartItemDto> items = new java.util.ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            Product p = byId.get(ci.getProductId());
            if (p == null) continue;
            BigDecimal line = p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            subtotal = subtotal.add(line);
            items.add(new CartDto.CartItemDto(p.getId(), p.getName(), ci.getQuantity(),
                p.getPrice(), line, p.getStock()));
        }
        return new CartDto(cart.getId(), cart.getUserId(), items, subtotal, cart.getUpdatedAt());
    }
}
