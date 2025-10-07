package com.example.ecommerce.il.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartDto(
        List<CartLineDto> lines,
        int totalItems,
        BigDecimal totalAmount
) {}
