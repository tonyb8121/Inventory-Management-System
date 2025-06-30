package com.inventory.service.exception;

/**
 * Custom exception to indicate that there is insufficient stock for a product.
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }
}
