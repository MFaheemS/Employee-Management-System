package com.ems;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void handleSubmit() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please enter both username and password.");
            return;
        }

        // Default credentials: admin / admin
        if (username.equals("admin") && password.equals("admin")) {
            try {
                Main.showEmployeeAdd();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Navigation Error",
                        "Could not load the next page: " + e.getMessage());
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Login Failed",
                    "Invalid username or password.\nHint: use admin / admin");
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
