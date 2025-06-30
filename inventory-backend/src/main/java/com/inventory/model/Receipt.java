package com.inventory.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference; // ADDED

/**
 * Represents a complete sales transaction (a "receipt").
 * A receipt can contain multiple individual sale items.
 * It tracks the overall transaction details, including total amount,
 * payment method, cashier, and timestamps.
 */
@Entity
@Table(name = "receipts")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String receiptNumber; // Unique human-readable receipt identifier

    @ManyToOne(fetch = FetchType.LAZY) // Many receipts can be managed by one cashier
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier; // The user (cashier) who processed the sale

    @Column(nullable = false)
    private LocalDateTime transactionDate; // Date and time of the transaction

    @Column(nullable = false)
    private double totalAmount; // Sum of totalAmount from all associated Sales

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod; // e.g., CASH, MPESA, MIXED

    private double cashAmount; // Amount paid by cash if paymentMethod is CASH or MIXED
    private double mpesaAmount; // Amount paid by M-Pesa if paymentMethod is MPESA or MIXED
    private String mpesaTransactionId; // M-Pesa transaction reference (e.g., from STK Push)

    // One-to-Many relationship with Sale. A Receipt can have multiple individual Sale items.
    // orphanRemoval = true: if a Sale is removed from the set, it's also removed from the database.
    // cascade = CascadeType.ALL: Operations (persist, merge, remove) on Receipt will cascade to associated Sales.
    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference // ADDED: This side will be serialized, managing the reference
    private Set<Sale> sales = new HashSet<>();

    // Enum for Payment Methods
    public enum PaymentMethod {
        CASH,
        MPESA,
        MIXED
    }

    // Constructors
    public Receipt() {
        this.transactionDate = LocalDateTime.now();
    }

    public Receipt(String receiptNumber, User cashier, double totalAmount, PaymentMethod paymentMethod, double cashAmount, double mpesaAmount, String mpesaTransactionId) {
        this(); // Call default constructor to set transactionDate
        this.receiptNumber = receiptNumber;
        this.cashier = cashier;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.cashAmount = cashAmount;
        this.mpesaAmount = mpesaAmount;
        this.mpesaTransactionId = mpesaTransactionId;
    }

    // Getters and Setters
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

    public Set<Sale> getSales() {
        return sales;
    }

    public void setSales(Set<Sale> sales) {
        this.sales = sales;
    }

    /**
     * Helper method to add a Sale to this Receipt.
     * Ensures bidirectional relationship is maintained.
     * @param sale The Sale item to add.
     */
    public void addSale(Sale sale) {
        this.sales.add(sale);
        sale.setReceipt(this); // Set the receipt on the sale item
    }

    /**
     * Helper method to remove a Sale from this Receipt.
     * Ensures bidirectional relationship is maintained.
     * @param sale The Sale item to remove.
     */
    public void removeSale(Sale sale) {
        this.sales.remove(sale);
        sale.setReceipt(null); // Remove the receipt reference from the sale item
    }

    @Override
    public String toString() {
        return "Receipt{" +
                "id=" + id +
                ", receiptNumber='" + receiptNumber + '\'' +
                ", cashier=" + (cashier != null ? cashier.getUsername() : "N/A") +
                ", transactionDate=" + transactionDate +
                ", totalAmount=" + totalAmount +
                ", paymentMethod=" + paymentMethod +
                '}';
    }
}
