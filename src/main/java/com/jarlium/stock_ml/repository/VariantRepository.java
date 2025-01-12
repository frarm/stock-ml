package com.jarlium.stock_ml.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jarlium.stock_ml.model.Product;
import com.jarlium.stock_ml.model.Variant;

public interface VariantRepository extends JpaRepository<Variant, Long> {
    Optional<Variant> findByProductAndName(Product product, String name);
}