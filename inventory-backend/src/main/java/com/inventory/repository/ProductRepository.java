package com.inventory.repository;

import com.inventory.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// @Repository indicates that this interface is a "Repository" in the Spring sense,
// providing mechanisms for storage, retrieval, update, delete operation on objects.
@Repository
// JpaRepository provides standard CRUD (Create, Read, Update, Delete) operations
// and pagination/sorting capabilities.
// It takes two generic parameters: the entity type (Product) and the ID type (Long).
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Custom method to find products by name, ignoring case.
    // Spring Data JPA automatically generates the query for this method name.
    List<Product> findByNameContainingIgnoreCase(String name);

    // Custom method to find products with quantity less than or equal to their minStockLevel.
    // This will be used for low stock alerts.
    List<Product> findByQuantityLessThanEqual(int quantity);
}
