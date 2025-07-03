package com.inventory.ui.controller;

import com.inventory.model.Receipt;
import com.inventory.model.Sale;
import com.inventory.ui.util.ReceiptPrinter;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.print.PrinterJob;
import javafx.scene.layout.ColumnConstraints; // ADDED: Missing import
import javafx.scene.layout.Priority; // ADDED: Missing import
import javafx.scene.layout.Region; // ADDED: Missing import
import javafx.geometry.HPos; // ADDED: Missing import
import javafx.scene.control.Alert; // ADDED: Missing import
import javafx.scene.control.Alert.AlertType; // ADDED: Missing import

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

/**
 * Controller for the receipt-view.fxml, responsible for displaying a sales receipt
 * and handling print functionality.
 */
public class ReceiptController {

    @FXML private Label shopNameLabel;
    @FXML private Label shopAddressLabel;
    @FXML private Label shopPhoneLabel;
    @FXML private Label shopEmailLabel;

    @FXML private Label receiptNumberLabel;
    @FXML private Label transactionDateLabel;
    @FXML private Label cashierLabel;

    @FXML private VBox itemsVBox; // Container for dynamically added item rows

    @FXML private Label subtotalLabel;
    @FXML private Label vatLabel;
    @FXML private Label grandTotalReceiptLabel;

    @FXML private Label paymentMethodLabel;
    @FXML private HBox cashPaymentRow;
    @FXML private Label cashPaidLabel;
    @FXML private HBox changeRow;
    @FXML private Label changeReceiptLabel;
    @FXML private HBox mpesaPaymentRow;
    @FXML private Label mpesaPaidLabel;
    @FXML private HBox mpesaRefRow;
    @FXML private Label mpesaRefIdLabel;

    private Receipt receipt; // The receipt data to display
    private final DecimalFormat currencyFormat = new DecimalFormat("KSh #,##0.00");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Sets the receipt data to be displayed in the view.
     * This method must be called from the parent controller (e.g., POSController)
     * before showing the receipt dialog.
     * @param receipt The Receipt object containing transaction details.
     */
    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
        populateReceiptDetails();
    }

    /**
     * Populates the FXML elements with data from the Receipt object.
     */
    private void populateReceiptDetails() {
        if (receipt == null) {
            System.err.println("Receipt object is null in ReceiptController. Cannot populate view.");
            return;
        }

        // Shop Info (Hardcoded for now, will be dynamic in future)
        shopNameLabel.setText("Mini Shop");
        shopAddressLabel.setText("123 Main Street, Nairobi");
        shopPhoneLabel.setText("Tel: +254 7XX XXX XXX");
        shopEmailLabel.setText("Email: info@minishop.co.ke");

        // Transaction Details
        receiptNumberLabel.setText(receipt.getReceiptNumber());
        transactionDateLabel.setText(receipt.getTransactionDate().format(dateTimeFormatter));
        cashierLabel.setText(receipt.getCashier() != null ? receipt.getCashier().getUsername() : "N/A");

        // Items
        itemsVBox.getChildren().clear(); // Clear any previous items
        double subtotal = 0.0;
        if (receipt.getSales() != null) {
            for (Sale sale : receipt.getSales()) {
                subtotal += sale.getTotalAmount();
                addItemToReceiptDisplay(sale);
            }
        }
        subtotalLabel.setText(currencyFormat.format(subtotal));
        grandTotalReceiptLabel.setText(currencyFormat.format(receipt.getTotalAmount()));

        // Payment Details
        paymentMethodLabel.setText(receipt.getPaymentMethod().name().replace("_", " "));

        // Hide/show rows based on payment method
        boolean isCash = (receipt.getPaymentMethod() == Receipt.PaymentMethod.CASH || receipt.getPaymentMethod() == Receipt.PaymentMethod.MIXED);
        boolean isMpesa = (receipt.getPaymentMethod() == Receipt.PaymentMethod.MPESA || receipt.getPaymentMethod() == Receipt.PaymentMethod.MIXED);

        cashPaymentRow.setVisible(isCash);
        cashPaymentRow.setManaged(isCash); // Also manage layout
        changeRow.setVisible(isCash);
        changeRow.setManaged(isCash);

        mpesaPaymentRow.setVisible(isMpesa);
        mpesaPaymentRow.setManaged(isMpesa);
        mpesaRefRow.setVisible(isMpesa);
        mpesaRefRow.setManaged(isMpesa);


        if (isCash) {
            cashPaidLabel.setText(currencyFormat.format(receipt.getCashAmount()));
            double change = receipt.getCashAmount() + receipt.getMpesaAmount() - receipt.getTotalAmount();
            changeReceiptLabel.setText(currencyFormat.format(change));
            if (change < 0) {
                changeReceiptLabel.getStyleClass().add("negative-value");
            } else {
                changeReceiptLabel.getStyleClass().remove("negative-value");
            }
        }
        if (isMpesa) {
            mpesaPaidLabel.setText(currencyFormat.format(receipt.getMpesaAmount()));
            mpesaRefIdLabel.setText(receipt.getMpesaTransactionId() != null ? receipt.getMpesaTransactionId() : "N/A");
        }
    }

    /**
     * Dynamically adds a sale item row to the receipt display.
     */
    private void addItemToReceiptDisplay(Sale sale) {
        GridPane itemRow = new GridPane();
        itemRow.setHgap(5.0);
        itemRow.setVgap(2.0);
        itemRow.getColumnConstraints().addAll(
                new ColumnConstraints(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE, Priority.ALWAYS, HPos.LEFT, true), // Item Name
                new ColumnConstraints(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE, Priority.NEVER, HPos.RIGHT, true), // Qty
                new ColumnConstraints(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE, Priority.NEVER, HPos.RIGHT, true), // Price
                new ColumnConstraints(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE, Priority.NEVER, HPos.RIGHT, true)  // Total
        );

        Label itemName = new Label(sale.getProduct() != null ? sale.getProduct().getName() : "Unknown Product");
        itemName.setStyle("-fx-font-size: 13px;");
        Label itemQty = new Label(String.valueOf(sale.getQuantity()));
        itemQty.setStyle("-fx-font-size: 13px;");
        Label itemPrice = new Label(currencyFormat.format(sale.getUnitPrice()));
        itemPrice.setStyle("-fx-font-size: 13px;");
        Label itemTotal = new Label(currencyFormat.format(sale.getTotalAmount()));
        itemTotal.setStyle("-fx-font-size: 13px;");

        GridPane.setConstraints(itemName, 0, 0);
        GridPane.setConstraints(itemQty, 1, 0);
        GridPane.setConstraints(itemPrice, 2, 0);
        GridPane.setConstraints(itemTotal, 3, 0);

        itemRow.getChildren().addAll(itemName, itemQty, itemPrice, itemTotal);
        itemsVBox.getChildren().add(itemRow);
    }

    /**
     * Handles the Print button action.
     */
    @FXML
    private void handlePrint() {
        // The root node of the content area that contains all receipt elements
        // This is important: We want to print the VBox that contains the receipt details, not the DialogPane itself.
        VBox receiptContent = (VBox) shopNameLabel.getParent();
        if (receiptContent == null) {
            System.err.println("Could not find receipt content VBox for printing.");
            showAlert("Print Error", "Content Missing", "Could not find content to print.");
            return;
        }
        ReceiptPrinter.printNode(receiptContent);
    }

    /**
     * Handles the Close button action.
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) shopNameLabel.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
