package com.inventory.service;

import com.inventory.model.Product;
import com.inventory.model.Sale;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SalesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime; // Keep for now if other date usage
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing sales.
 * NOTE: With the introduction of ReceiptService for batch sales,
 * this service primarily handles individual sale retrieval/deletion
 * and dashboard-related aggregates. Recording new sales should now
 * go through ReceiptService.
 */
@Service
public class SalesService {

    @Autowired
    private SalesRepository salesRepository;

    @Autowired
    private ProductRepository productRepository;

    // This method is now likely deprecated by the new ReceiptService's recordBatchSale,
    // but kept for compatibility if old endpoints still call it directly.
    // In a POS system, individual sales are typically part of a larger Receipt.
    @Transactional
    public Sale recordSale(Long productId, int quantitySold) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));

        if (product.getQuantity() < quantitySold) {
            throw new RuntimeException("Insufficient stock for product: " + product.getName());
        }

        // Create the sale item
        Sale sale = new Sale();
        sale.setProduct(product);
        sale.setQuantity(quantitySold);
        sale.setUnitPrice(product.getPrice());
        sale.setTotalAmount(product.getPrice() * quantitySold);
        // Note: saleDate and receipt will be handled when linking to a Receipt in ReceiptService
        // For standalone individual sales, you might set saleDate here: sale.setSaleDate(LocalDateTime.now());

        // Update product quantity
        product.setQuantity(product.getQuantity() - quantitySold);
        productRepository.save(product);

        return salesRepository.save(sale);
    }

    @Transactional(readOnly = true)
    public List<Sale> getAllSales() {
        return salesRepository.findAll();
    }

    /**
     * Re-added: Retrieves an individual sale by its ID.
     * @param id The ID of the sale.
     * @return An Optional containing the Sale if found.
     */
    @Transactional(readOnly = true)
    public Optional<Sale> getSaleById(Long id) {
        return salesRepository.findById(id);
    }

    /**
     * Re-added: Deletes an individual sale record and refunds the stock.
     * Note: Careful consideration is needed for deleting individual sales
     * if they are part of a receipt. This method assumes it can operate
     * independently or that the associated receipt logic handles this.
     * @param id The ID of the sale to delete.
     */
    @Transactional
    public void deleteSale(Long id) {
        Sale sale = salesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found with ID: " + id));

        // Refund stock to the product
        Product product = sale.getProduct();
        if (product != null) {
            product.setQuantity(product.getQuantity() + sale.getQuantity());
            productRepository.save(product);
        }

        salesRepository.delete(sale);
    }


    @Transactional(readOnly = true)
    public double getTotalSalesAmount() {
        return salesRepository.findAll().stream()
                .mapToDouble(Sale::getTotalAmount)
                .sum();
    }

    // REMOVED: getSalesByDateRange as Sale entity no longer has 'saleDate' field.
    // Date-based queries for sales should now be handled via ReceiptService.
    /*
    @Transactional(readOnly = true)
    public List<Sale> getSalesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return salesRepository.findBySaleDateBetween(startDate, endDate);
    }
    */
}
