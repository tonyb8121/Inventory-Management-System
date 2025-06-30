package com.inventory.controller;

import com.inventory.model.Sale;
import com.inventory.service.SalesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing individual sales.
 * IMPORTANT: Receipt-specific endpoints (like /receipts, /receipts/{id}, /receipts/batch) are now
 * exclusively handled by ReceiptController to avoid ambiguous mappings and ensure clear responsibilities.
 * This controller now focuses purely on `Sale` entities themselves, not the `Receipt` parent.
 */
@RestController
@RequestMapping("/api/sales") // Base path for individual sales operations
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class SalesController {

    @Autowired
    private SalesService salesService; // Service for individual Sale entities

    // Removed @Autowired private ReceiptService receiptService;
    // Removed @PostMapping("/batch") recordBatchSale method - this now belongs solely in ReceiptController.


    /**
     * Get all individual sales (items) regardless of their receipt.
     * This is separate from fetching receipts themselves.
     * Accessible by OWNER or CASHIER.
     * @return List of all individual Sale objects.
     */
    @GetMapping // Maps to /api/sales
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_CASHIER')")
    public ResponseEntity<List<Sale>> getAllSales() {
        System.out.println("DEBUG: SalesController - getAllSales() - Current Authentication: " + SecurityContextHolder.getContext().getAuthentication());
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            System.out.println("DEBUG: SalesController - getAllSales() - Authorities: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        }

        List<Sale> sales = salesService.getAllSales();
        return ResponseEntity.ok(sales);
    }

    /**
     * Delete an individual sale line item by ID.
     * Note: This operation might need careful consideration if all sales are tied to receipts.
     * Accessible by OWNER or CASHIER.
     * @param id The ID of the sale to delete.
     * @return 204 No Content on success, or 404 if not found.
     */
    @DeleteMapping("/{id}") // Maps to /api/sales/{id}
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_CASHIER')")
    public ResponseEntity<Void> deleteSale(@PathVariable Long id) {
        System.out.println("DEBUG: SalesController - deleteSale() - Current Authentication: " + SecurityContextHolder.getContext().getAuthentication());
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            System.out.println("DEBUG: SalesController - deleteSale() - Authorities: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        }

        try {
            salesService.deleteSale(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // All conflicting receipt-related endpoints (like /receipts, /receipts/{id}, /receipts/filter, /receipts/batch)
    // have been removed from here and should now be exclusively defined in ReceiptController.java.
}
