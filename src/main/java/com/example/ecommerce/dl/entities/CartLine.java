package com.example.ecommerce.dl.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * Cart line = a product row inside a cart, with a quantity.
 * - Unique (cart_id, product_id) guarantees only one line per product in a given cart.
 */
@Entity
@Table(name = "cart_lines",
        uniqueConstraints = @UniqueConstraint(name = "uk_cartline_cart_product", columnNames = {"cart_id", "product_id"}),
        indexes = {
                @Index(name = "ix_cartline_cart", columnList = "cart_id"),
                @Index(name = "ix_cartline_product", columnList = "product_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartLine {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cartline_cart"))
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cartline_product"))
    private Product product;

    @Column(nullable = false)
    private Integer quantity;
}