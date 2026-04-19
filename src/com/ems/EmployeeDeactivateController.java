package com.ems;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class EmployeeDeactivateController extends BaseController {

    private final EmployeeRepository employeeRepository = new EmployeeRepository();

    @FXML
    private TextField employeeField;

    @FXML
    private TextArea reasonField;

    @FXML
    private Label userLabel;

    @FXML
    private Button employeeAddNavButton;

    @FXML
    private Button employeeDeactivateNavButton;

    @FXML
    private Button leaveApplyNavButton;

    @FXML
    private Button leaveApprovalsNavButton;

    @FXML
    private Button attendanceNavButton;

    @FXML
    private Button employeeSearchNavButton;

    @FXML
    private Button dashboardNavButton;

    @FXML
    private Button payrollNavButton;

    @FXML
    private Button documentsNavButton;

    @FXML
    private void initialize() {
        if (!ensureEmployeeManagementAccess()) {
            return;
        }

        configureNavigation();
    }

    @FXML
    private void handleDeactivate() {
        String employeeId = employeeField.getText().trim();
        String reason   = reasonField.getText().trim();

        if (employeeId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please enter an employee ID to deactivate.");
            return;
        }

        if (reason.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please enter a reason for deactivation.");
            return;
        }

        try {
            Employee employee = employeeRepository.findById(employeeId);
            if (employee == null) {
                showAlert(Alert.AlertType.WARNING, "Not Found",
                        "No employee found with ID: " + employeeId);
                return;
            }

            if (!employee.isActive()) {
                showAlert(Alert.AlertType.INFORMATION, "Already Inactive",
                        "Employee \"" + employee.getFullName() + "\" is already inactive.");
                return;
            }

            if (employeeRepository.deactivateEmployee(employeeId, reason)) {
                showAlert(Alert.AlertType.INFORMATION, "Employee Deactivated",
                        "Employee \"" + employee.getFullName() + "\" (ID: " + employeeId
                                + ") has been deactivated.\nReason: " + reason);
                employeeField.clear();
                reasonField.clear();
            } else {
                showAlert(Alert.AlertType.ERROR, "Deactivation Failed",
                        "Could not deactivate employee.");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not deactivate employee: " + e.getMessage());
        }
    }

    @FXML
    private void goToDashboard() {
        try { Main.showDashboard(); } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", e.getMessage());
        }
    }

    @FXML
    private void goToPayroll() {
        try { Main.showPayroll(); } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", e.getMessage());
        }
    }

    @FXML
    private void goToDocuments() {
        try { Main.showDocuments(); } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", e.getMessage());
        }
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

    @FXML
    private void goToLeaveApply() {
        try {
            Main.showLeaveApplication();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
        }
    }

    @FXML
    private void goToAttendance() {
        try {
            Main.showAttendance();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
        }
    }

    @FXML
    private void goToEmployeeSearch() {
        try {
            Main.showEmployeeSearch();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
        }
    }

    @FXML
    private void goToLeaveApprovals() {
        try {
            Main.showLeaveApprovals();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
        }
    }

    @FXML
    protected void handleLogout() {
        super.handleLogout();
    }

    protected void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void configureNavigation() {
        configureSidebarNavigation(
                userLabel,
                employeeAddNavButton,
                employeeDeactivateNavButton,
                employeeSearchNavButton,
                attendanceNavButton,
                leaveApplyNavButton,
                leaveApprovalsNavButton
        );
        configureAdditionalNavigation(dashboardNavButton, payrollNavButton, documentsNavButton);
    }
}
