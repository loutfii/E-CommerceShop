// src/main/java/com/example/ecommerce/il/interfaces/ProductService.java
package com.example.ecommerce.il.interfaces;

import com.example.ecommerce.il.dto.ProductDetailDto;
import com.example.ecommerce.il.dto.ProductListItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProductService {

    Page<ProductListItemDto> findPage(String q, java.math.BigDecimal min, java.math.BigDecimal max, Long categoryId, Pageable pageable);

    Optional<ProductDetailDto> findDetail(Long productId);

    Long create(String name, String description, BigDecimal price, Integer stock, Long categoryId, String imageUrl);

    void update(Long id, String name, String description, BigDecimal price, Integer stock, Long categoryId, String imageUrl);

    void delete(Long id);

    /** ✅ Nouveau : renvoie l'id de catégorie du produit (si présent). */
    Optional<Long> findCategoryIdOfProduct(Long productId);
}
