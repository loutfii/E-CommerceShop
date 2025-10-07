// src/main/java/com/example/ecommerce/pl/controllers/AdminCategoryController.java
package com.example.ecommerce.pl.controllers;

import com.example.ecommerce.dal.repositories.ProductRepository;
import com.example.ecommerce.il.interfaces.CategoryService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NO_CONTENT;

/**
 * API admin minimaliste pour gérer les catégories depuis le formulaire produit.
 * - Création via modale (+ New)
 * - Suppression sécurisée (refus si référencée par des produits)
 * NB: Les messages renvoyés vers l'UI (JSON) sont en anglais.
 */
@RestController
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final ProductRepository productRepository;

    public AdminCategoryController(CategoryService categoryService,
                                   ProductRepository productRepository) {
        this.categoryService = categoryService;
        this.productRepository = productRepository;
    }

    /** Création (appelée par la modale). */
    @PostMapping
    public ResponseEntity<?> create(@RequestParam String name) {
        Long id = categoryService.create(name);
        return ResponseEntity
                .created(URI.create("/admin/categories/" + id))
                .body(Map.of("id", id, "name", name));
    }

    /** Suppression (bouton "Delete" sous + New). */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            long usage = productRepository.countByCategoryId(id);
            if (usage > 0) {
                return ResponseEntity.status(CONFLICT)
                        .body(Map.of("error", "Cannot delete: category is used by " + usage + " product(s)."));
            }
            categoryService.delete(id);
            return ResponseEntity.status(NO_CONTENT).build();
        } catch (DataIntegrityViolationException dive) {
            // Garde-fou si une contrainte DB bloque la suppression
            return ResponseEntity.status(CONFLICT)
                    .body(Map.of("error", "Cannot delete: category is referenced by products."));
        } catch (Exception e) {
            return ResponseEntity.status(CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
