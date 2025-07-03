package com.inventory.ui.util;

import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.transform.Scale;

/**
 * Utility class for printing JavaFX Nodes.
 * Provides a method to print any given Node, typically used for receipts or reports.
 */
public class ReceiptPrinter {

    /**
     * Prints a given JavaFX Node.
     * This method initiates a printer job and prints the content of the node,
     * scaling it to fit the page if necessary.
     * @param nodeToPrint The JavaFX Node (e.g., a VBox containing the receipt) to be printed.
     */
    public static void printNode(Node nodeToPrint) {
        Printer printer = Printer.getDefaultPrinter();
        if (printer == null) {
            showAlert("No Printer Found", "Printer Not Available", "Please ensure a printer is installed and set as default.");
            return;
        }

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(nodeToPrint.getScene().getWindow())) {
            PageLayout pageLayout = job.getPrinter().getDefaultPageLayout();
            double scaleX = pageLayout.getPrintableWidth() / nodeToPrint.getBoundsInParent().getWidth();
            double scaleY = pageLayout.getPrintableHeight() / nodeToPrint.getBoundsInParent().getHeight();
            double scale = Math.min(scaleX, scaleY); // Scale to fit shortest dimension, maintaining aspect ratio

            // Apply scaling to the node
            nodeToPrint.getTransforms().add(new Scale(scale, scale));

            boolean success = job.printPage(nodeToPrint);
            if (success) {
                job.endJob();
            } else {
                showAlert("Printing Error", "Job Failed", "Failed to complete print job.");
            }

            // Remove the scaling transformation after printing to restore original size
            nodeToPrint.getTransforms().clear(); // Or remove the specific scale transform
        } else {
            System.out.println("Printing cancelled or job creation failed.");
        }
    }

    private static void showAlert(String title, String header, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
