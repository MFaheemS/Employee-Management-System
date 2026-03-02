package com.ems;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class EmployeeDeactivateController {

    @FXML
    private TextField employeeField;

    @FXML
    private TextArea reasonField;

    @FXML
    private void handleDeactivate() {
        String employee = employeeField.getText().trim();
        String reason   = reasonField.getText().trim();

        if (employee.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please specify the employee to deactivate.");
            return;
        }

        if (reason.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please enter a reason for deactivation.");
            return;
        }

        showAlert(Alert.AlertType.INFORMATION, "Employee Deactivated",
                "Employee \"" + employee + "\" has been deactivated.\nReason: " + reason);

        employeeField.clear();
        reasonField.clear();
    }

    @FXML
    private void goToAdd() {
        try {
            Main.showEmployeeAdd();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
        }
    }

    @FXML
    private void goToDeactivate() {
        // already on this page — no-op
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
