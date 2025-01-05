package com.jarlium.stock_ml.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jarlium.stock_ml.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}