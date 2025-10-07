// src/main/java/com/example/ecommerce/il/interfaces/CategoryService.java
package com.example.ecommerce.il.interfaces;

import com.example.ecommerce.il.dto.CategoryDto;
import java.util.List;

/**
 * Contrat pour les catégories (lecture / création / suppression).
 */
public interface CategoryService {
    /** Retourne toutes les catégories (pour filtres / formulaires). */
    List<CategoryDto> findAll();

    /** Crée une catégorie et renvoie son identifiant. */
    Long create(String name);

    /** Supprime une catégorie par identifiant. */
    void delete(Long id);
}
