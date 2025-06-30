package com.inventory.controller;

import com.inventory.model.Product;
import com.inventory.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Import PreAuthorize
import org.springframework.web.bind.annotation.*;

import java.util.List;

// @RestController is a convenience annotation that combines @Controller and @ResponseBody.
@RestController
// @RequestMapping specifies the base URL path.
@RequestMapping("/api/products")
// @CrossOrigin enables Cross-Origin Resource Sharing.
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_CASHIER')") // MODIFIED: Changed to hasAnyAuthority
    public ResponseEntity<List<Product>> getAllProducts(@RequestParam(required = false) String name) {
        List<Product> products;
        if (name != null && !name.trim().isEmpty()) {
            products = productService.searchProductsByName(name);
        } else {
            products = productService.getAllProducts();
        }
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_CASHIER')") // MODIFIED: Changed to hasAnyAuthority
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_OWNER')") // MODIFIED: Changed to hasAuthority
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        try {
            Product savedProduct = productService.saveProduct(product);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_OWNER')") // MODIFIED: Changed to hasAuthority
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        // Ensure the ID from the path is used for the update operation
        product.setId(id);
        try {
            Product updatedProduct = productService.saveProduct(product);
            return ResponseEntity.ok(updatedProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_OWNER')") // MODIFIED: Changed to hasAuthority
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.noContent().build(); // 204 No Content on successful deletion
        } catch (IllegalArgumentException e) {
            // Product not found, or invalid ID passed to service
            return ResponseEntity.notFound().build(); // 404 Not Found
        } catch (IllegalStateException e) {
            // Product has associated sales (Referential Integrity Constraint Violation)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); // 409 Conflict with message
        }
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_CASHIER')") // MODIFIED: Changed to hasAnyAuthority
    public ResponseEntity<List<Product>> getLowStockProducts() {
        List<Product> lowStockProducts = productService.getLowStockProducts();
        return ResponseEntity.ok(lowStockProducts);
    }
}
