package com.inventory.controller;

import com.inventory.model.Receipt;
import com.inventory.service.ReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for managing Receipt entities.
 * This controller is now the EXCLUSIVE handler for all /api/sales/receipts/** endpoints.
 */
@RestController
@RequestMapping("/api/sales/receipts") // Base path for ReceiptController
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ReceiptController {

    @Autowired
    private ReceiptService receiptService;

    /**
     * Endpoint to record a batch sale (multiple items) and create a receipt.
     * This endpoint is exclusively here now.
     * Accessible by OWNER or CASHIER.
     * @param request The RecordReceiptRequest containing details of the items sold and payment.
     * @return The created Receipt object on success, or an error response.
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_CASHIER')")
    public ResponseEntity<?> recordBatchSale(@RequestBody ReceiptService.RecordReceiptRequest request) {
        System.out.println("DEBUG: ReceiptController - recordBatchSale() hit. Current Authentication: " + SecurityContextHolder.getContext().getAuthentication());
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            System.out.println("DEBUG: ReceiptController - recordBatchSale() - Authorities: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        }
        try {
            String cashierUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            Receipt newReceipt = receiptService.recordBatchSale(request, cashierUsername);
            return ResponseEntity.status(HttpStatus.CREATED).body(newReceipt);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    /**
     * COMBINED METHOD for fetching all receipts AND filtered receipts.
     * This will now handle requests to /api/sales/receipts (for all) and
     * /api/sales/receipts?param=value (for filtered).
     * This replaces a separate @GetMapping for "getAllReceipts()" and the problematic "/filter" endpoint.
     * Accessible by OWNER or CASHIER.
     * @param startDate Optional start date for filtering.
     * @param endDate Optional end date for filtering.
     * @param cashierId Optional cashier ID for filtering.
     * @param paymentMethod Optional payment method for filtering.
     * @param productName Optional product name contained in the sale for filtering.
     * @return A list of Receipt objects.
     */
    @GetMapping // This maps to /api/sales/receipts and accepts query parameters
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_CASHIER')")
    public ResponseEntity<List<Receipt>> getReceipts(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long cashierId,
            @RequestParam(required = false) Receipt.PaymentMethod paymentMethod,
            @RequestParam(required = false) String productName) {

        System.out.println("DEBUG: ReceiptController - getReceipts (filtered/all) hit. Current Authentication: " + SecurityContextHolder.getContext().getAuthentication());
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            System.out.println("DEBUG: ReceiptController - getReceipts (filtered/all) - Authorities: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        }

        try {
            // Your existing receiptService.getFilteredReceipts method already handles null/empty parameters
            List<Receipt> filteredReceipts = receiptService.getFilteredReceipts(startDate, endDate, cashierId, paymentMethod, productName);
            return ResponseEntity.ok(filteredReceipts);
        } catch (Exception e) {
            System.err.println("Error fetching filtered receipts: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Get a specific receipt by ID.
     * This method correctly handles /api/sales/receipts/{id} paths.
     * Accessible by OWNER or CASHIER.
     * @param id The ID of the receipt.
     * @return The Receipt if found, else 404.
     */
    @GetMapping("/{id}") // This maps to /api/sales/receipts/{id}
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_CASHIER')")
    public ResponseEntity<Receipt> getReceiptById(@PathVariable Long id) {
        System.out.println("DEBUG: ReceiptController - getReceiptById(" + id + ") hit. Current Authentication: " + SecurityContextHolder.getContext().getAuthentication());
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            System.out.println("DEBUG: ReceiptController - getReceiptById(" + id + ") - Authorities: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        }
        Optional<Receipt> receipt = receiptService.getReceiptById(id);
        return receipt.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a receipt by ID.
     * Accessible only by OWNER.
     * @param id The ID of the receipt to delete.
     * @return 204 No Content on success, or 404 if not found.
     */
    @DeleteMapping("/{id}") // This maps to /api/sales/receipts/{id} for DELETE
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<Void> deleteReceipt(@PathVariable Long id) {
        System.out.println("DEBUG: ReceiptController - deleteReceipt(" + id + ") hit. Current Authentication: " + SecurityContextHolder.getContext().getAuthentication());
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            System.out.println("DEBUG: ReceiptController - deleteReceipt(" + id + ") - Authorities: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        }
        try {
            receiptService.deleteReceipt(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
