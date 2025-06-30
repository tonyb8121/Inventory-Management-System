package com.inventory.service;

import com.inventory.model.Product;
import com.inventory.model.StockAdjustment;
import com.inventory.model.StockAdjustment.AdjustmentType;
import com.inventory.model.User;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.StockAdjustmentRepository;
import com.inventory.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime; // Make sure LocalDateTime is imported
import java.util.List;

/**
 * Service class for managing stock adjustments.
 * Handles applying stock changes to products and recording adjustment history.
 */
@Service
public class StockAdjustmentService {

    @Autowired
    private StockAdjustmentRepository stockAdjustmentRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * DTO for stock adjustment requests from frontend.
     * This nested class allows the API to receive a clean payload.
     */
    public static class StockAdjustmentRequest {
        private Long productId;
        private int quantityChange;
        private String reason;
        private AdjustmentType adjustmentType;

        // Getters and Setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public int getQuantityChange() { return quantityChange; }
        public void setQuantityChange(int quantityChange) { this.quantityChange = quantityChange; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public AdjustmentType getAdjustmentType() { return adjustmentType; }
        public void setAdjustmentType(AdjustmentType adjustmentType) { this.adjustmentType = adjustmentType; }
    }


    /**
     * Records a stock adjustment and updates the product quantity.
     * @param request The StockAdjustmentRequest containing details for the adjustment.
     * @return The created StockAdjustment entity.
     * @throws IllegalArgumentException if product not found or invalid quantity change/reason.
     */
    @Transactional
    public StockAdjustment adjustStock(StockAdjustmentRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + request.getProductId()));

        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Reason for stock adjustment cannot be empty.");
        }
        if (request.getQuantityChange() == 0) {
            throw new IllegalArgumentException("Quantity change cannot be zero.");
        }
        if (request.getAdjustmentType() == null) {
            throw new IllegalArgumentException("Adjustment type cannot be null.");
        }

        int currentQuantity = product.getQuantity();
        int newQuantity = currentQuantity;

        switch (request.getAdjustmentType()) {
            case ADDITION:
                if (request.getQuantityChange() < 0) {
                    throw new IllegalArgumentException("Addition quantity must be positive.");
                }
                newQuantity += request.getQuantityChange();
                break;
            case SUBTRACTION:
                if (request.getQuantityChange() > 0) {
                    throw new IllegalArgumentException("Subtraction quantity must be negative (or provide positive change and handle sign).");
                }
                newQuantity += request.getQuantityChange();
                if (newQuantity < 0) {
                    throw new IllegalArgumentException("Cannot subtract more than available stock. Current: " + currentQuantity);
                }
                break;
            case CORRECTION:
                newQuantity += request.getQuantityChange();
                if (newQuantity < 0) {
                    throw new IllegalArgumentException("Correction leads to negative stock. Resulting quantity: " + newQuantity);
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid adjustment type.");
        }

        product.setQuantity(newQuantity);
        productRepository.save(product);

        // Get the currently authenticated user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database: " + username));


        StockAdjustment adjustment = new StockAdjustment(
                product,
                request.getQuantityChange(),
                request.getAdjustmentType(),
                request.getReason(),
                currentUser
                // adjustmentDate is set by default constructor, no need to pass it here
        );

        return stockAdjustmentRepository.save(adjustment);
    }

    /**
     * Retrieves the complete history of stock adjustments.
     * @return A list of StockAdjustment entities, ordered by date descending.
     */
    @Transactional(readOnly = true)
    public List<StockAdjustment> getStockAdjustmentHistory() {
        return stockAdjustmentRepository.findAllByOrderByAdjustmentDateDesc();
    }
}
