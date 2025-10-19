package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.productservice.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/categories")
    public List<String> getCategories() {
        return productRepository.findDistinctCategories();
    }

    @GetMapping("/by-category/{category}")
    public List<Product> getByCategory(@PathVariable String category) {
        return productRepository.findByCategory_NameOrderByHomePositionAscNameAsc(category);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Optional<Product> product = productRepository.findById(id);
        return product.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Product createProduct(@RequestBody Product product) {
        // If category is provided as string, find or create the category
        if (product.getCategory() != null && product.getCategory().getName() != null) {
            Category category = categoryRepository.findBySlug(Category.toSlug(product.getCategory().getName()))
                    .orElseGet(() -> {
                        Category newCat = new Category();
                        newCat.setName(product.getCategory().getName());
                        newCat.setSlug(Category.toSlug(product.getCategory().getName()));
                        newCat.setIsActive(true);
                        return categoryRepository.save(newCat);
                    });
            product.setCategory(category);
        }
        return productRepository.save(product);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Product> createProducts(@RequestBody List<Product> products) {
        return productRepository.saveAll(products);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            Product updatedProduct = product.get();
            updatedProduct.setName(productDetails.getName());
            updatedProduct.setDescription(productDetails.getDescription());
            updatedProduct.setCategory(productDetails.getCategory());
            updatedProduct.setPrice(productDetails.getPrice());
            updatedProduct.setStock(productDetails.getStock());
            updatedProduct.setImageUrl(productDetails.getImageUrl());
            updatedProduct.setVideoUrl(productDetails.getVideoUrl());
            updatedProduct.setHighResImageUrl(productDetails.getHighResImageUrl());
            updatedProduct.setHomePosition(productDetails.getHomePosition());
            return ResponseEntity.ok(productRepository.save(updatedProduct));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
