package com.inventory.ui.model;

import com.inventory.model.Product; // Import the shared Product model
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Represents the shopping cart for a POS transaction.
 * Manages a list of selected products and their quantities,
 * and calculates the real-time total.
 */
public class ShoppingCart {

    // Inner class to represent a single item in the shopping cart
    public static class CartItem {
        private Product product;
        private IntegerProperty quantity;
        private DoubleProperty totalItemPrice; // Total for this specific item (quantity * unit price)

        public CartItem(Product product, int quantity) {
            this.product = product;
            this.quantity = new SimpleIntegerProperty(quantity);
            this.totalItemPrice = new SimpleDoubleProperty(product.getPrice() * quantity);

            // Listener to update totalItemPrice when quantity changes
            this.quantity.addListener((obs, oldVal, newVal) ->
                    updateTotalItemPrice()
            );
        }

        // --- Getters for Properties and Values ---
        public Product getProduct() {
            return product;
        }

        public IntegerProperty quantityProperty() {
            return quantity;
        }

        public int getQuantity() {
            return quantity.get();
        }

        public void setQuantity(int quantity) {
            this.quantity.set(quantity);
        }

        public DoubleProperty totalItemPriceProperty() {
            return totalItemPrice;
        }

        public double getTotalItemPrice() {
            return totalItemPrice.get();
        }

        // Method to recalculate totalItemPrice based on current quantity and product price
        private void updateTotalItemPrice() {
            this.totalItemPrice.set(product.getPrice() * getQuantity());
        }
    }

    private ObservableList<CartItem> items;
    private DoubleProperty grandTotal; // Total for all items in the cart

    public ShoppingCart() {
        this.items = FXCollections.observableArrayList();
        this.grandTotal = new SimpleDoubleProperty(0.0);

        // Listener to update grandTotal whenever the list of items changes
        // or when an individual item's total price changes.
        this.items.addListener((javafx.collections.ListChangeListener.Change<? extends CartItem> c) -> {
            updateGrandTotal();
            // Also add listeners to individual item's totalItemPrice property
            while (c.next()) {
                if (c.wasAdded()) {
                    c.getAddedSubList().forEach(item ->
                            item.totalItemPriceProperty().addListener((obs, oldVal, newVal) -> updateGrandTotal())
                    );
                } else if (c.wasRemoved()) {
                    // No explicit remove listener needed, as item is no longer in the list for aggregation
                }
            }
        });
    }

    /**
     * Adds a product to the cart. If the product is already in the cart, its quantity is increased.
     * @param product The product to add.
     * @param quantity The quantity to add.
     */
    public void addItem(Product product, int quantity) {
        if (product == null || quantity <= 0) {
            return; // Invalid input
        }

        boolean found = false;
        for (CartItem item : items) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.setQuantity(item.getQuantity() + quantity); // Increase quantity
                found = true;
                break;
            }
        }
        if (!found) {
            items.add(new CartItem(product, quantity)); // Add new item
        }
        updateGrandTotal(); // Ensure total is updated
    }

    /**
     * Removes an item completely from the cart.
     * @param cartItem The CartItem to remove.
     */
    public void removeItem(CartItem cartItem) {
        items.remove(cartItem);
        updateGrandTotal(); // Ensure total is updated
    }

    /**
     * Updates the quantity of a specific item in the cart.
     * If newQuantity is 0 or less, the item is removed.
     * @param cartItem The CartItem to update.
     * @param newQuantity The new quantity for the item.
     */
    public void updateItemQuantity(CartItem cartItem, int newQuantity) {
        if (newQuantity <= 0) {
            removeItem(cartItem);
        } else {
            cartItem.setQuantity(newQuantity);
        }
        updateGrandTotal(); // Ensure total is updated
    }

    /**
     * Clears all items from the shopping cart.
     */
    public void clearCart() {
        items.clear();
        updateGrandTotal(); // Reset total to 0
    }

    /**
     * Recalculates the grand total based on all items currently in the cart.
     */
    private void updateGrandTotal() {
        double sum = items.stream()
                .mapToDouble(CartItem::getTotalItemPrice)
                .sum();
        grandTotal.set(sum);
    }

    // --- Getters for Cart Properties ---
    public ObservableList<CartItem> getItems() {
        return items;
    }

    public DoubleProperty grandTotalProperty() {
        return grandTotal;
    }

    public double getGrandTotal() {
        return grandTotal.get();
    }

    /**
     * Returns true if the cart is empty, false otherwise.
     * @return boolean
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }
}
