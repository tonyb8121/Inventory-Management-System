package com.inventory.ui.controller;

import com.inventory.model.Product;
import com.inventory.ui.util.ApiClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService; // ADDED: Import for ScheduledExecutorService

/**
 * Main controller for the Inventory Management System's primary interface (main-view.fxml).
 * Manages different tabs (Products, Sales, Dashboard, POS, Stock Adjustments, User Management)
 * and their respective controllers.
 */
public class MainController {

    @FXML private TabPane mainTabPane;
    @FXML private Tab productsTab;
    @FXML private Tab salesTab;
    @FXML private Tab dashboardTab;
    @FXML private Tab posTab;
    @FXML private Tab stockAdjustmentTab; // NEW: Stock Adjustment Tab
    @FXML private Tab userManagementTab; // NEW: User Management Tab

    // Product Tab FXML elements
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Long> idColumn;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> descriptionColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, Integer> quantityColumn;
    @FXML private TableColumn<Product, Integer> minStockLevelColumn;
    @FXML private TableColumn<Product, Void> actionsColumn; // For in-table Edit/Delete buttons
    @FXML private TextField searchField;
    @FXML private Label productStatusLabel;

    private ApiClient apiClient;
    private ScheduledExecutorService executorService; // MODIFIED: Changed to ScheduledExecutorService
    private ObservableList<Product> productList = FXCollections.observableArrayList();

    // Controllers for nested FXMLs
    private SalesController salesController;
    private DashboardController dashboardController;
    private POSController posController;
    private StockAdjustmentController stockAdjustmentController;
    private UserManagementController userManagementController;


    /**
     * Sets the API client. This is injected from InventoryApp.
     * @param apiClient The API client instance.
     */
    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Sets the ExecutorService. This is injected from InventoryApp.
     * @param executorService The ExecutorService instance.
     */
    public void setExecutorService(ScheduledExecutorService executorService) { // MODIFIED: Changed parameter type
        this.executorService = executorService;
    }

    /**
     * Initializes the controller. This method is automatically called after the FXML file has been loaded.
     */
    @FXML
    public void initialize() {
        // Initialize product table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        minStockLevelColumn.setCellValueFactory(new PropertyValueFactory<>("minStockLevel"));

        // Custom cell factory for price column to format as currency
        priceColumn.setCellFactory(tc -> new TableCell<Product, Double>() {
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

        // Add action buttons to the 'Actions' column (Edit, Delete) for in-table actions
        actionsColumn.setCellFactory(param -> new TableCell<Product, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");

            {
                editButton.getStyleClass().add("edit-button");
                deleteButton.getStyleClass().add("danger-button");

                editButton.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    showProductForm(product); // Directly call helper method for editing
                });

                deleteButton.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    handleDeleteProductHelper(product); // Directly call helper method for deletion
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox pane = new HBox(5, editButton, deleteButton);
                    setGraphic(pane);
                }
            }
        });

        productTable.setItems(productList);

        // Listener for tab selection change to trigger data loading for specific tabs
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if (newTab == salesTab && salesController != null) {
                salesController.initController();
            } else if (newTab == dashboardTab && dashboardController != null) {
                dashboardController.initController();
            } else if (newTab == productsTab) {
                loadProducts(""); // Refresh products when product tab is selected
            } else if (newTab == posTab && posController != null) {
                posController.initController();
            } else if (newTab == stockAdjustmentTab && stockAdjustmentController != null) {
                stockAdjustmentController.initController();
            } else if (newTab == userManagementTab && userManagementController != null) {
                userManagementController.initController();
            }
        });
    }

    /**
     * This method is called by InventoryApp after all dependencies are injected.
     * It's safe to load initial data and nested FXMLs here.
     */
    public void initController() {
        System.out.println("MainController: initController called. Loading initial product data.");
        // Load initial data for the Products tab
        loadProducts("");

        // Load nested FXMLs for other tabs
        loadSalesView();
        loadDashboardView();
        loadPOSView();
        loadStockAdjustmentView();
        loadUserManagementView();

        // Determine initial tab visibility based on roles
        updateTabVisibility();
    }

    /**
     * Updates the visibility of tabs based on the logged-in user's roles.
     */
    private void updateTabVisibility() {
        boolean isOwner = apiClient.getLoggedInUserRoles().contains("OWNER");
        boolean isCashier = apiClient.getLoggedInUserRoles().contains("CASHIER");

        // POS tab is visible for both Owner and Cashier
        posTab.setDisable(!(isOwner || isCashier));

        // Products tab visible for both Owner and Cashier (cashier can view products)
        productsTab.setDisable(!(isOwner || isCashier));

        // Sales tab visible for both Owner and Cashier
        salesTab.setDisable(!(isOwner || isCashier));

        // Dashboard visible for both Owner and Cashier
        dashboardTab.setDisable(!(isOwner || isCashier));

        // Stock Adjustments tab is OWNER only
        stockAdjustmentTab.setDisable(!isOwner);

        // User Management tab is OWNER only
        userManagementTab.setDisable(!isOwner);

        // If a cashier logs in, automatically select the POS tab
        if (isCashier && !isOwner) {
            mainTabPane.getSelectionModel().select(posTab);
        } else {
            // Default to Products tab for owner or if no specific role for POS
            mainTabPane.getSelectionModel().select(productsTab);
        }
    }


    /**
     * Loads products from the backend based on search query.
     * @param searchQuery The query to filter products.
     */
    public void loadProducts(String searchQuery) {
        if (apiClient == null || executorService == null) {
            System.err.println("ERROR: ApiClient or ExecutorService is null in MainController.loadProducts().");
            Platform.runLater(() -> productStatusLabel.setText("System error: Dependencies not initialized for products."));
            return;
        }

        productStatusLabel.setText("Loading products...");
        executorService.submit(() -> {
            try {
                List<Product> products = apiClient.getAllProducts(searchQuery);
                Platform.runLater(() -> {
                    productList.setAll(products); // Update ObservableList
                    productTable.refresh(); // Ensure table view updates
                    productStatusLabel.setText("Products loaded successfully. Total: " + products.size());
                    // After products are loaded, also refresh low stock in dashboard
                    if (dashboardController != null) {
                        dashboardController.refreshDashboard();
                    }
                });
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    productStatusLabel.setText("Error loading products: " + e.getMessage());
                    // This is line 334 in your error. showAlert is a void method, which is fine here.
                    // The error you got ("incompatible types: void cannot be converted to java.util.Optional<javafx.scene.control.ButtonType>")
                    // likely came from the 'showProductForm' method trying to assign the result of dialogStage.showAndWait().
                    // The fix below for showProductForm should address that specific error.
                    showAlert("Error", "Product Load Failed", "Could not load products: " + e.getMessage());
                    System.err.println("MainController: Error fetching products: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Handles product search action.
     */
    @FXML
    private void handleSearch() {
        loadProducts(searchField.getText());
    }

    /**
     * Handles product refresh action.
     */
    @FXML
    private void handleRefresh() {
        searchField.clear();
        loadProducts("");
    }

    /**
     * Handles the "Add Product" button action directly from FXML.
     */
    @FXML
    private void handleAddProduct() {
        showProductForm(null); // Call the generic form with null for a new product
    }

    /**
     * Handles the "Edit Product" button action directly from FXML.
     * Gets the selected product and opens the form for editing.
     */
    @FXML
    private void handleEditProduct() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            showProductForm(selectedProduct);
        } else {
            showAlert("No Product Selected", "Please Select a Product", "Please select a product from the table to edit.");
        }
    }

    /**
     * Handles the "Delete Product" button action directly from FXML.
     * Gets the selected product and initiates the deletion process.
     */
    @FXML
    private void handleDeleteProduct() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            handleDeleteProductHelper(selectedProduct);
        } else {
            showAlert("No Product Selected", "Please Select a Product", "Please select a product from the table to delete.");
        }
    }


    /**
     * Displays the product form for adding a new product or editing an existing one.
     * This is an internal helper method. It now handles the API call based on dialog result.
     * @param product The product to edit, or null for a new product.
     */
    private void showProductForm(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/product-form.fxml"));
            DialogPane dialogPane = loader.load();
            ProductFormController controller = loader.getController();

            // Pass dependencies to the form controller
            controller.setApiClient(apiClient);
            controller.setExecutorService(executorService);
            controller.setProduct(product); // Pass product to controller (null for new, object for edit)
            controller.initCategories(); // Initialize categories after product is set, but before show

            // Create a Dialog instead of a Stage
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle(product == null ? "Add New Product" : "Edit Product");
            dialog.initOwner(mainTabPane.getScene().getWindow());

            // The DialogPane already has ButtonTypes defined (OK_DONE, CANCEL_CLOSE)
            // No need to explicitly set Scene or Stage on controller if dialog handles it
            // The ProductFormController's setDialogStage can be updated to accept Dialog type if needed for internal alerts.
            // For now, we'll keep it as Stage as it's cast internally to Stage.

            // Show the dialog and wait for it to close
            Optional<ButtonType> result = dialog.showAndWait(); // This will correctly return Optional<ButtonType>

            // Check which button was clicked and if it's the "Apply" button type
            if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                Product productToSave = controller.getProductData(); // Get the prepared product data

                if (productToSave != null) { // If product data is valid from the form
                    productStatusLabel.setText("Saving product...");
                    executorService.submit(() -> {
                        try {
                            Product savedProduct = apiClient.saveProduct(productToSave); // Perform the save
                            Platform.runLater(() -> {
                                productStatusLabel.setText("Product '" + savedProduct.getName() + "' saved successfully.");
                                showAlert("Success", "Product Saved", "Product '" + savedProduct.getName() + "' saved successfully!");
                                loadProducts(""); // Refresh the product list
                                refreshDashboard(); // Refresh dashboard data as product data may have changed
                            });
                        } catch (IOException | InterruptedException e) {
                            Platform.runLater(() -> {
                                String errorMessage = "Failed to save product: " + e.getMessage();
                                productStatusLabel.setText(errorMessage);
                                showAlert("Error", "Product Save Failed", errorMessage);
                                System.err.println("MainController: Error saving product: " + e.getMessage());
                            });
                        }
                    });
                } else {
                    // Product data was invalid (validation message already shown by ProductFormController)
                    productStatusLabel.setText("Product save cancelled due to invalid input.");
                }
            } else {
                productStatusLabel.setText("Product operation cancelled.");
            }

        } catch (IOException e) {
            System.err.println("Error loading product form FXML: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Form Load Failed", "Could not load product form: " + e.getMessage());
        }
    }


    /**
     * Internal helper method to handle product deletion. Requires confirmation.
     * @param product The product to delete.
     */
    private void handleDeleteProductHelper(Product product) {
        if (product == null) {
            showAlert("No Selection", "No Product Selected", "Please select a product from the table to delete.");
            return;
        }

        // Only OWNER can delete products
        if (!apiClient.getLoggedInUserRoles().contains("OWNER")) {
            showAlert("Access Denied", "Permission Denied", "Only users with the OWNER role can delete products.");
            return;
        }

        Optional<ButtonType> result = showAlertConfirmation("Confirm Delete", "Delete Product: " + product.getName(),
                "Are you sure you want to delete this product? This action cannot be undone.");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            productStatusLabel.setText("Deleting product...");
            executorService.submit(() -> {
                try {
                    apiClient.deleteProduct(product.getId());
                    Platform.runLater(() -> {
                        productStatusLabel.setText("Product '" + product.getName() + "' deleted successfully.");
                        loadProducts(""); // Refresh list after deletion
                    });
                } catch (IOException | InterruptedException e) {
                    Platform.runLater(() -> {
                        String errorMessage = "Error deleting product: " + e.getMessage();
                        productStatusLabel.setText(errorMessage);
                        showAlert("Error", "Product Deletion Failed", errorMessage);
                        System.err.println("MainController: Error deleting product: " + e.getMessage());
                    });
                }
            });
        } else {
            productStatusLabel.setText("Product deletion cancelled.");
        }
    }

    /**
     * Loads the sales history view into its tab.
     */
    private void loadSalesView() {
        try {
            FXMLLoader salesLoader = new FXMLLoader(getClass().getResource("/fxml/sales-view.fxml"));
            Parent salesRoot = salesLoader.load();
            salesController = salesLoader.getController();
            salesController.setApiClient(apiClient);
            salesController.setExecutorService(executorService);
            salesController.setMainController(this);

            salesTab.setContent(salesRoot);

        } catch (IOException e) {
            System.err.println("Error loading sales view FXML: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> salesTab.setContent(new Label("Failed to load Sales History. " + e.getMessage())));
        }
    }

    /**
     * Loads the dashboard view into its tab.
     */
    private void loadDashboardView() {
        try {
            FXMLLoader dashboardLoader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent dashboardRoot = dashboardLoader.load();
            dashboardController = dashboardLoader.getController();
            dashboardController.setApiClient(apiClient);
            dashboardController.setExecutorService(executorService);
            dashboardController.setMainController(this);

            dashboardTab.setContent(dashboardRoot);

        } catch (IOException e) {
            System.err.println("Error loading dashboard view FXML: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> dashboardTab.setContent(new Label("Failed to load Dashboard. " + e.getMessage())));
        }
    }

    /**
     * Loads the POS view into its tab.
     */
    private void loadPOSView() {
        try {
            FXMLLoader posLoader = new FXMLLoader(getClass().getResource("/fxml/pos-view.fxml"));
            Parent posRoot = posLoader.load();
            posController = posLoader.getController();
            posController.setApiClient(apiClient);
            posController.setExecutorService(executorService);
            posController.setMainController(this);

            posTab.setContent(posRoot);

        } catch (IOException e) {
            System.err.println("Error loading POS view FXML: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> posTab.setContent(new Label("Failed to load Point of Sale. " + e.getMessage())));
        }
    }

    /**
     * Loads the Stock Adjustment view into its tab.
     */
    private void loadStockAdjustmentView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/stock-adjustment-view.fxml"));
            Parent root = loader.load();
            stockAdjustmentController = loader.getController();
            stockAdjustmentController.setApiClient(apiClient);
            stockAdjustmentController.setExecutorService(executorService);
            stockAdjustmentController.setMainController(this);

            stockAdjustmentTab.setContent(root);

        } catch (IOException e) {
            System.err.println("Error loading stock adjustment view FXML: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> stockAdjustmentTab.setContent(new Label("Failed to load Stock Adjustments. " + e.getMessage())));
        }
    }

    /**
     * Loads the User Management view into its tab.
     */
    private void loadUserManagementView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user-management-view.fxml"));
            Parent root = loader.load();
            userManagementController = loader.getController();
            userManagementController.setApiClient(apiClient);
            userManagementController.setExecutorService(executorService);
            userManagementController.setMainController(this);

            userManagementTab.setContent(root);

        } catch (IOException e) {
            System.err.println("Error loading user management view FXML: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> userManagementTab.setContent(new Label("Failed to load User Management. " + e.getMessage())));
        }
    }


    /**
     * Public method to refresh sales data, callable by other controllers (e.g., POSController).
     * @param startDate Optional start date for filtering.
     * @param endDate Optional end date for filtering.
     * @param cashierId Optional ID of the cashier to filter by.
     * @param paymentMethod Optional payment method to filter by.
     * @param productName Optional product name for search within receipts.
     */
    public void refreshSales(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, Long cashierId,
                             com.inventory.model.Receipt.PaymentMethod paymentMethod, String productName) {
        if (salesController != null) {
            salesController.refreshSales(startDate, endDate, cashierId, paymentMethod, productName);
        }
    }

    /**
     * Public method to refresh dashboard data, callable by other controllers (e.g., POSController).
     */
    public void refreshDashboard() {
        if (dashboardController != null) {
            dashboardController.refreshDashboard();
        }
    }

    /**
     * Helper method to display a simple alert dialog.
     */
    private void showAlert(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    /**
     * Helper method to display a confirmation alert dialog.
     */
    private Optional<ButtonType> showAlertConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert.showAndWait();
    }

    /**
     * Shuts down the executor service when the application stops.
     * Called by the main InventoryApp.
     */
    public void shutdownExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            System.out.println("MainController: ExecutorService shut down.");
        }
    }
}
