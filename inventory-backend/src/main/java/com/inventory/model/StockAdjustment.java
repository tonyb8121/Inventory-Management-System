package com.inventory.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a manual adjustment made to a product's stock quantity.
 * This tracks additions, subtractions, or corrections.
 */
@Entity
@Table(name = "stock_adjustments")
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER) // Eager fetch as Product is central to adjustment
    @JoinColumn(name = "product_id", nullable = false)
    private Product product; // The product whose stock was adjusted

    @Column(nullable = false)
    private int quantityChange; // The change in quantity (positive for addition, negative for subtraction)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdjustmentType adjustmentType; // Type of adjustment (ADDITION, SUBTRACTION, CORRECTION)

    @Column(nullable = false, length = 500)
    private String reason; // Reason for the stock adjustment

    @ManyToOne(fetch = FetchType.EAGER) // Eager fetch as User is central to who made adjustment
    @JoinColumn(name = "adjusted_by_user_id", nullable = false)
    private User adjustedBy; // The user who performed the adjustment

    @Column(nullable = false)
    private LocalDateTime adjustmentDate; // Timestamp of the adjustment

    // Enum for Adjustment Types
    public enum AdjustmentType {
        ADDITION,
        SUBTRACTION,
        CORRECTION
    }

    // Constructors
    public StockAdjustment() {
        this.adjustmentDate = LocalDateTime.now(); // Set current timestamp by default
    }

    public StockAdjustment(Product product, int quantityChange, AdjustmentType adjustmentType, String reason, User adjustedBy) {
        this(); // Call default constructor to set adjustmentDate
        this.product = product;
        this.quantityChange = quantityChange;
        this.adjustmentType = adjustmentType;
        this.reason = reason;
        this.adjustedBy = adjustedBy;
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

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(AdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
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

    public LocalDateTime getAdjustmentDate() {
        return adjustmentDate;
    }

    public void setAdjustmentDate(LocalDateTime adjustmentDate) {
        this.adjustmentDate = adjustmentDate;
    }

    @Override
    public String toString() {
        return "StockAdjustment{" +
                "id=" + id +
                ", product=" + (product != null ? product.getName() : "N/A") +
                ", quantityChange=" + quantityChange +
                ", adjustmentType=" + adjustmentType +
                ", reason='" + reason + '\'' +
                ", adjustedBy=" + (adjustedBy != null ? adjustedBy.getUsername() : "N/A") +
                ", adjustmentDate=" + adjustmentDate +
                '}';
    }
}
