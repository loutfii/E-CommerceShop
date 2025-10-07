package com.example.ecommerce.bll.mappers;

import com.example.ecommerce.dl.entities.CartLine;
import com.example.ecommerce.dl.entities.Product;
import com.example.ecommerce.il.dto.*;

import java.math.BigDecimal;
import java.util.List;

/*Pourquoi un util statique ? Simple, sans dépendances
* Le mapping est un détail d’implémentation : PL ne doit pas savoir comment on compose un ProductListItemDto. On garde ça en BLL.*/
public final class DtoMapper {
    private DtoMapper() {}

    // --- Category ---
    public static CategoryDto toCategoryDto(com.example.ecommerce.dl.entities.Category c) {
        return new CategoryDto(c.getId(), c.getName());
    }

    // --- Product (list item) ---
    public static ProductListItemDto toProductListItem(Product p) {
        return new ProductListItemDto(
                p.getId(),
                p.getName(),
                p.getPrice(),
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getImageUrl()
        );
    }

    // --- Product (detail) ---
    public static ProductDetailDto toProductDetail(Product p) {
        return new ProductDetailDto(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getStock(),
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getImageUrl()
        );
    }

    // --- Cart ---
    public static CartLineDto toCartLineDto(CartLine l) {
        var unit = l.getProduct().getPrice();
        var total = unit.multiply(BigDecimal.valueOf(l.getQuantity()));
        return new CartLineDto(
                l.getProduct().getId(),
                l.getProduct().getName(),
                unit,
                l.getQuantity(),
                total
        );
    }

    public static CartDto toCartDto(List<CartLineDto> lines) {
        int totalItems = lines.stream().mapToInt(CartLineDto::quantity).sum();
        BigDecimal totalAmount = lines.stream().map(CartLineDto::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartDto(lines, totalItems, totalAmount);
    }
}
