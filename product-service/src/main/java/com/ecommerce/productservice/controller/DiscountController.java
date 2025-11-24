package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.entity.Discount;
import com.ecommerce.productservice.repository.DiscountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/discounts")
public class DiscountController {

    @Autowired
    private DiscountRepository discountRepository;

    @GetMapping
    public List<Discount> getAllDiscounts() {
        return discountRepository.findAll();
    }

    @GetMapping("/active")
    public List<Discount> getActiveDiscounts() {
        return discountRepository.findByActiveTrue();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Discount> getDiscountById(@PathVariable Long id) {
        Optional<Discount> discount = discountRepository.findById(id);
        return discount.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Discount createDiscount(@RequestBody Discount discount) {
        return discountRepository.save(discount);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Discount> updateDiscount(@PathVariable Long id, @RequestBody Discount discountDetails) {
        Optional<Discount> discount = discountRepository.findById(id);
        if (discount.isPresent()) {
            Discount updatedDiscount = discount.get();
            updatedDiscount.setName(discountDetails.getName());
            updatedDiscount.setDescription(discountDetails.getDescription());
            updatedDiscount.setPercentage(discountDetails.getPercentage());
            updatedDiscount.setStartDate(discountDetails.getStartDate());
            updatedDiscount.setEndDate(discountDetails.getEndDate());
            updatedDiscount.setActive(discountDetails.getActive());
            return ResponseEntity.ok(discountRepository.save(updatedDiscount));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDiscount(@PathVariable Long id) {
        if (discountRepository.existsById(id)) {
            discountRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}