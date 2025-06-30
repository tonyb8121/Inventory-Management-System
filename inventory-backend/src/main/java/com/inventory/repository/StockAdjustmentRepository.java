package com.inventory.repository;

import com.inventory.model.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the StockAdjustment entity.
 * Provides CRUD operations and custom query methods for stock adjustments.
 */
@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

    /**
     * Finds all stock adjustments, ordered by adjustment date in descending order.
     * @return A list of all stock adjustments.
     */
    List<StockAdjustment> findAllByOrderByAdjustmentDateDesc();
}
