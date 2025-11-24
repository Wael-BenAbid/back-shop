package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartRepository cartRepository;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Cart> getCartById(@PathVariable Long id) {
        Optional<Cart> cart = cartRepository.findById(id);
        return cart.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Cart createCart(@RequestBody Cart cart) {
        return cartRepository.save(cart);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Cart> updateCart(@PathVariable Long id, @RequestBody Cart cartDetails) {
        Optional<Cart> cart = cartRepository.findById(id);
        if (cart.isPresent()) {
            Cart updatedCart = cart.get();
            updatedCart.setUsername(cartDetails.getUsername());
            updatedCart.setProductIds(cartDetails.getProductIds());
            return ResponseEntity.ok(cartRepository.save(updatedCart));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteCart(@PathVariable Long id) {
        if (cartRepository.existsById(id)) {
            cartRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Cart> addToCart(@RequestBody Map<String, Long> request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // Find existing cart for user or create new one
        Optional<Cart> existingCart = cartRepository.findAll().stream()
            .filter(cart -> cart.getUsername() != null && cart.getUsername().equals(username))
            .findFirst();

        Cart cart;
        if (existingCart.isPresent()) {
            cart = existingCart.get();
            // Add product to existing cart
            List<Long> productIds = cart.getProductIds();
            if (productIds == null) {
                productIds = new java.util.ArrayList<>();
            }
            if (!productIds.contains(request.get("productId"))) {
                productIds.add(request.get("productId"));
            }
            cart.setProductIds(productIds);
        } else {
            // Create new cart
            cart = new Cart();
            cart.setUsername(username);
            List<Long> productIds = new java.util.ArrayList<>();
            productIds.add(request.get("productId"));
            cart.setProductIds(productIds);
        }

        Cart savedCart = cartRepository.save(cart);
        return ResponseEntity.ok(savedCart);
    }
}
