// src/main/java/com/example/ecommerce/dal/repositories/CartRepository.java
package com.example.ecommerce.dal.repositories;

import com.example.ecommerce.dl.entities.Cart;
import com.example.ecommerce.dl.entities.User;
import com.example.ecommerce.dl.enums.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserAndStatus(User user, CartStatus status);
    List<Cart> findAllByUserAndStatusOrderByUpdatedAtDesc(User user, CartStatus status);

    // nécessaire pour supprimer tous les paniers du user
    List<Cart> findAllByUser(User user);
}
