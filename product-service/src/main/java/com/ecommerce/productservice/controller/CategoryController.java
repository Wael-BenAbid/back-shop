package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryController(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<Category> list(@RequestParam(required = false) Long parentId) {
        if (parentId != null) {
            return categoryRepository.findByParentIdOrderByDisplayOrderAscNameAsc(parentId);
        }
        return categoryRepository.findByParentIdOrderByDisplayOrderAscNameAsc(null);
    }

    @GetMapping("/tree")
    public List<Map<String, Object>> tree() {
        List<Category> roots = categoryRepository.findByParentIdOrderByDisplayOrderAscNameAsc(null);
        return buildTree(roots);
    }

    @GetMapping("/{idOrSlug}")
    public ResponseEntity<Category> get(@PathVariable String idOrSlug) {
        try {
            Long id = Long.parseLong(idOrSlug);
            return categoryRepository.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (NumberFormatException e) {
            return categoryRepository.findBySlug(idOrSlug).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> create(@RequestBody Category payload) {
        if (payload.getSlug() == null || payload.getSlug().isBlank()) {
            payload.setSlug(Category.toSlug(payload.getName()));
        }
        if (payload.getSlug() == null || categoryRepository.existsBySlug(payload.getSlug())) {
            return ResponseEntity.badRequest().build();
        }
        if (payload.getDisplayOrder() == null) payload.setDisplayOrder(0);
        if (payload.getIsActive() == null) payload.setIsActive(true);
        Category saved = categoryRepository.save(payload);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> update(@PathVariable Long id, @RequestBody Category payload) {
        return categoryRepository.findById(id).map(cat -> {
            if (payload.getName() != null) cat.setName(payload.getName());
            if (payload.getSlug() != null) cat.setSlug(payload.getSlug());
            if (payload.getDescription() != null) cat.setDescription(payload.getDescription());
            if (payload.getImageUrl() != null) cat.setImageUrl(payload.getImageUrl());
            if (payload.getBannerUrl() != null) cat.setBannerUrl(payload.getBannerUrl());
            if (payload.getParentId() != null) cat.setParentId(payload.getParentId());
            if (payload.getDisplayOrder() != null) cat.setDisplayOrder(payload.getDisplayOrder());
            if (payload.getIsActive() != null) cat.setIsActive(payload.getIsActive());
            return ResponseEntity.ok(categoryRepository.save(cat));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reorder(@RequestBody List<Map<String, Object>> orders) {
        for (Map<String, Object> item : orders) {
            Long id = ((Number) item.get("id")).longValue();
            Integer displayOrder = ((Number) item.get("displayOrder")).intValue();
            categoryRepository.findById(id).ifPresent(cat -> {
                cat.setDisplayOrder(displayOrder);
                categoryRepository.save(cat);
            });
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Optional<Category> catOpt = categoryRepository.findById(id);
        if (catOpt.isEmpty()) return ResponseEntity.notFound().build();
        Category cat = catOpt.get();
        // interdire suppression si produits liés (simple check par name jusqu'à migration complète)
        long count = productRepository.countByCategoryName(cat.getName());
        if (count > 0) {
            return ResponseEntity.status(409).build();
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private List<Map<String, Object>> buildTree(List<Category> nodes) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Category c : nodes) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("name", c.getName());
            item.put("slug", c.getSlug());
            item.put("description", c.getDescription());
            item.put("imageUrl", c.getImageUrl());
            item.put("bannerUrl", c.getBannerUrl());
            item.put("displayOrder", c.getDisplayOrder());
            item.put("isActive", c.getIsActive());
            List<Category> children = categoryRepository.findByParentIdOrderByDisplayOrderAscNameAsc(c.getId());
            item.put("children", buildTree(children));
            result.add(item);
        }
        return result;
    }
}
