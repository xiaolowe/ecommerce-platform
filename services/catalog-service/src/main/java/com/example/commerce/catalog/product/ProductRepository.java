package com.example.commerce.catalog.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {

    Optional<Product> findByIdAndStatus(String id, ProductStatus status);

    boolean existsBySkuIgnoreCase(String sku);
}
