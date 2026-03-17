package com.ems;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

public class EmployeeSearchController extends BaseController {

    private final EmployeeRepository employeeRepository = new EmployeeRepository();

    @FXML
    private Label userLabel;

    @FXML
    private TextField searchField;

    @FXML
    private Label statusLabel;

    @FXML
    private Label profileLabel;

    @FXML
    private Button employeeAddNavButton;

    @FXML
    private Button employeeDeactivateNavButton;

    @FXML
    private Button leaveApprovalsNavButton;

    @FXML
    private Button employeeSearchNavButton;

    @FXML
    private TableView<Employee> searchResultTable;

    @FXML
    private TableColumn<Employee, String> idColumn;

    @FXML
    private TableColumn<Employee, String> nameColumn;

    @FXML
    private TableColumn<Employee, String> deptColumn;

    @FXML
    private TableColumn<Employee, String> roleColumn;

    @FXML
    private TableColumn<Employee, String> statusColumn;

    @FXML
    private void initialize() {
        if (!ensureEmployeeSearchAccess()) {
            return;
        }

        configureNavigation();
        configureTable();
        runSearch();
    }

    @FXML
    private void handleSearch() {
        runSearch();
    }

    @FXML
    private void handleViewProfile() {
        Employee selected = searchResultTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setErrorStatus("Please select an employee from the results.");
            return;
        }

        profileLabel.setText(
                "Employee ID: " + selected.getEmployeeId() + "\n"
                        + "Name: " + selected.getFullName() + "\n"
                        + "Role: " + selected.getRole() + "\n"
                        + "Department: " + selected.getDepartment() + "\n"
                        + "Job Title: " + selected.getJobTitle() + "\n"
                        + "Email: " + selected.getEmail() + "\n"
                        + "Status: " + (selected.isActive() ? "Active" : "Inactive") + "\n"
                        + "Leave Balance: " + selected.getLeaveBalance() + " day(s)"
        );

        setSuccessStatus("Employee profile loaded.");
    }

    @FXML
    private void goToAdd() {
        try {
            Main.showEmployeeAdd();
        } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
        }
    }

    @FXML
    private void goToDeactivate() {
        try {
            Main.showEmployeeDeactivate();
        } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
        }
    }

    @FXML
    private void goToLeaveApply() {
        try {
            Main.showLeaveApplication();
        } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
        }
    }

    @FXML
    private void goToAttendance() {
        try {
            Main.showAttendance();
        } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
        }
    }

    @FXML
    private void goToLeaveApprovals() {
        try {
            Main.showLeaveApprovals();
        } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
        }
    }

    @FXML
    private void goToEmployeeSearch() {
        // already on this page
    }

    @FXML
    protected void handleLogout() {
        super.handleLogout();
    }

    private void configureNavigation() {
        AppUser user = currentUser();
        userLabel.setText(user.getUsername() + " (" + user.getRole() + ")");
        employeeAddNavButton.setDisable(!user.canManageEmployees());
        employeeDeactivateNavButton.setDisable(!user.canManageEmployees());
        leaveApprovalsNavButton.setDisable(!user.canManageLeaveApprovals());
        employeeSearchNavButton.setDisable(!user.canSearchEmployees());
    }

    private void configureTable() {
        searchResultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        idColumn.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        deptColumn.setCellValueFactory(new PropertyValueFactory<>("department"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        statusColumn.setCellValueFactory(cellData -> Bindings.createStringBinding(
                () -> cellData.getValue().isActive() ? "Active" : "Inactive"));
    }

    private void runSearch() {
        try {
            List<Employee> employees = employeeRepository.searchEmployees(searchField.getText());
            searchResultTable.setItems(FXCollections.observableArrayList(employees));

            if (employees.isEmpty()) {
                setErrorStatus("No employee records found for this search.");
                profileLabel.setText("Select an employee row and click 'View Profile' to see full details.");
                return;
            }

            setSuccessStatus("Found " + employees.size() + " matching employee record(s).");
        } catch (SQLException e) {
            setErrorStatus("Could not search employees: " + e.getMessage());
        }
    }

    private void setSuccessStatus(String message) {
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add("status-success");
        statusLabel.setText(message);
    }

    private void setErrorStatus(String message) {
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add("status-error");
        statusLabel.setText(message);
    }
}