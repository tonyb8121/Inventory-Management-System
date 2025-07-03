package com.inventory.ui.controller;

import com.inventory.model.Product;
import com.inventory.model.Sale; // Keep for data structure, but won't fetch directly
import com.inventory.model.Receipt; // NEW: Import Receipt
import com.inventory.ui.util.ApiClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.chart.BarChart;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService;
import javafx.scene.control.Alert; // Ensure Alert is imported

/**
 * Controller for the Dashboard view (dashboard.fxml).
 * Displays quick statistics, low stock alerts, and product sales charts.
 */
public class DashboardController {

    @FXML private Label totalProductsValueLabel;
    @FXML private Label totalSalesValueLabel;
    @FXML private TableView<Product> lowStockTable;
    @FXML private TableColumn<Product, Long> lowStockIdColumn;
    @FXML private TableColumn<Product, String> lowStockNameColumn;
    @FXML private TableColumn<Product, Integer> lowStockQuantityColumn;
    @FXML private TableColumn<Product, Integer> lowStockMinStockColumn;
    @FXML private Label dashboardStatusLabel;

    @FXML private BarChart<String, Number> lowStockBarChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    private ApiClient apiClient;
    private ExecutorService executorService;
    private MainController mainController;

    private ObservableList<Product> lowStockProducts = FXCollections.observableArrayList();

    /**
     * Called by MainController to inject dependencies.
     */
    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }


    /**
     * Initializes the controller. This method is automatically called after the FXML file has been loaded.
     */
    @FXML
    public void initialize() {
        lowStockIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        lowStockNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        lowStockQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        lowStockMinStockColumn.setCellValueFactory(new PropertyValueFactory<>("minStockLevel"));

        lowStockQuantityColumn.setCellFactory(column -> new TableCell<Product, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                this.getStyleClass().remove("low-stock");

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                    Product product = getTableView().getItems().get(getIndex());
                    if (product != null && product.getQuantity() <= product.getMinStockLevel()) {
                        this.getStyleClass().add("low-stock");
                    }
                }
            }
        });

        lowStockTable.setItems(lowStockProducts);

        xAxis.setLabel("Product Name");
        yAxis.setLabel("Quantity");
        lowStockBarChart.setTitle("Low Stock Quantities Comparison");
        lowStockBarChart.setAnimated(true);
        lowStockBarChart.setCategoryGap(20);
    }

    /**
     * This method is called by MainController after all dependencies are injected.
     * It's safe to load dashboard data here.
     */
    public void initController() {
        System.out.println("DashboardController: initController called. Refreshing dashboard data.");
        refreshDashboard();
    }

    /**
     * Refreshes all dashboard statistics and tables.
     * This method can be called by MainController after product/sale changes.
     */
    public void refreshDashboard() {
        if (apiClient == null || executorService == null) {
            System.err.println("ERROR: ApiClient or ExecutorService is null in DashboardController.refreshDashboard().");
            dashboardStatusLabel.setText("System error: Dependencies not initialized for dashboard.");
            return;
        }

        dashboardStatusLabel.setText("Loading dashboard data...");
        executorService.submit(() -> {
            try {
                // Fetch all products for low stock and total count
                List<Product> allProducts = apiClient.getAllProducts("");
                long totalProducts = allProducts.size();
                List<Product> currentLowStockProducts = allProducts.stream()
                        .filter(p -> p.getQuantity() <= p.getMinStockLevel())
                        .sorted(Comparator.comparing(Product::getName))
                        .collect(Collectors.toList());

                // NEW: Fetch all receipts and calculate total sales from them
                List<Receipt> allReceipts = apiClient.getFilteredReceipts(null, null, null, null, null); // Get all receipts
                double totalSalesAmount = allReceipts.stream()
                        .flatMap(receipt -> receipt.getSales().stream()) // Flatten sales from all receipts
                        .mapToDouble(Sale::getTotalAmount)
                        .sum();

                Platform.runLater(() -> {
                    totalProductsValueLabel.setText(String.valueOf(totalProducts));
                    totalSalesValueLabel.setText(String.format("KSh %.2f", totalSalesAmount));

                    lowStockProducts.setAll(currentLowStockProducts);

                    updateLowStockBarChart(currentLowStockProducts);

                    dashboardStatusLabel.setText("Dashboard data loaded.");
                });

            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    dashboardStatusLabel.setText("Error loading dashboard data: " + e.getMessage());
                    showAlert("Error", "Dashboard Load Failed", "Could not load dashboard data: " + e.getMessage());
                    System.err.println("DashboardController: Error fetching data: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Updates the BarChart with low stock product quantities.
     * @param products The list of low stock products.
     */
    private void updateLowStockBarChart(List<Product> products) {
        lowStockBarChart.getData().clear();

        XYChart.Series<String, Number> currentQuantitySeries = new XYChart.Series<>();
        currentQuantitySeries.setName("Current Quantity");

        XYChart.Series<String, Number> minStockSeries = new XYChart.Series<>();
        minStockSeries.setName("Min Stock Level");

        for (Product product : products) {
            currentQuantitySeries.getData().add(new XYChart.Data<>(product.getName(), product.getQuantity()));
            minStockSeries.getData().add(new XYChart.Data<>(product.getName(), product.getMinStockLevel()));
        }

        lowStockBarChart.getData().addAll(currentQuantitySeries, minStockSeries);
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
}
