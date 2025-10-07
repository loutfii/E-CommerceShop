package com.example.ecommerce.il.dto;

import java.math.BigDecimal;

public record CartLineDto(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal
) {}