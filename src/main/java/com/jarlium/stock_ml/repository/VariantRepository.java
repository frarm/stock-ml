package com.jarlium.stock_ml.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.jarlium.stock_ml.model.Product;
import com.jarlium.stock_ml.model.Variant;

public interface VariantRepository extends JpaRepository<Variant, Long> {
    Optional<Variant> findByProductAndColor(Product product, String color);

    // Método para traer solo el campo color de las variantes de un producto específico
    @Query("SELECT v.color FROM Variant v WHERE v.product = :product")
    List<String> findColorsByProduct(Product product);
}