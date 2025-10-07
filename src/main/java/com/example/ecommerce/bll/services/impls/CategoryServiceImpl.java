// src/main/java/com/example/ecommerce/bll/services/impls/CategoryServiceImpl.java
package com.example.ecommerce.bll.services.impls;

import com.example.ecommerce.bll.mappers.DtoMapper;
import com.example.ecommerce.dal.repositories.CategoryRepository;
import com.example.ecommerce.dl.entities.Category;
import com.example.ecommerce.il.dto.CategoryDto;
import com.example.ecommerce.il.interfaces.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service Catégories
 * - Lecture pour formulaires/filtres
 * - Création simple (utilisée par la modale "New category" côté admin)
 * - Suppression
 */
@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categories;

    public CategoryServiceImpl(CategoryRepository categories) {
        this.categories = categories;
    }

    /** Liste pour les formulaires/filtres. */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> findAll() {
        return categories.findAll().stream().map(DtoMapper::toCategoryDto).toList();
    }

    /**
     * Création d'une catégorie (nom requis, vérif unicité logique).
     * @param name nom de la catégorie (requis)
     * @return id de la nouvelle catégorie
     */
    @Override
    public Long create(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name is required");
        }
        String trimmed = name.trim();

        // (Optionnel) unicité logique sans contrainte DB
        boolean exists = categories.findAll().stream()
                .anyMatch(c -> c.getName() != null && c.getName().equalsIgnoreCase(trimmed));
        if (exists) {
            throw new IllegalArgumentException("Category already exists");
        }

        Category c = new Category();
        c.setName(trimmed);
        categories.save(c);
        return c.getId();
    }

    /** Suppression simple par identifiant. */
    @Override
    public void delete(Long id) {
        categories.deleteById(id);
    }
}
