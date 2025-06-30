package com.inventory.controller;

import com.inventory.model.StockAdjustment;
import com.inventory.service.StockAdjustmentService;
import com.inventory.service.StockAdjustmentService.StockAdjustmentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;

/**
 * REST Controller for managing stock-related operations, primarily stock adjustments.
 * Accessible only by users with the OWNER role. Stock history view is now accessible by CASHIER.
 */
@RestController
@RequestMapping("/api/stock")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class StockController {

    @Autowired
    private StockAdjustmentService stockAdjustmentService;

    /**
     * Endpoint to record a new stock adjustment.
     * Accessible by OWNER.
     * @param request The StockAdjustmentRequest containing details of the adjustment.
     * @return The created StockAdjustment entity, or an error response.
     */
    @PostMapping("/adjustments")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<?> adjustStock(@RequestBody StockAdjustmentRequest request) {
        try {
            StockAdjustment newAdjustment = stockAdjustmentService.adjustStock(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(newAdjustment);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error during stock adjustment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Endpoint to retrieve the complete history of stock adjustments.
     * Accessible by OWNER and now by CASHIER.
     * @return A list of StockAdjustment entities (or String in case of error).
     */
    @GetMapping("/adjustments/history")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_CASHIER')") // MODIFIED: Added ROLE_CASHIER
    public ResponseEntity<?> getStockAdjustmentHistory() {
        try {
            List<StockAdjustment> history = stockAdjustmentService.getStockAdjustmentHistory();
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            System.err.println("Error fetching stock adjustment history: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch stock adjustment history: " + e.getMessage());
        }
    }
}
