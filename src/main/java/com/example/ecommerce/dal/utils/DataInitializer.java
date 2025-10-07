package com.example.ecommerce.dal.utils;

import com.example.ecommerce.dal.repositories.CategoryRepository;
import com.example.ecommerce.dal.repositories.ProductRepository;
import com.example.ecommerce.dl.entities.Category;
import com.example.ecommerce.dl.entities.Product;
import com.example.ecommerce.il.interfaces.AuthService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Peuple la DB au démarrage (en dev).
 * - Crée un admin & un user si absents
 * - Ajoute des catégories & produits de démo si vides
 */
@Component
public class DataInitializer {

    private final AuthService authService;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public DataInitializer(AuthService authService,
                           CategoryRepository categoryRepository,
                           ProductRepository productRepository) {
        this.authService = authService;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @PostConstruct
    public void init() {
        // Comptes
        try { authService.registerAdmin("admin@example.com", "admin1234"); } catch (Exception ignored) {}
        try { authService.registerUser("user@example.com", "user1234"); } catch (Exception ignored) {}

        // Catégories
        if (categoryRepository.count() == 0) {
            categoryRepository.saveAll(List.of(
                    Category.builder().name("Fruits").build(),
                    Category.builder().name("Electronics").build(),
                    Category.builder().name("Books").build()
            ));
        }

        // Produits
        if (productRepository.count() == 0) {
            var cats = categoryRepository.findAll();
            var fruits = cats.stream().filter(c -> c.getName().equals("Fruits")).findFirst().orElse(null);
            var elec   = cats.stream().filter(c -> c.getName().equals("Electronics")).findFirst().orElse(null);

            productRepository.saveAll(List.of(
                    Product.builder().name("Pomme").description("Pomme rouge de saison")
                            .price(new BigDecimal("0.80")).stock(100).category(fruits).build(),
                    Product.builder().name("Poire").description("Poire juteuse")
                            .price(new BigDecimal("0.90")).stock(80).category(fruits).build(),
                    Product.builder().name("Casque Audio").description("Casque Bluetooth, 20h d'autonomie")
                            .price(new BigDecimal("59.99")).stock(25).category(elec).build()
            ));
        }
    }
}
