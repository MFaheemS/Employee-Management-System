package com.ems;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

public class EmployeeAddController {

    @FXML
    private TextField employeeIdField;

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField jobTitleField;

    @FXML
    private TextField departmentField;

    @FXML
    private TextField emailField;

    @FXML
    private void handleAddEmployee() {
        String id   = employeeIdField.getText().trim();
        String name = fullNameField.getText().trim();
        String title = jobTitleField.getText().trim();
        String dept  = departmentField.getText().trim();
        String email = emailField.getText().trim();

        if (id.isEmpty() || name.isEmpty() || title.isEmpty()
                || dept.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please fill in all fields.");
            return;
        }

        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            showAlert(Alert.AlertType.WARNING, "Invalid Email",
                    "Please enter a valid email address.");
            return;
        }

        showAlert(Alert.AlertType.INFORMATION, "Employee Added",
                "Employee \"" + name + "\" (ID: " + id + ") has been added successfully.");

        employeeIdField.clear();
        fullNameField.clear();
        jobTitleField.clear();
        departmentField.clear();
        emailField.clear();
    }

    @FXML
    private void goToAdd() {
        // already on this page — no-op
    }

    @FXML
    private void goToDeactivate() {
        try {
            Main.showEmployeeDeactivate();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
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
