package com.inventory.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonBackReference; // ADDED

/**
 * Represents an individual sale item within a larger receipt transaction.
 */
@Entity
@Table(name = "sales")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Sale implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER) // Eager fetch as Product is always needed for Sale details
    @JoinColumn(name = "product_id", nullable = false)
    private Product product; // The product sold

    private int quantity; // Quantity of the product sold in this sale
    private double unitPrice; // Price of the product at the time of sale (for historical accuracy)
    private double totalAmount; // Calculated total amount for this individual sale item

    // NEW: ManyToOne relationship with Receipt. Many sales belong to one receipt.
    @ManyToOne(fetch = FetchType.LAZY) // Lazy fetch to avoid circular dependencies and performance issues
    @JoinColumn(name = "receipt_id", nullable = false) // Foreign key column in 'sales' table
    @JsonBackReference // ADDED: This side will NOT be serialized to break the loop
    private Receipt receipt; // The receipt this sale item belongs to

    // Default constructor is required by JPA.
    public Sale() {
    }

    // Constructor with minimal fields for creating a sale item before linking to a receipt
    public Sale(Product product, int quantity, double unitPrice) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = quantity * unitPrice; // Calculate total amount for this item
    }

    // --- Getters and Setters ---
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Receipt getReceipt() { // NEW: Getter for Receipt
        return receipt;
    }

    public void setReceipt(Receipt receipt) { // NEW: Setter for Receipt
        this.receipt = receipt;
    }

    @Override
    public String toString() {
        return "Sale{" +
                "id=" + id +
                ", product=" + (product != null ? product.getName() : "N/A") +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", totalAmount=" + totalAmount +
                ", receiptId=" + (receipt != null ? receipt.getId() : "N/A") + // Include receipt ID
                '}';
    }
}
