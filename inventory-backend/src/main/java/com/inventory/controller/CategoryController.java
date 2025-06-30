package com.inventory.controller;

import com.inventory.model.Category;
import com.inventory.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for managing product categories.
 * Accessible only by users with the OWNER role.
 */
@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Initializes some default categories if none exist.
     * For development convenience.
     */
    @PostConstruct
    public void initCategories() {
        if (categoryRepository.count() == 0) {
            categoryRepository.save(new Category("Drinks", "Beverages like sodas, water, juice."));
            categoryRepository.save(new Category("Snacks", "Biscuits, crisps, candies."));
            categoryRepository.save(new Category("Household", "Cleaning supplies, detergents."));
            categoryRepository.save(new Category("Groceries", "Food items, dry goods."));
            System.out.println("Initialized default categories.");
        }
    }


    /**
     * Get all categories.
     * Accessible by OWNER.
     * @return List of all categories.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<List<Category>> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return ResponseEntity.ok(categories);
    }

    /**
     * Get a category by ID.
     * Accessible by OWNER.
     * @param id The ID of the category.
     * @return The category if found, else 404.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        Optional<Category> category = categoryRepository.findById(id);
        return category.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new category.
     * Accessible by OWNER.
     * @param category The category object to create.
     * @return The created category with HTTP status 201.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<?> createCategory(@RequestBody Category category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Category name cannot be empty.");
        }
        if (categoryRepository.existsByName(category.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Category with name '" + category.getName() + "' already exists.");
        }
        Category savedCategory = categoryRepository.save(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
    }

    /**
     * Update an existing category.
     * Accessible by OWNER.
     * @param id The ID of the category to update.
     * @param categoryDetails The updated category details.
     * @return The updated category, else 404 or 400.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody Category categoryDetails) {
        Optional<Category> optionalCategory = categoryRepository.findById(id);
        if (optionalCategory.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Category existingCategory = optionalCategory.get();
        if (categoryDetails.getName() == null || categoryDetails.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Category name cannot be empty.");
        }
        if (!existingCategory.getName().equalsIgnoreCase(categoryDetails.getName()) && categoryRepository.existsByName(categoryDetails.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Another category with name '" + categoryDetails.getName() + "' already exists.");
        }

        existingCategory.setName(categoryDetails.getName());
        existingCategory.setDescription(categoryDetails.getDescription());
        Category updatedCategory = categoryRepository.save(existingCategory);
        return ResponseEntity.ok(updatedCategory);
    }

    /**
     * Delete a category by ID.
     * Accessible by OWNER.
     * Note: This currently does not check for associated products.
     * In a real system, you'd prevent deletion if products are linked or reassign products.
     * @param id The ID of the category to delete.
     * @return 204 No Content on success, or 404 if not found.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        // TODO: In a production system, implement logic to handle products associated with this category
        // e.g., prevent deletion, reassign products to a default category, or delete associated products.
        // For now, it deletes the category even if products are linked (will cause foreign key constraint error if products exist).
        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
