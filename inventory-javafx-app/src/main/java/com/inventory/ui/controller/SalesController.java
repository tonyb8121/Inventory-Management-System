package com.inventory.ui.controller;

import com.inventory.model.Product;
import com.inventory.model.Sale;
import com.inventory.model.Receipt;
import com.inventory.model.User; // Import User for cashier filtering
import com.inventory.ui.util.ApiClient;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future; // For handling async task results

/**
 * Controller for the Sales History view (sales-view.fxml).
 * Displays sales records and allows deletion (with PIN authorization).
 * Now includes filtering by date range, cashier, and payment method.
 */
public class SalesController {

    @FXML private TableView<Sale> salesTable;
    @FXML private TableColumn<Sale, Long> saleIdColumn;
    @FXML private TableColumn<Sale, String> saleProductColumn;
    @FXML private TableColumn<Sale, Integer> saleQuantityColumn;
    @FXML private TableColumn<Sale, Double> saleUnitPriceColumn;
    @FXML private TableColumn<Sale, Double> saleTotalAmountColumn;
    @FXML private TableColumn<Sale, LocalDateTime> saleDateColumn;
    @FXML private TableColumn<Sale, String> saleReceiptColumn;
    @FXML private TableColumn<Sale, Void> saleActionsColumn;

    @FXML private TextField salesSearchField; // For searching sales by product name
    @FXML private Label salesStatusLabel;

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ChoiceBox<Receipt.PaymentMethod> paymentMethodChoiceBox; // For filtering by payment method
    @FXML private ComboBox<User> cashierComboBox; // For filtering by cashier

    private ApiClient apiClient;
    private ExecutorService executorService;
    private ObservableList<Sale> salesList = FXCollections.observableArrayList();
    private ObservableList<User> availableCashiers = FXCollections.observableArrayList();

    private MainController mainController;

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
        saleIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        saleProductColumn.setCellValueFactory(cellData -> {
            Product product = cellData.getValue().getProduct();
            return new SimpleStringProperty(product != null ? product.getName() : "N/A");
        });
        saleQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        saleUnitPriceColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getUnitPrice()).asObject());
        saleUnitPriceColumn.setCellFactory(tc -> new TableCell<Sale, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("KSh %.2f", price));
                }
            }
        });
        saleTotalAmountColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getTotalAmount()).asObject());
        saleTotalAmountColumn.setCellFactory(tc -> new TableCell<Sale, Double>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                if (empty || total == null) {
                    setText(null);
                } else {
                    setText(String.format("KSh %.2f", total));
                }
            }
        });

        saleReceiptColumn.setCellValueFactory(cellData -> {
            Receipt receipt = cellData.getValue().getReceipt();
            return new SimpleStringProperty(receipt != null ? receipt.getReceiptNumber() : "N/A");
        });

        saleDateColumn.setCellValueFactory(cellData -> {
            Receipt receipt = cellData.getValue().getReceipt();
            if (receipt != null && receipt.getTransactionDate() != null) {
                return new SimpleObjectProperty<>(receipt.getTransactionDate());
            }
            return new SimpleObjectProperty<>(null);
        });
        saleDateColumn.setCellFactory(tc -> new TableCell<Sale, LocalDateTime>() {
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

        saleActionsColumn.setCellFactory(param -> new TableCell<Sale, Void>() {
            private final Button deleteButton = new Button("Delete");
            {
                deleteButton.getStyleClass().add("danger-button");
                deleteButton.setOnAction(event -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    handleDeleteSale(sale);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteButton);
                }
            }
        });

        salesTable.setItems(salesList);

        // Initialize filtering controls
        // Payment Method ChoiceBox
        paymentMethodChoiceBox.getItems().add(null); // Option for "All"
        paymentMethodChoiceBox.getItems().addAll(Receipt.PaymentMethod.values());
        paymentMethodChoiceBox.getSelectionModel().selectFirst(); // Select "All" (null) by default

        // Cashier ComboBox
        cashierComboBox.setItems(availableCashiers);
        cashierComboBox.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                return user != null ? user.getUsername() : "All Cashiers";
            }

            @Override
            public User fromString(String string) {
                return availableCashiers.stream()
                        .filter(u -> u.getUsername().equalsIgnoreCase(string))
                        .findFirst()
                        .orElse(null);
            }
        });
        cashierComboBox.getItems().add(0, null); // Add "All Cashiers" option at the beginning
        cashierComboBox.getSelectionModel().selectFirst(); // Select "All Cashiers" by default
    }

    /**
     * Initializes the controller by loading initial sales data and cashiers.
     * Called by MainController after all dependencies are injected.
     */
    public void initController() {
        loadCashiers(); // Load cashiers first
        refreshSales(null, null, null, null, null); // Load all sales initially
    }

    /**
     * Handles the search action for sales by product name. This method is called by FXML.
     */
    @FXML
    private void handleSalesSearch() {
        // When searching by product name, clear other filters for a fresh search
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        paymentMethodChoiceBox.getSelectionModel().select(null);
        cashierComboBox.getSelectionModel().select(null);
        refreshSales(null, null, null, null, salesSearchField.getText());
    }

    /**
     * Handles the refresh button click for sales.
     * This method is public and has no arguments, making it suitable for FXML onAction.
     */
    @FXML
    public void handleRefreshSalesButton() {
        salesSearchField.clear(); // Clear product search field
        startDatePicker.setValue(null); // Clear date filters
        endDatePicker.setValue(null);
        paymentMethodChoiceBox.getSelectionModel().select(null); // Clear payment method filter
        cashierComboBox.getSelectionModel().select(null); // Clear cashier filter
        refreshSales(null, null, null, null, null); // Load all sales without filters
    }

    /**
     * Handles filtering sales by date range, cashier, payment method, and product name.
     * This method is called by FXML's "Filter" button.
     */
    @FXML
    private void handleFilterSales() {
        LocalDateTime startDate = (startDatePicker.getValue() != null) ? startDatePicker.getValue().atStartOfDay() : null;
        LocalDateTime endDate = (endDatePicker.getValue() != null) ? endDatePicker.getValue().atTime(LocalTime.MAX) : null;
        Long cashierId = (cashierComboBox.getSelectionModel().getSelectedItem() != null) ? cashierComboBox.getSelectionModel().getSelectedItem().getId() : null;
        Receipt.PaymentMethod paymentMethod = paymentMethodChoiceBox.getSelectionModel().getSelectedItem();
        String productName = salesSearchField.getText(); // Also include text search in general filter

        refreshSales(startDate, endDate, cashierId, paymentMethod, productName);
    }

    /**
     * Refreshes the sales data from the backend applying the given filters.
     * This method is now generalized to accept all filter parameters.
     *
     * @param startDate Optional start date for filtering.
     * @param endDate Optional end date for filtering.
     * @param cashierId Optional ID of the cashier to filter by.
     * @param paymentMethod Optional payment method to filter by.
     * @param productName Optional product name for search within receipts.
     */
    public void refreshSales(LocalDateTime startDate, LocalDateTime endDate, Long cashierId,
                             Receipt.PaymentMethod paymentMethod, String productName) {
        if (apiClient == null || executorService == null) {
            System.err.println("ERROR: ApiClient or ExecutorService is null in SalesController.refreshSales().");
            Platform.runLater(() -> salesStatusLabel.setText("System error: Dependencies not initialized for sales."));
            return;
        }

        salesStatusLabel.setText("Loading sales history...");
        executorService.submit(() -> {
            try {
                // Call the new API method with all filtering parameters
                List<Receipt> filteredReceipts = apiClient.getFilteredReceipts(
                        startDate, endDate, cashierId, paymentMethod, productName
                );

                // Extract individual sales from receipts.
                // Note: The backend's /api/sales/receipts endpoint returns Receipts,
                // but our SalesTable displays individual Sales. We need to flatten this.
                // If you want to display Receipts directly in the table, the TableView columns
                // would need to be reconfigured for Receipt properties.
                ObservableList<Sale> extractedSales = FXCollections.observableArrayList();
                for (Receipt receipt : filteredReceipts) {
                    if (receipt.getSales() != null) {
                        for (Sale sale : receipt.getSales()) {
                            sale.setReceipt(receipt); // Ensure sale has receipt reference for columns
                            extractedSales.add(sale);
                        }
                    }
                }

                Platform.runLater(() -> {
                    salesList.setAll(extractedSales);
                    salesStatusLabel.setText("Sales history loaded successfully. Total sales found: " + extractedSales.size());
                });
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    salesStatusLabel.setText("Error loading sales history: " + e.getMessage());
                    showAlert("Error", "Sales Load Failed", "Could not load sales history: " + e.getMessage());
                    System.err.println("SalesController: Error fetching sales: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Loads the list of all cashiers from the backend to populate the ComboBox.
     */
    private void loadCashiers() {
        if (apiClient == null || executorService == null) {
            System.err.println("ERROR: ApiClient or ExecutorService is null in SalesController.loadCashiers().");
            return;
        }
        executorService.submit(() -> {
            try {
                List<User> users = apiClient.getAllUsers();
                Platform.runLater(() -> {
                    // Preserve the "All Cashiers" (null) option if it exists
                    User currentSelection = cashierComboBox.getSelectionModel().getSelectedItem();
                    availableCashiers.setAll(users);
                    if (!availableCashiers.contains(null)) { // Add "All Cashiers" if not present
                        availableCashiers.add(0, null);
                    }
                    if (currentSelection == null || users.contains(currentSelection)) {
                        cashierComboBox.getSelectionModel().select(currentSelection);
                    } else {
                        cashierComboBox.getSelectionModel().selectFirst();
                    }
                });
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    System.err.println("Error loading cashiers: " + e.getMessage());
                    // Don't show alert for this, it's a minor component if it fails
                });
            }
        });
    }

    /**
     * Handles the deletion of a sale record. Requires owner role and PIN.
     * @param sale The sale to delete.
     */
    private void handleDeleteSale(Sale sale) {
        if (sale == null || sale.getReceipt() == null) {
            showAlert("No Selection", "No Sale Selected", "Please select a sale from the table to delete.");
            return;
        }

        // Check if the current user has OWNER role
        if (!apiClient.getLoggedInUserRoles().contains("OWNER")) {
            showAlert("Access Denied", "Permission Denied", "Only users with the OWNER role can delete sales records.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("PIN Authorization");
        dialog.setHeaderText("Enter Owner PIN to Authorize Deletion");
        dialog.setContentText("Please enter the owner's PIN:");
        Optional<String> pinResult = dialog.showAndWait();

        if (pinResult.isPresent() && pinResult.get().equals("ownerpin")) { // Replace "ownerpin" with actual secure PIN validation later
            salesStatusLabel.setText("Deleting receipt for sale...");
            executorService.submit(() -> {
                try {
                    // Now delete the entire receipt, which cascades to sales
                    apiClient.deleteReceipt(sale.getReceipt().getId());
                    Platform.runLater(() -> {
                        salesStatusLabel.setText("Receipt deleted successfully. Corresponding sales removed.");
                        refreshSales(null, null, null, null, null); // Refresh list after deletion
                        if (mainController != null) {
                            mainController.loadProducts(""); // Refresh product stock in product tab
                            mainController.refreshDashboard(); // Refresh dashboard stats
                        }
                    });
                } catch (IOException | InterruptedException e) {
                    Platform.runLater(() -> {
                        String errorMessage = "Error deleting receipt: " + e.getMessage();
                        salesStatusLabel.setText(errorMessage);
                        showAlert("Error", "Receipt Deletion Failed", errorMessage);
                        System.err.println("SalesController: Error deleting receipt via API: " + e.getMessage());
                    });
                }
            });
        } else if (pinResult.isPresent()) {
            showAlert("Authorization Failed", "Incorrect PIN", "The PIN entered is incorrect. Sale deletion cancelled.");
            salesStatusLabel.setText("Sale deletion cancelled: Incorrect PIN.");
        } else {
            salesStatusLabel.setText("Sale deletion cancelled by user.");
        }
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
