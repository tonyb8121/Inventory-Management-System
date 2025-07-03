package com.inventory.ui.controller;

import com.inventory.model.Category;
import com.inventory.model.Product;
import com.inventory.ui.util.ApiClient; // Ensure ApiClient is imported
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage; // Keep Stage import for showAlert, but not for dialogStage field
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Controller for the product-form.fxml.
 * Handles gathering input for adding new products and editing existing ones.
 * It DOES NOT perform the save operation or close the dialog itself.
 * The calling controller (MainController) will handle the save and dialog closing.
 */
public class ProductFormController {

    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private TextField priceField;
    @FXML private TextField quantityField;
    @FXML private TextField minStockLevelField;
    @FXML private ChoiceBox<Category> categoryChoiceBox;
    @FXML private Label statusLabel; // For internal validation messages within the form

    private ApiClient apiClient; // Still needed for loading categories
    private ExecutorService executorService; // Still needed for loading categories
    // Removed: private Stage dialogStage; // No longer needed as dialog is managed by MainController
    private Product productToEdit; // Product object if in edit mode

    /**
     * Sets the API client.
     * @param apiClient The API client instance.
     */
    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Sets the ExecutorService.
     * @param executorService The ExecutorService instance.
     */
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Sets the dialog stage for this dialog.
     * This method is retained for compatibility with showAlert, but the controller no longer manages the dialog's title or lifecycle.
     * @param dialogStage The stage of the dialog.
     */
    public void setDialogStage(Stage dialogStage) {
        // This method is now effectively a no-op as the controller does not manage the dialog's stage lifecycle or title.
        // It might be used by internal alerts (showAlert method), but the field 'dialogStage' is removed.
        // So, this method can be safely removed or kept as a placeholder if other parts of your app implicitly rely on it,
        // although it's better to refactor alerts to not rely on a 'dialogStage' field.
        // For now, removing the field and its usage within setProduct will fix the NPE.
    }

    /**
     * Sets the product to be edited. If null, it's a new product.
     * @param product The product to edit.
     */
    public void setProduct(Product product) {
        this.productToEdit = product;
        if (product != null) {
            nameField.setText(product.getName());
            descriptionField.setText(product.getDescription());
            priceField.setText(String.valueOf(product.getPrice()));
            quantityField.setText(String.valueOf(product.getQuantity()));
            minStockLevelField.setText(String.valueOf(product.getMinStockLevel()));
            // Category selection will be handled in initCategories after items are loaded
            // Removed: dialogStage.setTitle("Edit Product"); // Title is set by MainController's Dialog
        } else {
            // Removed: dialogStage.setTitle("Add New Product"); // Title is set by MainController's Dialog
        }
    }

    /**
     * Called automatically after FXML fields are injected.
     */
    @FXML
    public void initialize() {
        // Set up number formatters for price, quantity, min stock fields
        priceField.setTextFormatter(createDoubleTextFormatter());
        quantityField.setTextFormatter(createIntegerTextFormatter());
        minStockLevelField.setTextFormatter(createIntegerTextFormatter());

        // Set up category ChoiceBox converter to display category names
        categoryChoiceBox.setConverter(new StringConverter<Category>() {
            @Override
            public String toString(Category category) {
                return category != null ? category.getName() : "";
            }

            @Override
            public Category fromString(String string) {
                return categoryChoiceBox.getItems().stream()
                        .filter(c -> c.getName().equalsIgnoreCase(string))
                        .findFirst()
                        .orElse(null);
            }
        });
    }

    /**
     * Initializes categories from backend. Must be called after ApiClient is set.
     * This also handles pre-selecting the category if in edit mode.
     */
    public void initCategories() {
        if (apiClient == null || executorService == null) {
            System.err.println("ApiClient or ExecutorService is null in ProductFormController.initCategories().");
            Platform.runLater(() -> statusLabel.setText("System error: Dependencies not initialized."));
            return;
        }

        executorService.submit(() -> {
            try {
                List<Category> categories = apiClient.getAllCategories();
                Platform.runLater(() -> {
                    categoryChoiceBox.getItems().setAll(categories);
                    // Re-select category if in edit mode and productToEdit's category is available
                    if (productToEdit != null && productToEdit.getCategory() != null) {
                        categoryChoiceBox.getSelectionModel().select(
                                categoryChoiceBox.getItems().stream()
                                        .filter(c -> c != null && c.getId().equals(productToEdit.getCategory().getId()))
                                        .findFirst()
                                        .orElse(null)
                        );
                    }
                });
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error loading categories: " + e.getMessage());
                    showAlert("Error", "Category Load Failed", "Could not load categories: " + e.getMessage());
                });
            }
        });
    }


    /**
     * Gathers data from the form fields, validates it, and returns a Product object.
     * This method is called by the MainController after the dialog is confirmed.
     *
     * @return A Product object populated with form data, or null if input is invalid.
     */
    public Product getProductData() {
        if (!isInputValid()) {
            return null; // Return null if validation fails
        }

        // Create a new Product object or update the existing one based on edit mode
        Product product = (productToEdit != null) ? productToEdit : new Product();
        product.setName(nameField.getText().trim());
        product.setDescription(descriptionField.getText().trim());
        product.setPrice(Double.parseDouble(priceField.getText()));
        product.setQuantity(Integer.parseInt(quantityField.getText()));
        product.setMinStockLevel(Integer.parseInt(minStockLevelField.getText()));
        product.setCategory(categoryChoiceBox.getSelectionModel().getSelectedItem());

        return product;
    }


    /**
     * Validates the user input in the form fields.
     * @return true if input is valid, false otherwise.
     */
    private boolean isInputValid() {
        String errorMessage = "";

        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            errorMessage += "No valid product name!\n";
        }
        if (priceField.getText() == null || priceField.getText().isEmpty()) {
            errorMessage += "No valid price!\n";
        } else {
            try {
                double price = Double.parseDouble(priceField.getText());
                if (price < 0) errorMessage += "Price cannot be negative!\n";
            } catch (NumberFormatException e) {
                errorMessage += "Price must be a number!\n";
            }
        }
        if (quantityField.getText() == null || quantityField.getText().isEmpty()) {
            errorMessage += "No valid quantity!\n";
        } else {
            try {
                int quantity = Integer.parseInt(quantityField.getText());
                if (quantity < 0) errorMessage += "Quantity cannot be negative!\n";
            } catch (NumberFormatException e) {
                errorMessage += "Quantity must be an integer!\n";
            }
        }
        if (minStockLevelField.getText() == null || minStockLevelField.getText().isEmpty()) {
            errorMessage += "No valid minimum stock level!\n";
        } else {
            try {
                int minStock = Integer.parseInt(minStockLevelField.getText());
                if (minStock < 0) errorMessage += "Minimum stock level cannot be negative!\n";
            } catch (NumberFormatException e) {
                errorMessage += "Minimum stock level must be an integer!\n";
            }
        }
        if (categoryChoiceBox.getSelectionModel().getSelectedItem() == null) {
            errorMessage += "No category selected!\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            statusLabel.setText(errorMessage);
            // This showAlert should ideally be done by the calling controller (MainController)
            // or the ProductFormController should have a way to pass this message back.
            // For now, it directly shows an alert.
            showAlert("Invalid Fields", "Please correct invalid fields", errorMessage);
            return false;
        }
    }

    /**
     * Helper method to create a TextFormatter for double values.
     */
    private TextFormatter<Double> createDoubleTextFormatter() {
        return new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*\\.?\\d*")) { // Allows digits and one decimal point
                return change;
            }
            return null;
        });
    }

    /**
     * Helper method to create a TextFormatter for integer values.
     */
    private TextFormatter<Integer> createIntegerTextFormatter() {
        return new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) { // Allows only digits
                return change;
            }
            return null;
        });
    }

    /**
     * Helper method to display a simple alert dialog.
     * Note: This method now explicitly removes the erroneous `productComboBox` reference.
     * In a Dialog scenario, alerts displayed by Platform.runLater will typically float
     * above the current active window without an explicit owner, which is acceptable
     * for this kind of internal validation alert.
     */
    private void showAlert(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            // The following lines were causing the "cannot find symbol" error as productComboBox
            // is not part of ProductFormController. They are now removed.
            // if (productComboBox != null && productComboBox.getScene() != null && productComboBox.getScene().getWindow() instanceof Stage) {
            //     alert.initOwner((Stage) productComboBox.getScene().getWindow());
            // }
            alert.showAndWait();
        });
    }
}
