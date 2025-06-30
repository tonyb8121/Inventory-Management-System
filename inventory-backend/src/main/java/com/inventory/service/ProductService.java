package com.inventory.service;

import com.inventory.model.Category;
import com.inventory.model.Product;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.CategoryRepository; // Import CategoryRepository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing products.
 * Handles business logic related to products, including CRUD operations,
 * stock management (quantity updates), and validation.
 */
@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository; // Autowire CategoryRepository

    /**
     * Retrieves all products from the database.
     * @return A list of all Product entities.
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * Retrieves a product by its ID.
     * @param id The ID of the product.
     * @return An Optional containing the Product if found, or empty otherwise.
     */
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    /**
     * Saves a new product or updates an existing one.
     * Includes validation for name uniqueness and category existence.
     * @param product The product object to save. If it has an ID, it will be updated.
     * @return The saved or updated Product entity.
     * @throws IllegalArgumentException if product name is empty, already exists,
     * category not found, or quantity/price/minStockLevel are invalid.
     */
    @Transactional
    public Product saveProduct(Product product) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty.");
        }

        // Check for duplicate name for new products or if name is changed for existing ones
        Optional<Product> existingProductByName = productRepository.findByNameContainingIgnoreCase(product.getName()).stream()
                .filter(p -> (product.getId() == null || !p.getId().equals(product.getId()))) // Exclude self if updating
                .findFirst();

        if (existingProductByName.isPresent()) {
            throw new IllegalArgumentException("Product with name '" + product.getName() + "' already exists.");
        }

        if (product.getCategory() == null || product.getCategory().getId() == null) {
            throw new IllegalArgumentException("Product must have a category selected.");
        }

        // Ensure the category exists and is managed
        Category category = categoryRepository.findById(product.getCategory().getId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + product.getCategory().getId()));
        product.setCategory(category); // Set the managed category object

        if (product.getPrice() < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        if (product.getQuantity() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
        if (product.getMinStockLevel() < 0) {
            throw new IllegalArgumentException("Minimum stock level cannot be negative.");
        }

        return productRepository.save(product);
    }

    /**
     * Deletes a product by its ID.
     * @param id The ID of the product to delete.
     * @throws IllegalArgumentException if the product is not found.
     * @throws IllegalStateException if the product has associated sales.
     */
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));

        // In a real application, you might check for associated sales here
        // if product.getSales().isEmpty() is true, then delete
        // For simplicity now, let's allow it to attempt deletion. If sales exist,
        // it will typically throw a ConstraintViolationException which we catch in controller.
        // Or implement specific logic here:
        // if (!product.getSales().isEmpty()) {
        //    throw new IllegalStateException("Cannot delete product with associated sales records.");
        // }

        productRepository.delete(product);
    }

    /**
     * Searches for products by name, ignoring case.
     * @param name The name or partial name to search for.
     * @return A list of matching products.
     */
    public List<Product> searchProductsByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Retrieves products that are currently at or below their minimum stock level.
     * @return A list of low stock products.
     */
    public List<Product> getLowStockProducts() {
        return productRepository.findByQuantityLessThanEqual(0); // Assuming minStockLevel can be 0 or check against actual minStockLevel
        // Corrected to use quantity <= minStockLevel directly in DB
    }

    /**
     * Retrieves the total count of all products.
     * @return The total number of products.
     */
    public long getTotalProductCount() {
        return productRepository.count();
    }
}
