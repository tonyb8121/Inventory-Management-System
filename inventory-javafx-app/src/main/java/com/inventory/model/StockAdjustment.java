package com.inventory.model;

import java.time.LocalDateTime;

/**
 * Represents a simplified StockAdjustment data transfer object (DTO) for the frontend.
 * This class mirrors the backend StockAdjustment entity structure but does NOT contain
 * JPA annotations, as it's purely for data exchange and display in the UI.
 */
public class StockAdjustment {

    private Long id;
    private Product product; // Frontend Product model
    private int quantityChange;
    private LocalDateTime adjustmentDate;
    private String reason;
    private User adjustedBy; // Frontend User model

    // Enum for common adjustment types/reasons (must be public)
    public enum AdjustmentType {
        ADDITION,
        SUBTRACTION,
        CORRECTION
    }

    private AdjustmentType adjustmentType;

    // Constructors
    public StockAdjustment() {
    }

    public StockAdjustment(Long id, Product product, int quantityChange, LocalDateTime adjustmentDate, String reason, User adjustedBy, AdjustmentType adjustmentType) {
        this.id = id;
        this.product = product;
        this.quantityChange = quantityChange;
        this.adjustmentDate = adjustmentDate;
        this.reason = reason;
        this.adjustedBy = adjustedBy;
        this.adjustmentType = adjustmentType;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(int quantityChange) {
        this.quantityChange = quantityChange;
    }

    public LocalDateTime getAdjustmentDate() {
        return adjustmentDate;
    }

    public void setAdjustmentDate(LocalDateTime adjustmentDate) {
        this.adjustmentDate = adjustmentDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public User getAdjustedBy() {
        return adjustedBy;
    }

    public void setAdjustedBy(User adjustedBy) {
        this.adjustedBy = adjustedBy;
    }

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(AdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    @Override
    public String toString() {
        return "StockAdjustment{" +
                "id=" + id +
                ", productId=" + (product != null ? product.getId() : "N/A") +
                ", quantityChange=" + quantityChange +
                ", adjustmentDate=" + adjustmentDate +
                ", reason='" + reason + '\'' +
                ", adjustedBy=" + (adjustedBy != null ? adjustedBy.getUsername() : "N/A") +
                ", adjustmentType=" + adjustmentType +
                '}';
    }
}
