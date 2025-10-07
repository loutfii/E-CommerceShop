package com.example.ecommerce.dal.repositories;

import com.example.ecommerce.dl.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * DAL: User repository.
 * No business logic here; just persistence access patterns needed by BLL.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}