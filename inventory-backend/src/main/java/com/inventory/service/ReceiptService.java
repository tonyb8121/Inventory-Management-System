package com.inventory.service;

import com.inventory.model.Product;
import com.inventory.model.Receipt;
import com.inventory.model.Sale;
import com.inventory.model.User;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.ReceiptRepository;
import com.inventory.repository.SalesRepository;
import com.inventory.repository.UserRepository;
import com.inventory.service.exception.InsufficientStockException;
import com.inventory.service.exception.ProductNotFoundException;
import com.inventory.service.exception.ReceiptNotFoundException; // NEW: Import for ReceiptNotFoundException
import com.inventory.service.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional; // RE-ADDED: Used for Optional return types
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for managing sales receipts and related operations.
 */
@Service
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SalesRepository salesRepository;

    @Autowired
    public ReceiptService(ReceiptRepository receiptRepository, ProductRepository productRepository,
                          UserRepository userRepository, SalesRepository salesRepository) {
        this.receiptRepository = receiptRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.salesRepository = salesRepository;
    }

    /**
     * Records a batch of sales (multiple items in one transaction) and generates a receipt.
     * This operation is transactional to ensure atomicity.
     *
     * @param request The request object containing sales items and payment details.
     * @param cashierUsername The username of the cashier processing the sale.
     * @return The newly created Receipt object.
     * @throws UserNotFoundException If the cashier is not found.
     * @throws ProductNotFoundException If any product in the sale items is not found.
     * @throws InsufficientStockException If there's not enough stock for any product.
     */
    @Transactional
    public Receipt recordBatchSale(RecordReceiptRequest request, String cashierUsername) throws UserNotFoundException, ProductNotFoundException, InsufficientStockException {
        User cashier = userRepository.findByUsername(cashierUsername)
                .orElseThrow(() -> new UserNotFoundException("Cashier not found: " + cashierUsername));

        // 1. Calculate total amount and validate stock
        double totalAmount = 0.0;
        for (RecordSaleItem item : request.getSaleItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + item.getProductId()));

            if (product.getQuantity() < item.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product " + product.getName() +
                        ". Available: " + product.getQuantity() + ", Requested: " + item.getQuantity());
            }
            totalAmount += product.getPrice() * item.getQuantity();
        }

        // 2. Create Receipt
        String receiptNumber = "R" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 5);
        Receipt receipt = new Receipt(receiptNumber, cashier, totalAmount, request.getPaymentMethod(),
                request.getCashAmount(), request.getMpesaAmount(), request.getMpesaTransactionId());

        // 3. Create Sale items and update product stock
        for (RecordSaleItem item : request.getSaleItems()) {
            Product product = productRepository.findById(item.getProductId()).get(); // Already checked for existence
            Sale sale = new Sale(product, item.getQuantity(), product.getPrice());
            receipt.addSale(sale); // Add sale to receipt and link them bidirectionally

            product.setQuantity(product.getQuantity() - item.getQuantity()); // Deduct stock
            productRepository.save(product); // Save updated product stock
        }

        return receiptRepository.save(receipt); // Save the receipt (cascades to sales)
    }

    /**
     * Retrieves a list of receipts, optionally filtered by date range, cashier, payment method, or product name.
     * This method now uses a custom repository query to eagerly fetch associated sales and products.
     *
     * @param startDate Optional start date for filtering.
     * @param endDate Optional end date for filtering.
     * @param cashierId Optional ID of the cashier for filtering.
     * @param paymentMethod Optional payment method for filtering.
     * @param productName Optional product name (partial match) for filtering sales within receipts.
     * @return A list of filtered Receipt objects, with sales and products eagerly loaded.
     */
    @Transactional(readOnly = true)
    public List<Receipt> getFilteredReceipts(
            LocalDateTime startDate, LocalDateTime endDate,
            Long cashierId,
            Receipt.PaymentMethod paymentMethod,
            String productName) {
        return receiptRepository.findFilteredReceiptsWithSalesAndProduct(
                startDate, endDate, cashierId, paymentMethod, productName
        );
    }

    /**
     * Retrieves a single receipt by its ID.
     * @param id The ID of the receipt to retrieve.
     * @return An Optional containing the Receipt if found, or empty if not found.
     */
    @Transactional(readOnly = true)
    public Optional<Receipt> getReceiptById(Long id) {
        // This query also eager fetches sales and cashier to prevent lazy initialization issues
        return receiptRepository.findReceiptByIdWithSalesAndCashier(id);
    }

    /**
     * Deletes a receipt by its ID, and returns the stock of associated products.
     * This operation is transactional.
     * @param id The ID of the receipt to delete.
     * @throws ReceiptNotFoundException if the receipt is not found.
     * @throws ProductNotFoundException if a product associated with a sale item in the receipt is not found.
     */
    @Transactional
    public void deleteReceipt(Long id) throws ReceiptNotFoundException, ProductNotFoundException {
        // Find the receipt along with its sales to return stock
        Receipt receipt = receiptRepository.findReceiptByIdWithSalesAndCashier(id)
                .orElseThrow(() -> new ReceiptNotFoundException("Receipt not found with ID: " + id));

        // Return product quantities to stock
        if (receipt.getSales() != null) {
            for (Sale sale : receipt.getSales()) {
                Product product = productRepository.findById(sale.getProduct().getId())
                        .orElseThrow(() -> new ProductNotFoundException("Product not found for sale item with ID: " + sale.getProduct().getId()));
                product.setQuantity(product.getQuantity() + sale.getQuantity());
                productRepository.save(product);
            }
        }
        receiptRepository.delete(receipt);
    }


    /**
     * Request DTO for recording a batch of sales.
     */
    public static class RecordReceiptRequest {
        private List<RecordSaleItem> saleItems;
        private Receipt.PaymentMethod paymentMethod;
        private double cashAmount;
        private double mpesaAmount;
        private String mpesaTransactionId;
        private double totalAmount; // Added totalAmount to the request DTO

        // Getters and Setters
        public List<RecordSaleItem> getSaleItems() { return saleItems; }
        public void setSaleItems(List<RecordSaleItem> saleItems) { this.saleItems = saleItems; }
        public Receipt.PaymentMethod getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(Receipt.PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
        public double getCashAmount() { return cashAmount; }
        public void setCashAmount(double cashAmount) { this.cashAmount = cashAmount; }
        public double getMpesaAmount() { return mpesaAmount; }
        public void setMpesaAmount(double mpesaAmount) { this.mpesaAmount = mpesaAmount; }
        public String getMpesaTransactionId() { return mpesaTransactionId; }
        public void setMpesaTransactionId(String mpesaTransactionId) { this.mpesaTransactionId = mpesaTransactionId; }
        public double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    }

    /**
     * DTO for an individual sale item within a RecordReceiptRequest.
     */
    public static class RecordSaleItem {
        private Long productId;
        private int quantity;

        // Getters and Setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}
