package com.example.ecommerce.dal.repositories;

import com.example.ecommerce.dl.entities.Cart;
import com.example.ecommerce.dl.entities.CartLine;
import com.example.ecommerce.dl.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * DAL: CartLine repository.
 * Used for upsert pattern (findByCartAndProduct) and displaying cart lines.
 */
public interface CartLineRepository extends JpaRepository<CartLine, Long> {

    Optional<CartLine> findByCartAndProduct(Cart cart, Product product);

    List<CartLine> findByCart(Cart cart);
}