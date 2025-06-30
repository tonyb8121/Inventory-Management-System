package com.inventory.controller;

import com.inventory.model.Product;
import com.inventory.service.ProductService;
import com.inventory.service.SalesService; // Added SalesService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Import PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder; // Import SecurityContextHolder
import org.springframework.web.bind.annotation.*;

import java.util.HashMap; // For returning a map of summary data
import java.util.List;
import java.util.Map;

// @RestController marks this as a REST controller.
@RestController
// @RequestMapping sets the base path for dashboard-related endpoints.
@RequestMapping("/api/dashboard")
// @CrossOrigin enables CORS for flexible frontend integration.
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.OPTIONS})
public class DashboardController {

    // Autowire ProductService to get low stock product information and total product count.
    @Autowired
    private ProductService productService;

    // Autowire SalesService to get total sales amount.
    @Autowired
    private SalesService salesService;

    /**
     * GET /api/dashboard/low-stock
     * Endpoint to retrieve a list of products that are currently below their minimum stock level.
     * Accessible by OWNER or CASHIER.
     * @return ResponseEntity with a list of low stock Product objects and HTTP status 200.
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_CASHIER')") // MODIFIED: Changed to hasAnyAuthority
    public ResponseEntity<List<Product>> getLowStockProducts() {
        // DEBUGGING: Print current authentication details
        System.out.println("DEBUG: DashboardController - getLowStockProducts() - Current Authentication: " + SecurityContextHolder.getContext().getAuthentication());
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            System.out.println("DEBUG: DashboardController - getLowStockProducts() - Authorities: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        }

        List<Product> lowStockProducts = productService.getLowStockProducts();
        return ResponseEntity.ok(lowStockProducts);
    }

    /**
     * GET /api/dashboard/summary
     * Endpoint to retrieve a summary of key metrics for the dashboard.
     * Accessible by OWNER or CASHIER.
     * @return ResponseEntity with a Map containing various summary statistics (e.g., total products, total sales).
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_CASHIER')") // MODIFIED: Changed to hasAnyAuthority
    public ResponseEntity<Map<String, Double>> getDashboardSummary() {
        // DEBUGGING: Print current authentication details
        System.out.println("DEBUG: DashboardController - getDashboardSummary() - Current Authentication: " + SecurityContextHolder.getContext().getAuthentication());
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            System.out.println("DEBUG: DashboardController - getDashboardSummary() - Authorities: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        }

        Map<String, Double> summary = new HashMap<>();

        // Get total product count
        long totalProducts = productService.getTotalProductCount();
        summary.put("totalProducts", (double) totalProducts); // Cast to double for consistency in map

        // Get total sales amount
        double totalSales = salesService.getTotalSalesAmount();
        summary.put("totalSales", totalSales);

        return ResponseEntity.ok(summary);
    }
}
