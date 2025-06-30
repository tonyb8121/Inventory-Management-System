package com.inventory.repository;

import com.inventory.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // Keep Optional import

/**
 * Spring Data JPA repository for the Sale entity.
 * Provides CRUD operations and custom query methods for sales.
 */
@Repository
public interface SalesRepository extends JpaRepository<Sale, Long> {

    /**
     * Checks if any sales exist for a given product ID.
     * Used to prevent deletion of products that have associated sales.
     * @param productId The ID of the product.
     * @return true if sales exist for the product, false otherwise.
     */
    boolean existsByProductId(Long productId);

    // REMOVED: findBySaleDateBetween as Sale entity no longer has 'saleDate' field.
    // Date-based queries should now be performed on the Receipt entity.

    /**
     * Finds a sale by its ID.
     * Re-added for individual sale operations.
     * @param id The ID of the sale to find.
     * @return An Optional containing the Sale if found, or empty otherwise.
     */
    Optional<Sale> findById(Long id);

    /**
     * Finds all sales for a given product ID.
     * @param productId The ID of the product.
     * @return A list of sales associated with the product.
     */
    List<Sale> findByProductId(Long productId);
}
