package com.example.ecommerce.il.dto;

import java.math.BigDecimal;

/*Pourquoi des DTO séparés “list” vs “detail” ?
Pour éviter la surcharge (l’index n’a pas besoin de la description/stock par ex.), et optimiser les projections*/

public record ProductDetailDto(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String categoryName,
        String imageUrl
) {}