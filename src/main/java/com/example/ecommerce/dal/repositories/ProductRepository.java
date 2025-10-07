// src/main/java/com/example/ecommerce/dal/repositories/ProductRepository.java
package com.example.ecommerce.dal.repositories;

import com.example.ecommerce.dl.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * ProductRepository
 * -----------------
 * - Specifications pour filtres dynamiques (q/min/max/category).
 * - fetch-join pour le détail afin d'éviter les LazyInitializationException.
 * - Compteur d'usage d'une catégorie (pour sécuriser la suppression côté admin).
 */
public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Page<Product> findByNameContainingIgnoreCase(String q, Pageable pageable);

    /**
     * Charge le produit et sa catégorie (si présente) en 1 requête.
     * Évite d'avoir à "toucher" la relation dans le service.
     */
    @Query("""
           select p
           from Product p
           left join fetch p.category
           where p.id = :id
           """)
    Optional<Product> findByIdWithCategory(@Param("id") Long id);

    /**
     * Compte combien de produits référencent une catégorie donnée.
     * Utilisé pour interdire la suppression d'une catégorie encore utilisée.
     */
    long countByCategoryId(Long categoryId);
}
