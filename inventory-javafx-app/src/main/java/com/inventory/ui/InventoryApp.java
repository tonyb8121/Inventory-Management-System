package com.inventory.ui;

import com.inventory.ui.controller.LoginController;
import com.inventory.ui.controller.MainController;
import com.inventory.ui.util.ApiClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService; // ADDED: Import for ScheduledExecutorService

/**
 * The main application class for the Inventory Management System.
 * This class sets up the primary stage and manages the main application flow.
 */
public class InventoryApp extends Application {

    private ApiClient apiClient;
    private ScheduledExecutorService executorService; // MODIFIED: Changed to ScheduledExecutorService
    private Stage primaryStage; // Reference to the primary stage
    private MainController mainController; // Reference to the main controller

    @Override
    public void init() throws Exception {
        super.init();
        apiClient = new ApiClient();
        // Create a fixed thread pool for all async operations in the UI
        executorService = Executors.newScheduledThreadPool(4); // MODIFIED: Changed to newScheduledThreadPool
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Mini Shop Inventory System"); // MODIFIED: Updated title

        // Load the CSS file
        String cssPath = Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm();

        // Initially show the login view
        showLoginView(cssPath);
    }

    /**
     * Displays the login view.
     * @param cssPath Path to the CSS stylesheet.
     * @throws IOException If the FXML file cannot be loaded.
     */
    private void showLoginView(String cssPath) throws IOException {
        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
        Parent loginRoot = loginLoader.load();
        LoginController loginController = loginLoader.getController();
        loginController.setApiClient(apiClient);
        loginController.setExecutorService(executorService);
        loginController.setPrimaryStage(primaryStage); // Pass stage to controller if needed for closing
        loginController.setOnLoginSuccess(this::showMainView); // Set callback for successful login

        Scene loginScene = new Scene(loginRoot);
        loginScene.getStylesheets().add(cssPath);
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    /**
     * Displays the main application view after successful login.
     * This method is called as a callback from LoginController.
     */
    private void showMainView() {
        // Ensure this runs on the JavaFX Application Thread
        Platform.runLater(() -> {
            try {
                FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
                Parent mainRoot = mainLoader.load();
                mainController = mainLoader.getController();

                // Inject dependencies into MainController
                mainController.setApiClient(apiClient);
                mainController.setExecutorService(executorService);

                // Sales and Dashboard controllers are now loaded *within* MainController's initController
                // Their references are set there.

                Scene mainScene = new Scene(mainRoot);
                mainScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());
                primaryStage.setScene(mainScene);
                primaryStage.show();

                // Initialize the main controller to load its sub-views and initial data
                // This must be called AFTER the scene is set on the stage.
                mainController.initController();

            } catch (IOException e) {
                System.err.println("CRITICAL ERROR: Failed to load main view FXML or its dependencies.");
                System.err.println("Exception: " + e.getMessage());
                e.printStackTrace(); // Print full stack trace for debugging
                // Optionally show an alert to the user
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Application Error");
                alert.setHeaderText("Failed to Load Main Application");
                alert.setContentText("The main application interface could not be loaded due to an internal error. Please check the console for details.");
                alert.showAndWait();
                System.exit(1); // Exit the application
            } catch (Exception e) { // Catch any other unexpected exceptions during main view initialization
                System.err.println("CRITICAL UNEXPECTED ERROR during main view initialization.");
                System.err.println("Exception: " + e.getMessage());
                e.printStackTrace(); // Print full stack trace for debugging
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Application Error");
                alert.setHeaderText("Unexpected Application Error");
                alert.setContentText("An unexpected error occurred while loading the main application interface. Please check the console for details.");
                alert.showAndWait();
                System.exit(1); // Exit the application
            }
        });
    }

    @Override
    public void stop() throws Exception {
        if (executorService != null) {
            executorService.shutdownNow(); // Attempt to shut down all active tasks
        }
        // Ensure MainController's internal executor is also shut down if it was initialized
        if (mainController != null) {
            mainController.shutdownExecutor();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
