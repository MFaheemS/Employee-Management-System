package com.ems;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    private final AuthService authService = new AuthService();

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordVisibleField;

    @FXML
    private Button togglePasswordButton;

    @FXML
    private Label hintLabel;

    private boolean showingPassword;

    @FXML
    private void initialize() {
        AppSession.clear();
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        hintLabel.setText("Demo logins: admin/admin, manager/manager, employee/employee");
    }

    @FXML
    private void togglePasswordVisibility() {
        showingPassword = !showingPassword;

        passwordVisibleField.setVisible(showingPassword);
        passwordVisibleField.setManaged(showingPassword);

        passwordField.setVisible(!showingPassword);
        passwordField.setManaged(!showingPassword);

        togglePasswordButton.setText(showingPassword ? "Hide" : "Show");

        if (showingPassword) {
            passwordVisibleField.requestFocus();
            passwordVisibleField.positionCaret(passwordVisibleField.getText().length());
        } else {
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    @FXML
    private void handleSubmit() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please enter both username and password.");
            return;
        }

        try {
            AppUser user = authService.authenticate(username, password);
            if (user != null) {
                AppSession.setCurrentUser(user);
                Main.showHomeForCurrentUser();
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed",
                        "Invalid username, password, or inactive account.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Login Error",
                    "Could not complete login: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
