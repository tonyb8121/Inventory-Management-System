package com.inventory.model;

import java.time.LocalDateTime;

/**
 * Represents a Sale entity in the inventory system, mirroring the backend Sale model.
 * Used for data transfer and display in the JavaFX frontend.
 */
public class Sale {
    private Long id;
    private Product product; // Reference to the Product that was sold
    private int quantity;
    private double unitPrice;
    private double totalAmount;
    // Removed saleDate here, as it's now primarily on the Receipt
    // private LocalDateTime saleDate;

    private Receipt receipt; // NEW: Link to the frontend Receipt POJO

    // Default constructor for JSON deserialization
    public Sale() {
    }

    // Constructor for creating new Sale objects
    public Sale(Long id, Product product, int quantity, double unitPrice, double totalAmount, Receipt receipt) {
        this.id = id;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.receipt = receipt;
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

    // Removed get/setSaleDate

    public Receipt getReceipt() { // NEW: Getter for frontend Receipt
        return receipt;
    }

    public void setReceipt(Receipt receipt) { // NEW: Setter for frontend Receipt
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
                ", receiptId=" + (receipt != null ? receipt.getId() : "N/A") +
                '}';
    }
}
