package com.inventory.service.exception;

/**
 * Custom exception to indicate that a receipt was not found.
 */
public class ReceiptNotFoundException extends RuntimeException {
    public ReceiptNotFoundException(String message) {
        super(message);
    }

    public ReceiptNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
