package com.inventory.ui.controller;

import com.inventory.model.Receipt;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.event.ActionEvent; // Import for ActionEvent

import java.text.DecimalFormat;
import java.util.Optional;

/**
 * Controller for the payment-method-dialog.fxml.
 * Handles selection of payment method, calculation of change, and input validation.
 */
public class PaymentMethodController {

    @FXML private Label totalPayableLabel;
    @FXML private ChoiceBox<Receipt.PaymentMethod> paymentMethodChoiceBox;
    @FXML private TextField cashAmountField;
    @FXML private Label changeValueLabel;
    @FXML private TextField mpesaAmountField;
    @FXML private TextField mpesaTransactionIdField;
    @FXML private Label statusLabel;

    // References to labels/fields that show/hide based on payment method
    @FXML private Label cashAmountLabel;
    @FXML private Label changeLabelText;
    @FXML private Label mpesaAmountLabel;
    @FXML private Label mpesaTransactionIdLabel;

    private double totalAmountDue;
    private Receipt.PaymentMethod selectedPaymentMethod;
    private double cashAmountPaid = 0.0;
    private double mpesaAmountPaid = 0.0;
    private String mpesaTransactionId = null;

    private final DecimalFormat currencyFormat = new DecimalFormat("KSh #,##0.00");

    /**
     * Called automatically after FXML fields are injected.
     */
    @FXML
    public void initialize() {
        // Populate the ChoiceBox with PaymentMethod enum values
        paymentMethodChoiceBox.getItems().setAll(Receipt.PaymentMethod.values());

        // Set a converter to display user-friendly names (optional, enum.toString() is often fine)
        paymentMethodChoiceBox.setConverter(new StringConverter<Receipt.PaymentMethod>() {
            @Override
            public String toString(Receipt.PaymentMethod object) {
                if (object == null) return null;
                return object.name().replace("_", " "); // e.g., "M-Pesa", "Mixed"
            }

            @Override
            public Receipt.PaymentMethod fromString(String string) {
                return Receipt.PaymentMethod.valueOf(string.replace(" ", "_").toUpperCase());
            }
        });

        // Add listener to payment method choice box
        paymentMethodChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedPaymentMethod = newVal;
            updatePaymentFieldsVisibility();
            validateInput(); // Re-validate when method changes
        });

        // Add listeners to amount fields for real-time validation/calculation
        cashAmountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) cashAmountPaid = 0.0;
            else {
                try {
                    cashAmountPaid = Double.parseDouble(newVal);
                } catch (NumberFormatException e) {
                    cashAmountPaid = 0.0; // Invalid input, treat as 0
                }
            }
            updateChangeAndValidate();
        });

        mpesaAmountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) mpesaAmountPaid = 0.0;
            else {
                try {
                    mpesaAmountPaid = Double.parseDouble(newVal);
                } catch (NumberFormatException e) {
                    mpesaAmountPaid = 0.0; // Invalid input, treat as 0
                }
            }
            updateChangeAndValidate();
        });

        mpesaTransactionIdField.textProperty().addListener((obs, oldVal, newVal) -> {
            mpesaTransactionId = newVal.trim();
            validateInput();
        });

        // Set up text formatters for numeric input fields
        cashAmountField.setTextFormatter(createDoubleTextFormatter());
        mpesaAmountField.setTextFormatter(createDoubleTextFormatter());

        // Select CASH as default
        paymentMethodChoiceBox.getSelectionModel().select(Receipt.PaymentMethod.CASH);
        updatePaymentFieldsVisibility(); // Ensure initial state is correct
    }

    /**
     * Sets the total amount due for the current transaction.
     * This method must be called from the POSController before showing the dialog.
     * @param totalAmount The total amount to be paid.
     */
    public void setTotalAmountDue(double totalAmount) {
        this.totalAmountDue = totalAmount;
        totalPayableLabel.setText(currencyFormat.format(totalAmount));
        // Initialize fields based on total for convenience (e.g., cash payment pre-fill)
        if (selectedPaymentMethod == Receipt.PaymentMethod.CASH) {
            cashAmountField.setText(String.format("%.2f", totalAmount)); // Pre-fill cash field
        }
        updateChangeAndValidate(); // Recalculate change after setting total
    }

    /**
     * Updates the visibility of payment fields based on the selected payment method.
     */
    private void updatePaymentFieldsVisibility() {
        boolean isCash = (selectedPaymentMethod == Receipt.PaymentMethod.CASH);
        boolean isMpesa = (selectedPaymentMethod == Receipt.PaymentMethod.MPESA);
        boolean isMixed = (selectedPaymentMethod == Receipt.PaymentMethod.MIXED);

        // Cash fields
        cashAmountLabel.setVisible(isCash || isMixed);
        cashAmountField.setVisible(isCash || isMixed);
        changeLabelText.setVisible(isCash || isMixed);
        changeValueLabel.setVisible(isCash || isMixed);
        if (!isCash && !isMixed) {
            cashAmountField.setText(""); // Clear field if hidden
        }

        // M-Pesa fields
        mpesaAmountLabel.setVisible(isMpesa || isMixed);
        mpesaAmountField.setVisible(isMpesa || isMixed);
        mpesaTransactionIdLabel.setVisible(isMpesa || isMixed);
        mpesaTransactionIdField.setVisible(isMpesa || isMixed);
        if (!isMpesa && !isMixed) {
            mpesaAmountField.setText(""); // Clear field if hidden
            mpesaTransactionIdField.setText("");
        }

        // Reset current amounts based on visibility
        cashAmountPaid = (isCash || isMixed) ? parseDouble(cashAmountField.getText()) : 0.0;
        mpesaAmountPaid = (isMpesa || isMixed) ? parseDouble(mpesaAmountField.getText()) : 0.0;
        mpesaTransactionId = (isMpesa || isMixed) ? mpesaTransactionIdField.getText().trim() : null;

        updateChangeAndValidate();
    }

    /**
     * Parses a string to a double, returning 0.0 on NumberFormatException.
     */
    private double parseDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Recalculates change and triggers validation.
     */
    private void updateChangeAndValidate() {
        double totalPaid = cashAmountPaid + mpesaAmountPaid;
        double change = totalPaid - totalAmountDue;
        changeValueLabel.setText(currencyFormat.format(change));

        // Highlight change if it's negative (underpaid)
        if (change < 0) {
            changeValueLabel.getStyleClass().add("negative-value");
        } else {
            changeValueLabel.getStyleClass().remove("negative-value");
        }
        validateInput();
    }

    /**
     * Validates the inputs before allowing payment processing.
     * @return true if inputs are valid, false otherwise.
     */
    public boolean validateInput() {
        statusLabel.setText(""); // Clear previous status messages
        if (selectedPaymentMethod == null) {
            statusLabel.setText("Please select a payment method.");
            return false;
        }

        double currentTotalPaid = 0.0;

        if (selectedPaymentMethod == Receipt.PaymentMethod.CASH) {
            currentTotalPaid = cashAmountPaid;
            if (cashAmountPaid < totalAmountDue) {
                statusLabel.setText("Cash amount is less than total payable.");
                return false;
            }
        } else if (selectedPaymentMethod == Receipt.PaymentMethod.MPESA) {
            currentTotalPaid = mpesaAmountPaid;
            if (mpesaAmountPaid < totalAmountDue) {
                statusLabel.setText("M-Pesa amount is less than total payable.");
                return false;
            }
            if (mpesaTransactionId == null || mpesaTransactionId.isEmpty()) {
                statusLabel.setText("M-Pesa Transaction ID is required for M-Pesa payment.");
                return false;
            }
        } else if (selectedPaymentMethod == Receipt.PaymentMethod.MIXED) {
            currentTotalPaid = cashAmountPaid + mpesaAmountPaid;
            if (currentTotalPaid < totalAmountDue) {
                statusLabel.setText("Total combined amount (Cash + M-Pesa) is less than total payable.");
                return false;
            }
            if (mpesaTransactionId == null || mpesaTransactionId.isEmpty() && mpesaAmountPaid > 0) {
                // M-Pesa ID only required if M-Pesa amount is actually used in a mixed payment
                statusLabel.setText("M-Pesa Transaction ID is required if M-Pesa amount is provided.");
                return false;
            }
        }

        // Final check that total paid covers the amount due
        if (currentTotalPaid < totalAmountDue) {
            statusLabel.setText("Total amount paid is insufficient.");
            return false;
        }

        return true;
    }

    /**
     * Returns the selected payment method.
     */
    public Receipt.PaymentMethod getSelectedPaymentMethod() {
        return selectedPaymentMethod;
    }

    /**
     * Returns the amount paid in cash.
     */
    public double getCashAmountPaid() {
        return cashAmountPaid;
    }

    /**
     * Returns the amount paid via M-Pesa.
     */
    public double getMpesaAmountPaid() {
        return mpesaAmountPaid;
    }

    /**
     * Returns the M-Pesa transaction ID.
     */
    public String getMpesaTransactionId() {
        return mpesaTransactionId;
    }

    /**
     * Helper method to create a TextFormatter for double values.
     */
    private TextFormatter<Double> createDoubleTextFormatter() {
        return new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            // Allow empty string, or a number (with optional decimal point and digits)
            // Ensures only valid numeric characters for currency
            if (newText.matches("\\d*\\.?\\d*")) {
                return change;
            }
            return null;
        });
    }
}
