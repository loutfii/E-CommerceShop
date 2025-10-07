package com.example.ecommerce.dal.repositories;

import com.example.ecommerce.dl.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * DAL: Category repository.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);
}
