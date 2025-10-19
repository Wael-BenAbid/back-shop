package com.ecommerce.productservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // Backfill initial des catégories depuis les produits existants
    @Bean
    public org.springframework.boot.CommandLineRunner backfillCategories(
            com.ecommerce.productservice.repository.ProductRepository productRepository,
            com.ecommerce.productservice.repository.CategoryRepository categoryRepository
    ) {
        return args -> {
            var existing = categoryRepository.findAll();
            var existingByName = new java.util.HashMap<String, com.ecommerce.productservice.entity.Category>();
            for (var c : existing) existingByName.put(c.getName(), c);

            var names = productRepository.findDistinctCategories();
            for (String name : names) {
                if (name == null || name.isBlank()) continue;
                if (!existingByName.containsKey(name)) {
                    var cat = new com.ecommerce.productservice.entity.Category();
                    cat.setName(name);
                    cat.setSlug(com.ecommerce.productservice.entity.Category.toSlug(name));
                    cat.setDisplayOrder(0);
                    cat.setIsActive(true);
                    categoryRepository.save(cat);
                }
            }
        };
    }
}
