package com.inventory.ui.controller;

import com.inventory.model.Product;
import com.inventory.model.StockAdjustment;
import com.inventory.model.User; // ADDED: Import User model
import com.inventory.ui.util.ApiClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Controller for the stock-adjustment-view.fxml.
 * Handles manual stock adjustments for products and displays adjustment history.
 */
public class StockAdjustmentController {

    @FXML private ComboBox<Product> productComboBox;
    @FXML private ChoiceBox<StockAdjustment.AdjustmentType> adjustmentTypeChoiceBox;
    @FXML private TextField quantityChangeField;
    @FXML private TextArea reasonTextArea;
    @FXML private Label currentStockValueLabel;
    @FXML private Label newStockValueLabel;
    @FXML private Label statusLabel;

    @FXML private TableView<StockAdjustment> adjustmentHistoryTable;
    @FXML private TableColumn<StockAdjustment, Long> historyIdColumn;
    @FXML private TableColumn<StockAdjustment, String> historyProductColumn;
    @FXML private TableColumn<StockAdjustment, Integer> historyChangeColumn;
    @FXML private TableColumn<StockAdjustment, StockAdjustment.AdjustmentType> historyTypeColumn;
    @FXML private TableColumn<StockAdjustment, LocalDateTime> historyDateColumn;
    @FXML private TableColumn<StockAdjustment, String> historyReasonColumn;
    @FXML private TableColumn<StockAdjustment, String> historyAdjustedByColumn;

    private ApiClient apiClient;
    private ExecutorService executorService;
    private MainController mainController;

    private ObservableList<Product> availableProducts = FXCollections.observableArrayList();
    private ObservableList<StockAdjustment> adjustmentHistoryList = FXCollections.observableArrayList();

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        // --- Stock Adjustment Form Initialization ---
        // Setup product ComboBox
        productComboBox.setItems(availableProducts);
        productComboBox.setEditable(true); // Allow typing to search
        productComboBox.setConverter(new StringConverter<Product>() {
            @Override
            public String toString(Product product) {
                return product != null ? product.getName() : null;
            }

            @Override
            public Product fromString(String string) {
                String lowerCaseString = string.toLowerCase();
                return availableProducts.stream()
                        .filter(p -> p.getName().toLowerCase().contains(lowerCaseString))
                        .findFirst()
                        .orElse(null);
            }
        });
        productComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateStockLabels(newVal, quantityChangeField.getText());
        });


        // Setup adjustment type ChoiceBox
        adjustmentTypeChoiceBox.getItems().setAll(StockAdjustment.AdjustmentType.values());
        adjustmentTypeChoiceBox.getSelectionModel().selectFirst();

        // Set up quantity change field to accept only integers
        quantityChangeField.setTextFormatter(new TextFormatter<>(change -> {
            if (!change.getControlNewText().matches("\\d*")) {
                return null;
            }
            return change;
        }));
        quantityChangeField.setText("0");

        quantityChangeField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateStockLabels(productComboBox.getSelectionModel().getSelectedItem(), newVal);
        });

        currentStockValueLabel.setText("N/A");
        newStockValueLabel.setText("N/A");


        // --- Stock Adjustment History Table Initialization ---
        historyIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        historyProductColumn.setCellValueFactory(cellData -> {
            Product product = cellData.getValue().getProduct();
            return new SimpleStringProperty(product != null ? product.getName() : "N/A");
        });
        historyChangeColumn.setCellValueFactory(new PropertyValueFactory<>("quantityChange"));
        historyTypeColumn.setCellValueFactory(new PropertyValueFactory<>("adjustmentType"));
        historyDateColumn.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getAdjustmentDate();
            return new SimpleObjectProperty<>(date);
        });
        historyDateColumn.setCellFactory(tc -> new TableCell<StockAdjustment, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(formatter.format(date));
                }
            }
        });
        historyReasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));
        historyAdjustedByColumn.setCellValueFactory(cellData -> {
            User user = cellData.getValue().getAdjustedBy();
            return new SimpleStringProperty(user != null ? user.getUsername() : "N/A");
        });

        adjustmentHistoryTable.setItems(adjustmentHistoryList);
    }

    /**
     * Called by MainController after all dependencies are injected.
     * Loads initial product data and stock adjustment history.
     */
    public void initController() {
        loadProducts(""); // Load all products initially for the adjustment form
        refreshAdjustmentHistory(); // Load stock adjustment history
    }

    /**
     * Loads all products from the backend for the product selection ComboBox.
     */
    private void loadProducts(String searchQuery) {
        if (apiClient == null || executorService == null) {
            System.err.println("ERROR: ApiClient or ExecutorService is null in StockAdjustmentController.loadProducts().");
            Platform.runLater(() -> statusLabel.setText("System error: Dependencies not initialized for products."));
            return;
        }

        executorService.submit(() -> {
            try {
                List<Product> products = apiClient.getAllProducts(searchQuery);
                Platform.runLater(() -> {
                    availableProducts.setAll(products);
                    statusLabel.setText(""); // Clear status
                });
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error loading products: " + e.getMessage());
                    showAlert("Error", "Product Load Failed", "Could not load products: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Updates the current stock and projected new stock labels based on product selection
     * and quantity change input.
     */
    private void updateStockLabels(Product selectedProduct, String quantityChangeText) {
        if (selectedProduct == null) {
            currentStockValueLabel.setText("N/A");
            newStockValueLabel.setText("N/A");
            return;
        }

        currentStockValueLabel.setText(String.valueOf(selectedProduct.getQuantity()));

        int quantityChange = 0;
        try {
            quantityChange = Integer.parseInt(quantityChangeText);
        } catch (NumberFormatException e) {
            // quantityChange is 0, handled by default
        }

        int projectedNewStock = selectedProduct.getQuantity();
        StockAdjustment.AdjustmentType type = adjustmentTypeChoiceBox.getSelectionModel().getSelectedItem();

        if (type == StockAdjustment.AdjustmentType.SUBTRACTION) {
            projectedNewStock -= quantityChange;
        } else { // ADDITION or CORRECTION (assuming positive quantity change for correction if it's default behavior)
            projectedNewStock += quantityChange;
        }

        newStockValueLabel.setText(String.valueOf(projectedNewStock));

        // Highlight if projected stock is negative
        if (projectedNewStock < 0) {
            newStockValueLabel.getStyleClass().add("negative-value");
        } else {
            newStockValueLabel.getStyleClass().remove("negative-value");
        }
    }


    /**
     * Handles the "Adjust Stock" button action.
     */
    @FXML
    private void handleAdjustStock() {
        Product selectedProduct = productComboBox.getSelectionModel().getSelectedItem();
        StockAdjustment.AdjustmentType adjustmentType = adjustmentTypeChoiceBox.getSelectionModel().getSelectedItem();
        int quantityChange = 0;
        String reason = reasonTextArea.getText().trim();

        try {
            quantityChange = Integer.parseInt(quantityChangeField.getText());
        } catch (NumberFormatException e) {
            statusLabel.setText("Please enter a valid number for Quantity Change.");
            return;
        }

        // --- Basic Validation ---
        if (selectedProduct == null) {
            statusLabel.setText("Please select a product.");
            return;
        }
        if (adjustmentType == null) {
            statusLabel.setText("Please select an adjustment type.");
            return;
        }
        if (quantityChange <= 0) {
            statusLabel.setText("Quantity change must be a positive number.");
            return;
        }
        if (reason.isEmpty()) {
            statusLabel.setText("Please provide a reason for the stock adjustment.");
            return;
        }

        // Convert positive quantityChange to negative if type is SUBTRACTION
        int finalQuantityChange = quantityChange;
        if (adjustmentType == StockAdjustment.AdjustmentType.SUBTRACTION) {
            finalQuantityChange = -quantityChange;
        }


        // Construct the request DTO
        ApiClient.StockAdjustmentRequestDTO requestDTO = new ApiClient.StockAdjustmentRequestDTO(
                selectedProduct.getId(),
                finalQuantityChange,
                reason,
                adjustmentType
        );

        statusLabel.setText("Processing stock adjustment...");
        final int finalProjectedStock = selectedProduct.getQuantity() + finalQuantityChange;

        executorService.submit(() -> {
            try {
                StockAdjustment recordedAdjustment = apiClient.adjustStock(requestDTO);
                Platform.runLater(() -> {
                    statusLabel.setText("Stock adjusted successfully for " + selectedProduct.getName() + ". New Quantity: " + finalProjectedStock);
                    showAlert("Success", "Stock Adjustment Complete", "Stock for " + selectedProduct.getName() + " adjusted successfully. New Quantity: " + finalProjectedStock + ".");
                    handleClearForm(); // Clear the form after successful adjustment
                    if (mainController != null) {
                        mainController.loadProducts(""); // Refresh product list in main tab
                        mainController.refreshDashboard(); // Refresh dashboard stats
                        loadProducts(""); // Refresh the products in this controller (for updated quantities)
                        refreshAdjustmentHistory(); // Refresh the history table after an adjustment
                    }
                });
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    String errorMessage = "Error adjusting stock: " + e.getMessage();
                    statusLabel.setText(errorMessage);
                    showAlert("Error", "Stock Adjustment Failed", errorMessage);
                    System.err.println("StockAdjustmentController: Error adjusting stock via API: " + e.getMessage());
                });
            } catch (IllegalArgumentException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Validation Error: " + e.getMessage());
                    showAlert("Validation Error", "Invalid Input", e.getMessage());
                });
            }
        });
    }

    /**
     * Handles the "Clear Form" button action. Resets all input fields.
     */
    @FXML
    private void handleClearForm() {
        productComboBox.getSelectionModel().clearSelection();
        adjustmentTypeChoiceBox.getSelectionModel().selectFirst();
        quantityChangeField.setText("0");
        reasonTextArea.clear();
        currentStockValueLabel.setText("N/A");
        newStockValueLabel.setText("N/A");
        statusLabel.setText("");
    }

    /**
     * Refreshes the stock adjustment history table by fetching data from the backend.
     * This method is called upon initialization and after a successful adjustment.
     */
    @FXML // Make it FXML-callable for the refresh button
    private void refreshAdjustmentHistory() {
        if (apiClient == null || executorService == null) {
            System.err.println("ERROR: ApiClient or ExecutorService is null in StockAdjustmentController.refreshAdjustmentHistory().");
            Platform.runLater(() -> statusLabel.setText("System error: Dependencies not initialized for history."));
            return;
        }

        statusLabel.setText("Loading stock adjustment history...");
        executorService.submit(() -> {
            try {
                List<StockAdjustment> history = apiClient.getStockAdjustmentHistory();
                Platform.runLater(() -> {
                    adjustmentHistoryList.setAll(history);
                    statusLabel.setText("Stock adjustment history loaded successfully. Total records: " + history.size());
                });
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error loading adjustment history: " + e.getMessage());
                    showAlert("Error", "History Load Failed", "Could not load stock adjustment history: " + e.getMessage());
                    System.err.println("StockAdjustmentController: Error fetching adjustment history: " + e.getMessage());
                });
            }
        });
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
}
