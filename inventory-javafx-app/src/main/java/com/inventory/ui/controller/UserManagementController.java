package com.inventory.ui.controller;

import com.inventory.model.User;
import com.inventory.ui.util.ApiClient;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Controller for the user-management-view.fxml.
 * Allows OWNERs to manage user accounts (view, add, edit, delete).
 */
public class UserManagementController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Long> userIdColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> rolesColumn; // Displays comma-separated roles
    @FXML private TableColumn<User, Void> userActionsColumn;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ChoiceBox<User.Role> roleChoiceBox; // Correctly reference User.Role
    @FXML private Label statusLabel;

    private ApiClient apiClient;
    private ExecutorService executorService;
    private MainController mainController; // To refresh main UI components if needed

    private ObservableList<User> userList = FXCollections.observableArrayList();
    private User selectedUserForEdit; // To hold the user being edited

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
        // --- Table Initialization ---
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        rolesColumn.setCellValueFactory(cellData -> {
            Set<User.Role> roles = cellData.getValue().getRoles();
            String rolesString = roles.stream()
                    .map(Enum::name) // Use Enum::name for string representation
                    .collect(Collectors.joining(", "));
            return new SimpleStringProperty(rolesString);
        });

        userActionsColumn.setCellFactory(param -> new TableCell<User, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");

            {
                editButton.getStyleClass().add("secondary-button");
                deleteButton.getStyleClass().add("danger-button");

                editButton.setOnAction(event -> {
                    selectedUserForEdit = getTableView().getItems().get(getIndex());
                    populateFormForEdit(selectedUserForEdit);
                });

                deleteButton.setOnAction(event -> {
                    User userToDelete = getTableView().getItems().get(getIndex());
                    handleDeleteUser(userToDelete);
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

        userTable.setItems(userList);

        // --- Form Initialization ---
        roleChoiceBox.getItems().setAll(User.Role.values()); // Correctly reference User.Role
        roleChoiceBox.getSelectionModel().selectFirst(); // Select the first role by default
    }

    /**
     * Called by MainController after all dependencies are injected.
     * Loads initial user data.
     */
    public void initController() {
        refreshUsers(); // Load all users initially
    }

    /**
     * Refreshes the user table by fetching the latest data from the backend.
     */
    @FXML
    private void handleRefreshUsers() {
        refreshUsers();
    }

    private void refreshUsers() {
        if (apiClient == null || executorService == null) {
            System.err.println("ERROR: ApiClient or ExecutorService is null in UserManagementController.refreshUsers().");
            Platform.runLater(() -> statusLabel.setText("System error: Dependencies not initialized."));
            return;
        }

        statusLabel.setText("Loading users...");
        executorService.submit(() -> {
            try {
                // This API call should succeed for OWNER
                List<User> users = apiClient.getAllUsers();
                Platform.runLater(() -> {
                    userList.setAll(users);
                    statusLabel.setText("Users loaded successfully. Total users: " + users.size());
                });
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error loading users: " + e.getMessage());
                    showAlert("Error", "User Load Failed", "Could not load users: " + e.getMessage());
                    System.err.println("UserManagementController: Error fetching users: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Populates the form fields with details of the selected user for editing.
     * @param user The user object to populate the form with.
     */
    private void populateFormForEdit(User user) {
        usernameField.setText(user.getUsername());
        // Password field is intentionally left blank for security on edit.
        // User must re-enter if they want to change it.
        passwordField.setText("");
        // Assuming user has at least one role and we're picking the first for the ChoiceBox
        if (!user.getRoles().isEmpty()) {
            roleChoiceBox.getSelectionModel().select(user.getRoles().iterator().next());
        } else {
            roleChoiceBox.getSelectionModel().clearSelection();
        }
        statusLabel.setText("Editing user: " + user.getUsername());
    }

    /**
     * Handles saving a new user or updating an existing one.
     */
    @FXML
    private void handleSaveUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText(); // Keep as is, backend handles encoding/blank
        User.Role role = roleChoiceBox.getSelectionModel().getSelectedItem();

        // --- Validation ---
        if (username.isEmpty()) {
            statusLabel.setText("Username cannot be empty.");
            return;
        }
        if (role == null) {
            statusLabel.setText("Please select a role.");
            return;
        }
        if (selectedUserForEdit == null && password.isEmpty()) {
            statusLabel.setText("Password cannot be empty for new users.");
            return;
        }

        User userToSave = new User();
        userToSave.setUsername(username);
        userToSave.setPassword(password); // Set password using the method
        userToSave.setRoles(Set.of(role)); // Set the selected role

        statusLabel.setText("Saving user...");
        executorService.submit(() -> {
            try {
                User resultUser;
                if (selectedUserForEdit == null) { // Creating new user
                    resultUser = apiClient.createUser(userToSave);
                    Platform.runLater(() -> {
                        statusLabel.setText("User '" + resultUser.getUsername() + "' created successfully.");
                        showAlert("Success", "User Created", "User '" + resultUser.getUsername() + "' has been created.");
                    });
                } else { // Updating existing user
                    userToSave.setId(selectedUserForEdit.getId()); // Ensure ID is set for update
                    resultUser = apiClient.updateUser(userToSave.getId(), userToSave);
                    Platform.runLater(() -> {
                        statusLabel.setText("User '" + resultUser.getUsername() + "' updated successfully.");
                        showAlert("Success", "User Updated", "User '" + resultUser.getUsername() + "' has been updated.");
                    });
                }
                Platform.runLater(() -> {
                    handleClearForm(); // Clear form and reset state
                    refreshUsers(); // Refresh the table
                });
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    String errorMessage = "Error saving user: " + e.getMessage();
                    statusLabel.setText(errorMessage);
                    showAlert("Error", "User Save Failed", errorMessage);
                    System.err.println("UserManagementController: Error saving user via API: " + e.getMessage());
                });
            } catch (IllegalArgumentException e) { // For username exists error from backend
                Platform.runLater(() -> {
                    statusLabel.setText("Validation Error: " + e.getMessage());
                    showAlert("Validation Error", "Input Error", e.getMessage());
                });
            }
        });
    }

    /**
     * Handles deleting a user.
     * @param userToDelete The user object to delete.
     */
    private void handleDeleteUser(User userToDelete) {
        if (userToDelete == null) {
            showAlert("No Selection", "No User Selected", "Please select a user from the table to delete.");
            return;
        }

        Optional<ButtonType> result = showAlertConfirmation("Confirm Deletion", "Delete User",
                "Are you sure you want to delete user: " + userToDelete.getUsername() + " (ID: " + userToDelete.getId() + ")?");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            statusLabel.setText("Deleting user...");
            executorService.submit(() -> {
                try {
                    apiClient.deleteUser(userToDelete.getId());
                    Platform.runLater(() -> {
                        statusLabel.setText("User '" + userToDelete.getUsername() + "' deleted successfully.");
                        showAlert("Success", "User Deleted", "User '" + userToDelete.getUsername() + "' has been deleted.");
                        refreshUsers(); // Refresh table after deletion
                    });
                } catch (IOException | InterruptedException e) {
                    Platform.runLater(() -> {
                        String errorMessage = "Error deleting user: " + e.getMessage();
                        statusLabel.setText(errorMessage);
                        showAlert("Error", "User Deletion Failed", errorMessage);
                        System.err.println("UserManagementController: Error deleting user via API: " + e.getMessage());
                    });
                }
            });
        }
    }

    /**
     * Handles the "Clear Form" button action. Resets all input fields and clears selected user.
     */
    @FXML
    private void handleClearForm() {
        selectedUserForEdit = null; // Clear edit state
        usernameField.clear();
        passwordField.clear();
        roleChoiceBox.getSelectionModel().selectFirst();
        statusLabel.setText("");
        userTable.getSelectionModel().clearSelection(); // Clear table selection
    }

    private void showAlert(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
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
