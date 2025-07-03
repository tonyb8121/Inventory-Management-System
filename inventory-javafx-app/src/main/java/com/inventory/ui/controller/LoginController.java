package com.inventory.ui.controller;

import com.inventory.ui.util.ApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Executors; // For simple internal executor if not passed from main app

/**
 * Controller for the login-view.fxml.
 * Handles user authentication.
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;

    private ApiClient apiClient;
    private ExecutorService executorService;
    private Stage primaryStage; // Reference to the primary stage
    private Runnable onLoginSuccess; // Callback for successful login

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
     * Sets the primary stage of the application.
     * @param primaryStage The main application stage.
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * Sets the callback to be executed on successful login.
     * @param onLoginSuccess Runnable to execute.
     */
    public void setOnLoginSuccess(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    @FXML
    public void initialize() {
        // Optional: Pre-fill for quick testing
        // usernameField.setText("owner");
        // passwordField.setText("password");
    }

    /**
     * Handles the login button action.
     */

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty.");
            return;
        }

        statusLabel.setText("Logging in...");
        loginButton.setDisable(true); // Disable button to prevent multiple clicks

        executorService.submit(() -> {
            try {
                // Call the API client to perform login
                Map<String, Object> loginResponse = apiClient.login(username, password);
                String jwt = (String) loginResponse.get("jwt");
                String loggedInUsername = (String) loginResponse.get("username");

                @SuppressWarnings("unchecked")
                List<String> rolesList = (List<String>) loginResponse.get("roles");

                // 🔐 DEBUG LOGGING
                System.out.println("✅ Login successful");
                System.out.println("🔐 JWT token: " + jwt);
                System.out.println("👤 Username: " + loggedInUsername);
                System.out.println("🎭 Roles: " + rolesList);

                // Store the JWT and roles for future API use
                apiClient.setJwtToken(jwt);
                apiClient.setLoggedInUserRoles(rolesList);

                Platform.runLater(() -> {
                    statusLabel.setText("Login successful! Welcome, " + loggedInUsername + ".");
                    if (onLoginSuccess != null) {
                        onLoginSuccess.run();
                    }
                    if (primaryStage != null) {
                        primaryStage.close();
                    }
                });

            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    String errorMessage = "Login failed: " + e.getMessage();
                    if (e.getMessage() != null && e.getMessage().contains("Unauthorized")) {
                        errorMessage = "Login failed: Incorrect username or password.";
                    }
                    System.err.println("❌ Login error: " + errorMessage);
                    statusLabel.setText(errorMessage);
                    loginButton.setDisable(false);
                });
            }
        });
    }}
