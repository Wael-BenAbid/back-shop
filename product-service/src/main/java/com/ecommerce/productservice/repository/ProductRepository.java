package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    java.util.List<Product> findByCategory_NameOrderByHomePositionAscNameAsc(String categoryName);
    @org.springframework.data.jpa.repository.Query("select distinct p.category.name from Product p where p.category is not null")
    java.util.List<String> findDistinctCategories();

    @org.springframework.data.jpa.repository.Query("select count(p) from Product p where p.category.name = :name")
    long countByCategoryName(@org.springframework.data.repository.query.Param("name") String name);
}
