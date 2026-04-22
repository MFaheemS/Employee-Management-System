package com.ems;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class EmployeeAddController extends BaseController {

    private static final String MANAGER_ID_PATTERN  = "^M\\d{1,3}$";
    private static final String EMPLOYEE_ID_PATTERN = "^E\\d{1,3}$";
    private static final String MANAGER_ID_PREFIX   = "M";
    private static final String EMPLOYEE_ID_PREFIX  = "E";

    private final EmployeeRepository employeeRepository = new EmployeeRepository();
    private final DepartmentRepository departmentRepository = new DepartmentRepository();

    @FXML
    private TextField employeeIdField;

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField jobTitleField;

    @FXML
    private ComboBox<String> departmentComboBox;

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

    @FXML private Label pageTitleLabel;
    @FXML private Label topbarTitleLabel;
    @FXML private Label topbarSubtitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label cardTitleLabel;
    @FXML private Label idFieldLabel;
    @FXML private Button saveButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;

    @FXML private HBox titleDeptRow;
    @FXML private HBox salaryRow;
    @FXML private VBox roleRow;
    @FXML private VBox adminInfoCard;
    @FXML private Label idHintLabel;
    @FXML private Button autoIdButton;

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
    private Button departmentNavButton;

    @FXML
    private void initialize() {
        if (!ensureEmployeeManagementAccess()) {
            return;
        }

        AppUser user = currentUser();
        if (user.isAdmin()) {
            configureAdminManagerForm();
        } else {
            configureManagerEmployeeForm(user);
        }
        departmentComboBox.setOnAction(e -> autoFillManager());
        configureNavigation();
    }

    private void configureAdminManagerForm() {
        roleComboBox.setItems(FXCollections.observableArrayList("Manager"));
        roleComboBox.setValue("Manager");

        setRowVisible(titleDeptRow, false);
        setRowVisible(salaryRow, false);
        if (roleRow != null) { roleRow.setVisible(false); roleRow.setManaged(false); }
        if (adminInfoCard != null) { adminInfoCard.setVisible(true); adminInfoCard.setManaged(true); }

        setText(pageTitleLabel,      "Add Manager");
        setText(topbarTitleLabel,    "Manager Management");
        setText(topbarSubtitleLabel, "Register managers who can then be assigned to departments");
        setText(pageSubtitleLabel,   "Use Manager ID to add a new manager or load an existing record for updates.");
        setText(cardTitleLabel,      "Manager Details");
        setText(idFieldLabel,        "Manager ID");
        setText(saveButton,          "Add Manager");
        setText(updateButton,        "Update Manager");
        setText(deleteButton,        "Delete Manager");
        if (employeeIdField != null) employeeIdField.setPromptText("M001");
        setText(idHintLabel,         "Format: M followed by up to 3 digits  (e.g. M001, M12)");
        setupIdValidation(MANAGER_ID_PATTERN);
    }

    private void configureManagerEmployeeForm(AppUser user) {
        roleComboBox.setItems(FXCollections.observableArrayList("Employee"));
        roleComboBox.setValue("Employee");
        if (roleRow != null)       { roleRow.setVisible(false);      roleRow.setManaged(false); }
        if (adminInfoCard != null) { adminInfoCard.setVisible(false); adminInfoCard.setManaged(false); }

        setText(pageTitleLabel,      "Add Employee");
        setText(topbarTitleLabel,    "Employee Management");
        setText(cardTitleLabel,      "Employee Details");
        setText(idFieldLabel,        "Employee ID");
        setText(saveButton,          "Save Employee");
        setText(updateButton,        "Update Employee");
        setText(deleteButton,        "Delete Employee");
        if (employeeIdField != null) employeeIdField.setPromptText("E001");
        setText(idHintLabel,         "Format: E followed by up to 3 digits  (e.g. E001, E99)");
        setupIdValidation(EMPLOYEE_ID_PATTERN);

        loadDepartments();
        lockDepartmentToManager(user);
    }

    private void setupIdValidation(String pattern) {
        if (employeeIdField == null || idHintLabel == null) return;
        employeeIdField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                idHintLabel.setStyle("");
                return;
            }
            boolean valid = newVal.matches(pattern);
            idHintLabel.setStyle(valid
                    ? "-fx-text-fill: #22C55E;"
                    : "-fx-text-fill: #EF4444;");
        });
    }

    private void loadDepartments() {
        try {
            List<String> depts = departmentRepository.getAllDepartmentNames();
            departmentComboBox.setItems(FXCollections.observableArrayList(depts));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Could not load departments: " + e.getMessage());
        }
    }

    private void lockDepartmentToManager(AppUser user) {
        try {
            departmentRepository.getAllDepartments().stream()
                    .filter(d -> d.getManagerUsername().equals(user.getUsername()))
                    .findFirst()
                    .ifPresent(d -> departmentComboBox.setValue(d.getDepartmentName()));
            departmentComboBox.setDisable(true);
        } catch (Exception e) {
            // ignore — dept stays editable
        }
    }

    private void autoFillManager() {
        // manager_username is resolved from selected department in buildEmployeeFromFields
    }

    @FXML
    private void handleAutoId() {
        AppUser user = currentUser();
        String prefix = (user != null && user.isAdmin()) ? MANAGER_ID_PREFIX : EMPLOYEE_ID_PREFIX;
        try {
            String next = employeeRepository.getNextAvailableId(prefix);
            if (next != null && employeeIdField != null) {
                employeeIdField.setText(next);
            } else {
                showAlert(Alert.AlertType.WARNING, "No ID Available",
                        "All " + prefix + "001–" + prefix + "999 IDs are in use.");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not generate ID: " + e.getMessage());
        }
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
            if (departmentComboBox.getItems().contains(employee.getDepartment())) {
                departmentComboBox.setValue(employee.getDepartment());
            }
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
        AppUser user = currentUser();
        if (user != null && user.isAdmin()) {
            return buildManagerFromFields();
        }
        return buildEmployeeFromFieldsFull();
    }

    private Employee buildManagerFromFields() {
        String id    = employeeIdField.getText().trim();
        String name  = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        if (id.isEmpty() || name.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Manager ID, Full Name, and Email are required.");
            return null;
        }
        if (!id.matches(MANAGER_ID_PATTERN)) {
            showAlert(Alert.AlertType.WARNING, "Invalid Manager ID",
                    "Manager ID must start with 'M' followed by 1–3 digits (e.g. M001, M12).");
            return null;
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            showAlert(Alert.AlertType.WARNING, "Invalid Email",
                    "Please enter a valid email address.");
            return null;
        }
        return new Employee(id, name, "Manager", "Unassigned", email, phone, true, "Manager", "admin", 20, 0.0);
    }

    private Employee buildEmployeeFromFieldsFull() {
        String id         = employeeIdField.getText().trim();
        String name       = fullNameField.getText().trim();
        String title      = jobTitleField.getText().trim();
        String dept       = departmentComboBox.getValue() != null ? departmentComboBox.getValue().trim() : "";
        String email      = emailField.getText().trim();
        String phone      = phoneField.getText().trim();
        String salaryText = salaryField.getText().trim();
        String role       = roleComboBox.getValue();

        if (id.isEmpty() || name.isEmpty() || title.isEmpty() || dept.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please fill in all required fields (ID, Name, Job Title, Department, Email).");
            return null;
        }
        if (!id.matches(EMPLOYEE_ID_PATTERN)) {
            showAlert(Alert.AlertType.WARNING, "Invalid Employee ID",
                    "Employee ID must start with 'E' followed by 1–3 digits (e.g. E001, E99).");
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
                    showAlert(Alert.AlertType.WARNING, "Invalid Salary", "Salary cannot be negative.");
                    return null;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Invalid Salary", "Please enter a valid numeric salary.");
                return null;
            }
        }

        if (role == null) role = "Employee";
        String managerUsername = null;
        try {
            managerUsername = departmentRepository.getManagerForDepartment(dept);
        } catch (SQLException e) {
            // fall through
        }
        return new Employee(id, name, title, dept, email, phone, true, role, managerUsername, 20, salary);
    }

    private void clearFields() {
        employeeIdField.clear();
        fullNameField.clear();
        jobTitleField.clear();
        departmentComboBox.setValue(null);
        emailField.clear();
        phoneField.clear();
        salaryField.clear();
        usernameField.clear();
        passwordField.clear();
        // restore role default based on user context
        AppUser user = currentUser();
        roleComboBox.setValue(user != null && user.isAdmin() ? "Manager" : "Employee");
    }

    private void setRowVisible(HBox row, boolean visible) {
        if (row == null) return;
        row.setVisible(visible);
        row.setManaged(visible);
    }

    private void setText(Label label, String text) {
        if (label != null) label.setText(text);
    }

    private void setText(Button button, String text) {
        if (button != null) button.setText(text);
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
        configureDepartmentNavigation(departmentNavButton);
    }

    @FXML
    private void goToDepartments() {
        try { Main.showDepartmentManagement(); } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", e.getMessage());
        }
    }
}
