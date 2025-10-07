package com.example.ecommerce.il.dto;

import java.math.BigDecimal;

public record ProductListItemDto(
        Long id,
        String name,
        BigDecimal price,
        String categoryName,
        String imageUrl
) {}