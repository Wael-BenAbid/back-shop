package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        Optional<Order> order = orderRepository.findById(id);
        return order.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Order createOrder(@RequestBody Order order) {
        Order savedOrder = orderRepository.save(order);
        // Decrement stock for each product
        RestTemplate restTemplate = new RestTemplate();
        for (Long productId : savedOrder.getProductIds()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                // Assume token is passed or use service account
                // For simplicity, assume product-service allows update without auth for internal calls
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                restTemplate.exchange(
                    "http://localhost:8081/api/products/" + productId + "/decrement-stock",
                    HttpMethod.PUT,
                    entity,
                    Void.class
                );
            } catch (Exception e) {
                // Log error, but don't fail order
                System.err.println("Failed to decrement stock for product " + productId + ": " + e.getMessage());
            }
        }
        return savedOrder;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order orderDetails) {
        Optional<Order> order = orderRepository.findById(id);
        if (order.isPresent()) {
            Order updatedOrder = order.get();
            updatedOrder.setUsername(orderDetails.getUsername());
            updatedOrder.setProductIds(orderDetails.getProductIds());
            updatedOrder.setStatus(orderDetails.getStatus());
            return ResponseEntity.ok(orderRepository.save(updatedOrder));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        if (orderRepository.existsById(id)) {
            orderRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
