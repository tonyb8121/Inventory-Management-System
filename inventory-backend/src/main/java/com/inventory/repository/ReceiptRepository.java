package com.inventory.repository;

import com.inventory.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional; // NEW: Import Optional

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    // Custom query to fetch receipts and their associated sales, products, and cashier in one go.
    // This uses a JOIN FETCH clause to eagerly load the 'sales' and 'cashier' collections,
    // preventing LazyInitializationException and N+1 query problems when fetching filtered receipts.
    // Adjusted alias for product name to 'p.name' to match typical usage in a join.
    @Query("SELECT DISTINCT r FROM Receipt r " +
            "LEFT JOIN FETCH r.sales s " +
            "LEFT JOIN FETCH s.product p " + // Ensure product is also fetched for filtering by product name
            "LEFT JOIN FETCH r.cashier c WHERE " +
            "(:startDate IS NULL OR r.transactionDate >= :startDate) AND " +
            "(:endDate IS NULL OR r.transactionDate <= :endDate) AND " +
            "(:cashierId IS NULL OR r.cashier.id = :cashierId) AND " +
            "(:paymentMethod IS NULL OR r.paymentMethod = :paymentMethod) AND " +
            "(:productName IS NULL OR p.name LIKE %:productName%)") // Changed 's.product.name' to 'p.name'
    List<Receipt> findFilteredReceiptsWithSalesAndProduct(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("cashierId") Long cashierId,
            @Param("paymentMethod") Receipt.PaymentMethod paymentMethod,
            @Param("productName") String productName
    );

    // NEW: Query to fetch a single receipt by ID, eagerly fetching its sales and cashier
    @Query("SELECT r FROM Receipt r LEFT JOIN FETCH r.sales s LEFT JOIN FETCH r.cashier c WHERE r.id = :id")
    Optional<Receipt> findReceiptByIdWithSalesAndCashier(@Param("id") Long id);

}
