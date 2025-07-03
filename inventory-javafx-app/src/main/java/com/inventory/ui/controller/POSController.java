package com.inventory.ui.controller;

import com.inventory.model.Product;
import com.inventory.model.Receipt;
import com.inventory.ui.model.ShoppingCart;
import com.inventory.ui.util.ApiClient;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.scene.Scene;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Controller for the Point-of-Sale (POS) view (pos-view.fxml).
 * Handles product selection, shopping cart management, and initiating sales.
 * Now integrated with a payment method dialog and receipt printing.
 */
public class POSController {

    @FXML private TextField productSearchField;
    @FXML private ComboBox<Product> productComboBox;
    @FXML private TextField quantityField;
    @FXML private Label productStatusLabel;
    @FXML private TableView<ShoppingCart.CartItem> cartTable;
    @FXML private Label grandTotalLabel;
    @FXML private Label posStatusLabel;
    @FXML private Label selectedProductQuantityLabel;

    private ApiClient apiClient;
    private ScheduledExecutorService executorService;
    private ObservableList<Product> allProductsFromBackend; // Holds ALL products fetched from backend
    private FilteredList<Product> filteredProducts; // For filtering the ComboBox items
    private ShoppingCart shoppingCart;

    // Debounce for product search field
    private Future<?> searchDebounceFuture;

    // Track the currently selected product to maintain state consistency
    private Product currentlySelectedProduct;

    private MainController mainController;

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setExecutorService(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        allProductsFromBackend = FXCollections.observableArrayList();
        filteredProducts = new FilteredList<>(allProductsFromBackend, p -> true);
        shoppingCart = new ShoppingCart();
        currentlySelectedProduct = null;

        setupProductComboBox();
        setupSearchField();
        setupCartTable();
        setupQuantityField();

        // Initialize UI state
        clearProductSelection();
    }

    /**
     * Sets up the product ComboBox with proper filtering and selection handling
     */
    private void setupProductComboBox() {
        productComboBox.setItems(filteredProducts);
        productComboBox.setEditable(false); // Make it non-editable to avoid conflicts with search field

        productComboBox.setConverter(new StringConverter<Product>() {
            @Override
            public String toString(Product product) {
                if (product == null) {
                    return "";
                }
                return product.getName() +
                        (product.getDescription() != null && !product.getDescription().isEmpty()
                                ? " (" + product.getDescription() + ")" : "");
            }

            @Override
            public Product fromString(String string) {
                // Since ComboBox is not editable, this should not be called
                return null;
            }
        });

        // Handle ComboBox selection changes
        productComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldProduct, newProduct) -> {
            if (newProduct != null && !newProduct.equals(currentlySelectedProduct)) {
                selectProduct(newProduct);
            } else if (newProduct == null && currentlySelectedProduct != null) {
                clearProductSelection();
            }
        });
    }

    /**
     * Sets up the search field with debounced filtering
     */
    private void setupSearchField() {
        productSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Cancel previous search if still pending
            if (searchDebounceFuture != null) {
                searchDebounceFuture.cancel(true);
            }

            // Schedule new search with debounce
            searchDebounceFuture = executorService.schedule(() -> {
                Platform.runLater(() -> applyProductFilter(newValue));
            }, 300, TimeUnit.MILLISECONDS);
        });
    }

    /**
     * Sets up the cart table with all columns and event handlers
     */
    private void setupCartTable() {
        // Product column
        TableColumn<ShoppingCart.CartItem, String> productCol = new TableColumn<>("Product");
        productCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getProduct().getName() +
                        (cellData.getValue().getProduct().getDescription() != null &&
                                !cellData.getValue().getProduct().getDescription().isEmpty()
                                ? " (" + cellData.getValue().getProduct().getDescription() + ")" : "")
        ));

        // Quantity column
        TableColumn<ShoppingCart.CartItem, Integer> quantityCol = new TableColumn<>("Qty");
        quantityCol.setCellValueFactory(cellData -> cellData.getValue().quantityProperty().asObject());

        // Unit price column
        TableColumn<ShoppingCart.CartItem, Double> unitPriceCol = new TableColumn<>("Unit Price");
        unitPriceCol.setCellValueFactory(cellData ->
                new SimpleDoubleProperty(cellData.getValue().getProduct().getPrice()).asObject());
        unitPriceCol.setCellFactory(tc -> new TableCell<ShoppingCart.CartItem, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("KSh %.2f", price));
            }
        });

        // Total price column
        TableColumn<ShoppingCart.CartItem, Double> totalItemPriceCol = new TableColumn<>("Total");
        totalItemPriceCol.setCellValueFactory(cellData -> cellData.getValue().totalItemPriceProperty().asObject());
        totalItemPriceCol.setCellFactory(tc -> new TableCell<ShoppingCart.CartItem, Double>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                setText(empty || total == null ? null : String.format("KSh %.2f", total));
            }
        });

        // Actions column
        TableColumn<ShoppingCart.CartItem, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(param -> new TableCell<ShoppingCart.CartItem, Void>() {
            private final Button removeButton = new Button("Remove");
            private final Button plusButton = new Button("+");
            private final Button minusButton = new Button("-");

            {
                removeButton.getStyleClass().add("danger-button");
                removeButton.setOnAction(event -> {
                    ShoppingCart.CartItem item = getTableView().getItems().get(getIndex());
                    if (item != null) {
                        // Restore stock in frontend for removed item
                        updateFrontendProductQuantity(item.getProduct().getId(), item.getQuantity());
                        shoppingCart.removeItem(item);
                        updateCartDisplay();
                    }
                });

                plusButton.getStyleClass().add("quantity-button");
                plusButton.setOnAction(event -> {
                    ShoppingCart.CartItem item = getTableView().getItems().get(getIndex());
                    if (item != null) {
                        if (!validateStockForCartIncrease(item, 1)) return;
                        shoppingCart.updateItemQuantity(item, item.getQuantity() + 1);
                        // Deduct stock in frontend
                        updateFrontendProductQuantity(item.getProduct().getId(), -1);
                        updateCartDisplay();
                    }
                });

                minusButton.getStyleClass().add("quantity-button");
                minusButton.setOnAction(event -> {
                    ShoppingCart.CartItem item = getTableView().getItems().get(getIndex());
                    if (item != null) {
                        if (item.getQuantity() > 1) {
                            shoppingCart.updateItemQuantity(item, item.getQuantity() - 1);
                            // Restore stock in frontend
                            updateFrontendProductQuantity(item.getProduct().getId(), 1);
                        } else {
                            // If quantity becomes 0, remove the item entirely
                            // Restore stock in frontend for the full quantity removed
                            updateFrontendProductQuantity(item.getProduct().getId(), item.getQuantity());
                            shoppingCart.removeItem(item);
                        }
                        updateCartDisplay();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox pane = new HBox(5, minusButton, plusButton, removeButton);
                    setGraphic(pane);
                }
            }
        });

        cartTable.getColumns().setAll(productCol, quantityCol, unitPriceCol, totalItemPriceCol, actionsCol);
        cartTable.setItems(shoppingCart.getItems());
        grandTotalLabel.textProperty().bind(shoppingCart.grandTotalProperty().asString("KSh %.2f"));
    }

    /**
     * Sets up the quantity field with numeric input validation
     */
    private void setupQuantityField() {
        quantityField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (!newText.matches("\\d*") || newText.length() > 6) {
                return null;
            }
            return change;
        }));

        quantityField.setText("1");

        // Update quantity display when field changes
        quantityField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateQuantityDisplay();
        });
    }

    /**
     * Called by MainController to initialize data after API client is set
     */
    public void initController() {
        loadAllProductsFromBackend();
    }

    /**
     * Handles the search button click - refreshes products from backend and applies current filter
     */
    @FXML
    private void handleSearchButtonAction() {
        productStatusLabel.setText("Refreshing products...");
        // Re-fetch all products from backend, which will then re-apply filter and refresh quantities
        loadAllProductsFromBackend();
    }

    /**
     * Handles product selection from ComboBox
     */
    @FXML
    private void handleProductComboBoxSelection() {
        Product selected = productComboBox.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selectProduct(selected);
        }
    }

    /**
     * Increases quantity with validation
     */
    @FXML
    private void handleIncreaseQuantity() {
        if (currentlySelectedProduct == null) {
            showProductNotSelectedError();
            return;
        }

        int currentQty = getQuantityFieldValue();
        Product latestProduct = getLatestProductInfo(currentlySelectedProduct.getId());

        if (latestProduct == null) {
            showProductUnavailableError();
            return;
        }

        if (currentQty + 1 > latestProduct.getQuantity()) {
            showInsufficientStockError(latestProduct.getName(), latestProduct.getQuantity());
            return;
        }

        quantityField.setText(String.valueOf(currentQty + 1));
        updateQuantityDisplay();
    }

    /**
     * Decreases quantity with minimum validation
     */
    @FXML
    private void handleDecreaseQuantity() {
        if (currentlySelectedProduct == null) {
            showProductNotSelectedError();
            return;
        }

        int currentQty = getQuantityFieldValue();
        if (currentQty > 1) {
            quantityField.setText(String.valueOf(currentQty - 1));
        } else {
            quantityField.setText("1");
        }
        updateQuantityDisplay();
    }

    /**
     * Adds selected product to cart with comprehensive validation
     */
    @FXML
    private void handleAddToCart() {
        if (currentlySelectedProduct == null) {
            showProductNotSelectedError();
            return;
        }

        int quantity = getQuantityFieldValue();
        if (quantity <= 0) {
            productStatusLabel.setText("Quantity must be positive.");
            return;
        }

        Product latestProduct = getLatestProductInfo(currentlySelectedProduct.getId());
        if (latestProduct == null) {
            showProductUnavailableError();
            return;
        }

        if (latestProduct.getQuantity() < 1) {
            productStatusLabel.setText(latestProduct.getName() + " is out of stock.");
            showAlert("No Stock", "Product Out of Stock",
                    latestProduct.getName() + " is currently out of stock.");
            return;
        }

        // Check if adding this quantity would exceed available stock (considering items already in cart)
        int quantityInCartForProduct = shoppingCart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(latestProduct.getId()))
                .mapToInt(ShoppingCart.CartItem::getQuantity)
                .sum();

        if (quantityInCartForProduct + quantity > latestProduct.getQuantity()) {
            showAlert("Insufficient Stock", "Cannot add more",
                    String.format("Adding %d to current cart quantity (%d) of %s would exceed available stock (%d).",
                            quantity, quantityInCartForProduct, latestProduct.getName(), latestProduct.getQuantity()));
            productStatusLabel.setText("Cannot add more: exceeds available stock.");
            return;
        }

        // Add to cart
        shoppingCart.addItem(latestProduct, quantity);

        // IMPORTANT: Deduct quantity from the frontend's master product list immediately
        updateFrontendProductQuantity(latestProduct.getId(), -quantity);

        updateCartDisplay(); // Refresh cart table and re-evaluate quantity display

        productStatusLabel.setText(latestProduct.getName() + " added to cart.");

        // Reset selection and quantity after adding
        clearProductSelection(); // Clears ComboBox selection
        productSearchField.clear(); // Clear the search field
        quantityField.setText("1");
    }

    /**
     * Clears the shopping cart with confirmation
     */
    @FXML
    private void handleClearCart() {
        if (shoppingCart.isEmpty()) {
            posStatusLabel.setText("Cart is already empty.");
            return;
        }

        Optional<ButtonType> result = showAlertConfirmation("Clear Cart", "Confirm Clear",
                "Are you sure you want to clear the entire shopping cart?");
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Restore quantities in frontend product list before clearing cart
            for (ShoppingCart.CartItem item : shoppingCart.getItems()) {
                updateFrontendProductQuantity(item.getProduct().getId(), item.getQuantity());
            }
            shoppingCart.clearCart();
            updateCartDisplay(); // Re-filters and updates UI based on restored quantities
            posStatusLabel.setText("Shopping cart cleared.");
        }
    }

    /**
     * Processes the sale through payment dialog
     */
    @FXML
    private void handleProcessSale() {
        if (shoppingCart.isEmpty()) {
            posStatusLabel.setText("Cannot process sale: cart is empty.");
            showAlert("Empty Cart", "No Items to Sell",
                    "Please add items to the cart before processing a sale.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/payment-method-dialog.fxml"));
            DialogPane dialogPane = loader.load();
            PaymentMethodController paymentController = loader.getController();
            paymentController.setTotalAmountDue(shoppingCart.getGrandTotal());

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Process Payment");

            ButtonType processPaymentButtonType = new ButtonType("Process Payment", ButtonBar.ButtonData.OK_DONE);
            dialogPane.getButtonTypes().setAll(processPaymentButtonType, ButtonType.CANCEL);

            Button processButton = (Button) dialogPane.lookupButton(processPaymentButtonType);
            if (processButton != null) {
                processButton.getStyleClass().add("primary-button");
            }

            Optional<ButtonType> result = dialog.showAndWait();

            if (result.isPresent() && result.get() == processPaymentButtonType && paymentController.validateInput()) {
                processSaleWithPayment(paymentController);
            } else {
                posStatusLabel.setText("Payment cancelled or invalid details provided.");
            }
        } catch (IOException e) {
            String errorMessage = "Error opening payment dialog: " + e.getMessage();
            posStatusLabel.setText(errorMessage);
            showAlert("Error", "Payment Dialog Failed", errorMessage);
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Applies filter to products based on search query
     */
    private void applyProductFilter(String searchQuery) {
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            filteredProducts.setPredicate(p -> true);
        } else {
            String lowerCaseQuery = searchQuery.toLowerCase().trim();
            filteredProducts.setPredicate(product -> {
                return product.getName().toLowerCase().contains(lowerCaseQuery) ||
                        (product.getDescription() != null &&
                                product.getDescription().toLowerCase().contains(lowerCaseQuery));
            });
        }

        // Check if current selection is still valid after filtering
        if (currentlySelectedProduct != null) {
            boolean stillVisible = filteredProducts.stream()
                    .anyMatch(p -> p.getId().equals(currentlySelectedProduct.getId()));

            if (!stillVisible) {
                clearProductSelection();
            } else {
                // Ensure ComboBox shows the correct selection
                // This might not be strictly necessary as ComboBox should handle it,
                // but ensures consistency if ComboBox's internal selection logic wavers.
                productComboBox.getSelectionModel().select(currentlySelectedProduct);
            }
        }

        updateStatusAfterFilter(searchQuery);
        updateQuantityDisplay(); // Ensure display reflects potentially cleared selection or restored one
    }

    /**
     * Updates status label after filtering
     */
    private void updateStatusAfterFilter(String searchQuery) {
        if (filteredProducts.isEmpty() && searchQuery != null && !searchQuery.trim().isEmpty()) {
            productStatusLabel.setText("No products found for '" + searchQuery + "'.");
        } else if (allProductsFromBackend.isEmpty()) {
            productStatusLabel.setText("No products available.");
        } else {
            productStatusLabel.setText("Products loaded. Showing: " + filteredProducts.size() +
                    " of " + allProductsFromBackend.size());
        }
    }

    /**
     * Selects a product and updates UI accordingly
     */
    private void selectProduct(Product product) {
        currentlySelectedProduct = product;

        // Ensure ComboBox reflects the selection
        if (!product.equals(productComboBox.getSelectionModel().getSelectedItem())) {
            productComboBox.getSelectionModel().select(product);
        }

        // Reset quantity to 1 when selecting new product
        quantityField.setText("1");

        updateQuantityDisplay();
        productStatusLabel.setText("Selected: " + product.getName());
    }

    /**
     * Clears product selection and resets UI
     */
    private void clearProductSelection() {
        currentlySelectedProduct = null;
        productComboBox.getSelectionModel().clearSelection();
        selectedProductQuantityLabel.setText("");
        selectedProductQuantityLabel.setStyle("");
        productStatusLabel.setText(""); // Clear status for selection
    }

    /**
     * Updates the quantity display label based on current selection and input
     */
    private void updateQuantityDisplay() {
        if (currentlySelectedProduct == null) {
            selectedProductQuantityLabel.setText("");
            selectedProductQuantityLabel.setStyle("");
            return;
        }

        // Get the latest quantity for the currently selected product from the frontend's master list
        Product latestProduct = getLatestProductInfo(currentlySelectedProduct.getId());
        if (latestProduct == null) {
            selectedProductQuantityLabel.setText("Product unavailable.");
            selectedProductQuantityLabel.setStyle("-fx-text-fill: red;");
            clearProductSelection(); // Clear selection if product is truly gone
            return;
        }

        int inputQuantity = getQuantityFieldValue();

        if (inputQuantity > latestProduct.getQuantity()) {
            selectedProductQuantityLabel.setText("Available: " + latestProduct.getQuantity() + " (Input Exceeds!)");
            selectedProductQuantityLabel.setStyle("-fx-text-fill: red;");
        } else {
            selectedProductQuantityLabel.setText("Available: " + latestProduct.getQuantity());
            selectedProductQuantityLabel.setStyle("-fx-text-fill: #555555;");
        }
    }

    /**
     * Gets the latest product information from the frontend's allProductsFromBackend list.
     * This will reflect temporary in-cart deductions.
     */
    private Product getLatestProductInfo(Long productId) {
        return allProductsFromBackend.stream()
                .filter(p -> p.getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the quantity field value as integer, defaulting to 0 if invalid
     */
    private int getQuantityFieldValue() {
        try {
            String text = quantityField.getText().trim();
            return text.isEmpty() ? 0 : Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    /**
     * Validates stock for cart item quantity increase.
     * This checks against the frontend's current (potentially reduced) stock.
     */
    private boolean validateStockForCartIncrease(ShoppingCart.CartItem item, int increase) {
        Product latestProduct = getLatestProductInfo(item.getProduct().getId());
        if (latestProduct == null) {
            showAlert("Product Not Found", "Cannot update quantity", "The product " + item.getProduct().getName() + " is no longer available.");
            return false;
        }

        if (item.getQuantity() + increase > latestProduct.getQuantity()) {
            showAlert("Insufficient Stock", "Cannot add more",
                    "Only " + latestProduct.getQuantity() + " of " + latestProduct.getName() + " available.");
            return false;
        }
        return true;
    }

    /**
     * Updates the quantity of a product in the frontend's allProductsFromBackend list.
     * This is used for temporary in-cart stock adjustments.
     * @param productId The ID of the product to update.
     * @param quantityChange The amount to change the quantity by (positive for add, negative for deduct).
     */
    private void updateFrontendProductQuantity(Long productId, int quantityChange) {
        allProductsFromBackend.stream()
                .filter(p -> p.getId().equals(productId))
                .findFirst()
                .ifPresent(product -> {
                    int newQuantity = product.getQuantity() + quantityChange;
                    // Ensure quantity doesn't go below zero in frontend due to multiple removals,
                    // though validation should prevent this in most cases.
                    product.setQuantity(Math.max(0, newQuantity));
                });
        // After updating the underlying product quantity, refresh the filter to re-evaluate visibility
        // and ensure UI components (like quantity label) are updated.
        applyProductFilter(productSearchField.getText());
    }

    /**
     * Loads all products from the backend into allProductsFromBackend.
     * This should be called only for initial load or explicit refresh (e.g., via refresh button, or after a sale).
     * It completely overwrites any temporary frontend stock adjustments with fresh backend data.
     */
    private void loadAllProductsFromBackend() {
        if (apiClient == null || executorService == null) {
            Platform.runLater(() -> productStatusLabel.setText("System error: Dependencies not initialized."));
            return;
        }

        executorService.submit(() -> {
            try {
                List<Product> products = apiClient.getAllProducts(""); // Fetch ALL products
                Platform.runLater(() -> {
                    // Capture current selection before replacing the master list
                    Long selectedProductId = currentlySelectedProduct != null ? currentlySelectedProduct.getId() : null;

                    // Removed: shoppingCart.clearCart(); // This line was causing the issue

                    allProductsFromBackend.setAll(products); // Update the master list with fresh data

                    // Reapply current filter
                    applyProductFilter(productSearchField.getText());

                    // Restore selection if product still exists
                    if (selectedProductId != null) {
                        Product restoredProduct = getLatestProductInfo(selectedProductId);
                        if (restoredProduct != null) {
                            selectProduct(restoredProduct);
                        } else {
                            clearProductSelection(); // Product might have been deleted from backend
                        }
                    }

                    updateQuantityDisplay(); // Final update of quantity display
                    updateStatusAfterFilter(productSearchField.getText()); // Update general status label
                });
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    productStatusLabel.setText("Error loading products: " + e.getMessage());
                    showAlert("Error", "Product Load Failed", "Could not load products: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Processes sale with payment information
     */
    private void processSaleWithPayment(PaymentMethodController paymentController) {
        posStatusLabel.setText("Processing sale with payment details...");
        executorService.submit(() -> {
            try {
                // --- DEBUGGING START ---
                System.out.println("--- DEBUG: Before sending RecordReceiptRequest ---");
                System.out.println("Shopping Cart Item Count: " + shoppingCart.getItems().size());
                if (shoppingCart.getItems().isEmpty()) {
                    System.out.println("DEBUG: Shopping cart is EMPTY!");
                } else {
                    System.out.println("DEBUG: Shopping Cart Contents:");
                    shoppingCart.getItems().forEach(item ->
                            System.out.println("  Product ID: " + item.getProduct().getId() +
                                    ", Name: " + item.getProduct().getName() +
                                    ", Quantity: " + item.getQuantity())
                    );
                }
                // --- DEBUGGING END ---

                List<ApiClient.SaleItemRequestDTO> saleItems = shoppingCart.getItems().stream()
                        .map(item -> new ApiClient.SaleItemRequestDTO(item.getProduct().getId(), item.getQuantity()))
                        .collect(Collectors.toList());

                // --- DEBUGGING START ---
                System.out.println("DEBUG: Prepared SaleItemRequestDTOs for backend:");
                if (saleItems.isEmpty()) {
                    System.out.println("DEBUG: saleItems list is EMPTY after mapping!");
                } else {
                    saleItems.forEach(itemDto ->
                            System.out.println("  DTO Product ID: " + itemDto.getProductId() +
                                    ", DTO Quantity: " + itemDto.getQuantity())
                    );
                }
                System.out.println("--- DEBUG: After preparing SaleItemRequestDTOs ---");
                // --- DEBUGGING END ---

                ApiClient.RecordReceiptRequestDTO receiptRequest = new ApiClient.RecordReceiptRequestDTO(
                        saleItems,
                        paymentController.getSelectedPaymentMethod(),
                        paymentController.getCashAmountPaid(),
                        paymentController.getMpesaAmountPaid(),
                        paymentController.getMpesaTransactionId()
                );

                Receipt recordedReceipt = apiClient.recordBatchSale(receiptRequest);

                Platform.runLater(() -> {
                    posStatusLabel.setText("Sale processed successfully! Receipt No: " + recordedReceipt.getReceiptNumber());
                    shoppingCart.clearCart(); // Cart is cleared. Stock will be refreshed from backend on next `loadAllProductsFromBackend`
                    updateCartDisplay(); // Triggers a full product refresh from backend, syncing actual quantities

                    if (mainController != null) {
                        mainController.loadProducts(""); // Refresh product list in Product tab
                        mainController.refreshSales(null, null, null, null, null); // Refresh sales history
                        mainController.refreshDashboard(); // Refresh dashboard
                    }

                    showAlert("Sale Complete", "Transaction Successful",
                            "Sale recorded! Receipt Number: " + recordedReceipt.getReceiptNumber() +
                                    "\nTotal: KSh " + String.format("%.2f", recordedReceipt.getTotalAmount()));

                    // --- DEBUGGING: Confirming showReceiptPreview call ---
                    System.out.println("DEBUG: POSController - Attempting to show receipt preview...");
                    showReceiptPreview(recordedReceipt);
                    System.out.println("DEBUG: POSController - showReceiptPreview call completed.");
                });

            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    String errorMessage = "Failed to process sale: " + e.getMessage();
                    posStatusLabel.setText(errorMessage);
                    showAlert("Error", "Sale Processing Failed", errorMessage);
                });
            }
        });
    }

    /**
     * Shows receipt preview dialog
     */
    private void showReceiptPreview(Receipt receipt) {
        try {
            // --- DEBUGGING: Confirming FXML path ---
            System.out.println("DEBUG: showReceiptPreview - Loading FXML from: " + getClass().getResource("/fxml/receipt-view.fxml"));

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/receipt-view.fxml"));
            DialogPane dialogPane = loader.load();
            com.inventory.ui.controller.ReceiptController receiptController = loader.getController(); // Fully qualified name
            receiptController.setReceipt(receipt);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Sales Receipt");

            // --- DEBUGGING: Confirming dialog show call ---
            System.out.println("DEBUG: showReceiptPreview - Calling dialog.showAndWait()...");
            dialog.showAndWait();
            System.out.println("DEBUG: showReceiptPreview - dialog.showAndWait() returned.");

        } catch (IOException e) {
            System.err.println("Error loading receipt view: " + e.getMessage());
            showAlert("Receipt Error", "Cannot Display Receipt", "Failed to load receipt preview: " + e.getMessage());
        }
    }

    /**
     * Updates cart display and refreshes product data from the backend.
     * This is called after significant cart operations (add, remove, clear)
     * or after a sale to re-sync frontend stock with actual backend stock.
     */
    private void updateCartDisplay() {
        cartTable.refresh();
        // No direct call to loadAllProductsFromBackend() here anymore.
        // Frontend stock is managed locally until a full backend refresh is explicitly needed
        // (e.g., on tab switch, or after a full sale, which loadAllProductsFromBackend handles).
        updateQuantityDisplay(); // Ensure quantity label is up-to-date with current frontend stock
    }

    // ===== ERROR HANDLING METHODS =====

    private void showProductNotSelectedError() {
        productStatusLabel.setText("Please select a product first.");
    }

    private void showProductUnavailableError() {
        productStatusLabel.setText("Selected product is no longer available.");
        clearProductSelection();
    }

    private void showInsufficientStockError(String productName, int availableStock) {
        showAlert("Insufficient Stock", "Cannot increase quantity",
                "Only " + availableStock + " units of " + productName + " available.");
    }

    private void showAlert(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    private Optional<ButtonType> showAlertConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert.showAndWait();
    }
}
