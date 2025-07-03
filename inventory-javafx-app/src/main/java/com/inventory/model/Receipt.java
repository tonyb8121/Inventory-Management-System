package com.inventory.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a simplified Receipt data transfer object (DTO) for the frontend.
 * This class mirrors the backend Receipt entity structure but does NOT contain
 * JPA annotations, as it's purely for data exchange and display in the UI.
 */
public class Receipt {
    private Long id;
    private String receiptNumber;
    private User cashier; // Frontend User model (simplified if needed, or just keep as is)
    private LocalDateTime transactionDate;
    private double totalAmount;
    public enum PaymentMethod { // IMPORTANT: Ensure this enum is PUBLIC
        CASH,
        MPESA,
        MIXED
    }
    private PaymentMethod paymentMethod;
    private double cashAmount;
    private double mpesaAmount;
    private String mpesaTransactionId;
    private List<Sale> sales; // List of Sale objects for this receipt

    public Receipt() {
        // Default constructor for Gson
    }

    // Constructor with all fields for convenience (optional, Gson usually doesn't need it)
    public Receipt(Long id, String receiptNumber, User cashier, LocalDateTime transactionDate, double totalAmount, PaymentMethod paymentMethod, double cashAmount, double mpesaAmount, String mpesaTransactionId, List<Sale> sales) {
        this.id = id;
        this.receiptNumber = receiptNumber;
        this.cashier = cashier;
        this.transactionDate = transactionDate;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.cashAmount = cashAmount;
        this.mpesaAmount = mpesaAmount;
        this.mpesaTransactionId = mpesaTransactionId;
        this.sales = sales;
    }

    // --- Getters and Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    // For frontend, User can be a simple representation if full User object isn't needed
    public User getCashier() {
        return cashier;
    }

    public void setCashier(User cashier) {
        this.cashier = cashier;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getCashAmount() {
        return cashAmount;
    }

    public void setCashAmount(double cashAmount) {
        this.cashAmount = cashAmount;
    }

    public double getMpesaAmount() {
        return mpesaAmount;
    }

    public void setMpesaAmount(double mpesaAmount) {
        this.mpesaAmount = mpesaAmount;
    }

    public String getMpesaTransactionId() {
        return mpesaTransactionId;
    }

    public void setMpesaTransactionId(String mpesaTransactionId) {
        this.mpesaTransactionId = mpesaTransactionId;
    }

    public List<Sale> getSales() {
        return sales;
    }

    public void setSales(List<Sale> sales) {
        this.sales = sales;
    }

    @Override
    public String toString() {
        return "Receipt{" +
                "id=" + id +
                ", receiptNumber='" + receiptNumber + '\'' +
                ", totalAmount=" + totalAmount +
                ", paymentMethod=" + paymentMethod +
                '}';
    }
}
