package com.inventory.repository;

import com.inventory.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Category entity.
 * Provides CRUD operations and custom query methods for categories.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Checks if a category with the given name already exists (case-insensitive).
     * @param name The name of the category to check.
     * @return true if a category with the name exists, false otherwise.
     */
    boolean existsByName(String name);
}
