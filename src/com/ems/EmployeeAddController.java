package com.ems;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class EmployeeAddController extends BaseController {

    private final EmployeeRepository employeeRepository = new EmployeeRepository();

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
    private TextField phoneField;

    @FXML
    private TextField salaryField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

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

        roleComboBox.setItems(FXCollections.observableArrayList("Employee", "Manager", "Admin"));
        roleComboBox.setValue("Employee");
        configureNavigation();
    }

    @FXML
    private void handleAddEmployee() {
        Employee employee = buildEmployeeFromFields();
        if (employee == null) {
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please provide a login username and password for the employee.");
            return;
        }

        try {
            if (employeeRepository.findById(employee.getEmployeeId()) != null) {
                showAlert(Alert.AlertType.WARNING, "Duplicate Employee ID",
                        "An employee with this ID already exists.");
                return;
            }

            if (employeeRepository.usernameExists(username)) {
                showAlert(Alert.AlertType.WARNING, "Duplicate Username",
                        "The username \"" + username + "\" is already taken.");
                return;
            }

            if (employeeRepository.addEmployee(employee)) {
                employeeRepository.createUserAccount(username, password, employee.getRole(), employee.getEmployeeId());
                showAlert(Alert.AlertType.INFORMATION, "Employee Added",
                        "Employee \"" + employee.getFullName() + "\" (ID: "
                                + employee.getEmployeeId() + ") has been added successfully.\n"
                                + "Login username: " + username);
                clearFields();
            } else {
                showAlert(Alert.AlertType.ERROR, "Add Failed",
                        "Could not add employee.");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not add employee: " + e.getMessage());
        }
    }

    @FXML
    private void handleFindEmployee() {
        String id = employeeIdField.getText().trim();
        if (id.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please enter Employee ID to search.");
            return;
        }

        try {
            Employee employee = employeeRepository.findById(id);
            if (employee == null) {
                showAlert(Alert.AlertType.WARNING, "Not Found",
                        "No employee found with ID: " + id);
                return;
            }

            fullNameField.setText(employee.getFullName());
            jobTitleField.setText(employee.getJobTitle());
            departmentField.setText(employee.getDepartment());
            emailField.setText(employee.getEmail());
            phoneField.setText(employee.getPhone() != null ? employee.getPhone() : "");
            salaryField.setText(employee.getSalary() > 0 ? String.valueOf((long) employee.getSalary()) : "");
            roleComboBox.setValue(employee.getRole() != null ? employee.getRole() : "Employee");

            String status = employee.isActive() ? "Active" : "Inactive";
            showAlert(Alert.AlertType.INFORMATION, "Employee Loaded",
                    "Employee data loaded successfully. Status: " + status);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not load employee: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateEmployee() {
        Employee employee = buildEmployeeFromFields();
        if (employee == null) {
            return;
        }

        try {
            Employee existing = employeeRepository.findById(employee.getEmployeeId());
            if (existing == null) {
                showAlert(Alert.AlertType.WARNING, "Not Found",
                        "No employee found with ID: " + employee.getEmployeeId());
                return;
            }

            employee.setActive(existing.isActive());
            if (employeeRepository.updateEmployee(employee)) {
                showAlert(Alert.AlertType.INFORMATION, "Employee Updated",
                        "Employee (ID: " + employee.getEmployeeId() + ") has been updated.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Update Failed",
                        "Could not update employee.");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not update employee: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteEmployee() {
        String id = employeeIdField.getText().trim();
        if (id.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please enter Employee ID to delete.");
            return;
        }

        try {
            if (employeeRepository.deleteEmployee(id)) {
                showAlert(Alert.AlertType.INFORMATION, "Employee Deleted",
                        "Employee (ID: " + id + ") has been deleted.");
                clearFields();
            } else {
                showAlert(Alert.AlertType.WARNING, "Not Found",
                        "No employee found with ID: " + id);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not delete employee: " + e.getMessage());
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

    private Employee buildEmployeeFromFields() {
        String id = employeeIdField.getText().trim();
        String name = fullNameField.getText().trim();
        String title = jobTitleField.getText().trim();
        String dept = departmentField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String salaryText = salaryField.getText().trim();
        String role = roleComboBox.getValue();

        if (id.isEmpty() || name.isEmpty() || title.isEmpty() || dept.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please fill in all required fields (ID, Name, Job Title, Department, Email).");
            return null;
        }

        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            showAlert(Alert.AlertType.WARNING, "Invalid Email",
                    "Please enter a valid email address.");
            return null;
        }

        double salary = 0.0;
        if (!salaryText.isEmpty()) {
            try {
                salary = Double.parseDouble(salaryText);
                if (salary < 0) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Salary",
                            "Salary cannot be negative.");
                    return null;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Invalid Salary",
                        "Please enter a valid numeric salary.");
                return null;
            }
        }

        if (role == null) role = "Employee";

        return new Employee(id, name, title, dept, email, phone, true, role, null, 20, salary);
    }

    private void clearFields() {
        employeeIdField.clear();
        fullNameField.clear();
        jobTitleField.clear();
        departmentField.clear();
        emailField.clear();
        phoneField.clear();
        salaryField.clear();
        roleComboBox.setValue("Employee");
        usernameField.clear();
        passwordField.clear();
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
