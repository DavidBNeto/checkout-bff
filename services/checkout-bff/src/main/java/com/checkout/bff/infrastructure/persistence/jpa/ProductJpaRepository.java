package com.checkout.bff.infrastructure.persistence.jpa;

import com.checkout.bff.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    List<Product> findByActiveTrue();

    @Query("SELECT p FROM Product p WHERE p.slug IN :slugs AND p.active = true")
    List<Product> findBySlugInAndActiveTrue(@Param("slugs") List<String> slugs);
}