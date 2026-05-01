package com.ems;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class EmployeeDeactivateController extends BaseController {

    private static final String ID_PATTERN_PARTIAL = "[MEme][0-9]{0,3}";
    private static final String ID_PATTERN_FULL    = "[MEme][0-9]{3}";

    private final EmployeeRepository employeeRepository = new EmployeeRepository();

    // Admin fields
    @FXML private TextField adminEmployeeField;
    @FXML private TextArea  adminReasonField;

    // Manager fields
    @FXML private TextField employeeField;
    @FXML private TextArea  reasonField;

    @FXML private Label     requestStatusLabel;

    private final ContextMenu adminSuggestions   = new ContextMenu();
    private final ContextMenu managerSuggestions = new ContextMenu();
    private boolean suppressListener = false;

    // Admin-only: direct deactivation form
    @FXML private VBox adminDeactivateSection;

    // Admin-only: pending requests table
    @FXML private VBox                         pendingRequestsSection;
    @FXML private TableView<EmployeeRepository.DeactivationRequest> requestsTable;
    @FXML private TableColumn<EmployeeRepository.DeactivationRequest, String>  colReqEmpId;
    @FXML private TableColumn<EmployeeRepository.DeactivationRequest, String>  colReqEmpName;
    @FXML private TableColumn<EmployeeRepository.DeactivationRequest, String>  colReqBy;
    @FXML private TableColumn<EmployeeRepository.DeactivationRequest, String>  colReqReason;
    @FXML private TableColumn<EmployeeRepository.DeactivationRequest, String>  colReqAt;

    // Manager-only: submit request section
    @FXML private VBox managerRequestSection;

    @FXML private Label  userLabel;
    @FXML private Button employeeAddNavButton;
    @FXML private Button employeeDeactivateNavButton;
    @FXML private Button leaveApplyNavButton;
    @FXML private Button leaveApprovalsNavButton;
    @FXML private Button attendanceNavButton;
    @FXML private Button employeeSearchNavButton;
    @FXML private Button dashboardNavButton;
    @FXML private Button payrollNavButton;
    @FXML private Button documentsNavButton;
    @FXML private Button departmentNavButton;

    @FXML
    private void initialize() {
        if (!ensureLoggedIn()) return;
        AppUser user = currentUser();
        if (!user.canManageEmployees()) {
            showAlert(Alert.AlertType.ERROR, "Access Denied",
                    "You are not authorized to deactivate employee records.");
            goHome();
            return;
        }

        boolean isAdmin = user.isAdmin();
        show(adminDeactivateSection, isAdmin);
        show(pendingRequestsSection, isAdmin);
        show(managerRequestSection, !isAdmin);

        if (isAdmin) {
            configureRequestsTable();
            loadPendingRequests();
            setupAutocomplete(adminEmployeeField, adminSuggestions, true);
        } else {
            setupAutocomplete(employeeField, managerSuggestions, false);
        }

        configureNavigation();
    }

    // ── Admin: direct deactivation ────────────────────────────────────────────

    @FXML
    private void handleDeactivate() {
        AppUser user = currentUser();
        boolean isAdmin = user.isAdmin();
        String employeeId = (isAdmin ? adminEmployeeField.getText() : employeeField.getText()).trim().toUpperCase();
        String reason     = (isAdmin ? adminReasonField.getText()   : reasonField.getText()).trim();

        if (employeeId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter an employee or manager ID.");
            return;
        }
        if (!employeeId.matches(ID_PATTERN_FULL)) {
            showAlert(Alert.AlertType.WARNING, "Invalid ID Format",
                    "ID must be M or E followed by exactly 3 digits (e.g. M001, E003).");
            return;
        }
        if (reason.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a reason.");
            return;
        }

        try {
            Employee employee = employeeRepository.findById(employeeId);
            if (employee == null) {
                showAlert(Alert.AlertType.WARNING, "Not Found", "No record found with ID: " + employeeId);
                return;
            }
            if (!employee.isActive()) {
                showAlert(Alert.AlertType.INFORMATION, "Already Inactive",
                        "\"" + employee.getFullName() + "\" is already inactive.");
                return;
            }

            if (user.isManager()) {
                boolean inDept = employeeRepository.isEmployeeInManagerDept(employeeId, user.getUsername());
                if (!inDept) {
                    showAlert(Alert.AlertType.ERROR, "Access Denied",
                            "You can only submit deactivation requests for employees in your own department.");
                    return;
                }
                if (employeeRepository.hasPendingDeactivationRequest(employeeId)) {
                    showAlert(Alert.AlertType.WARNING, "Already Pending",
                            "A deactivation request for this employee is already pending admin approval.");
                    return;
                }
                employeeRepository.submitDeactivationRequest(employeeId, user.getUsername(), reason);
                setStatus("Deactivation request submitted for \"" + employee.getFullName()
                        + "\". Awaiting admin approval.", false);
                employeeField.clear();
                reasonField.clear();
            } else {
                // Admin: permanently remove from database
                // Block if the target is a manager currently assigned to a department
                if ("Manager".equalsIgnoreCase(employee.getRole()) && isManagerAssignedToDepartment(employeeId)) {
                    showAlert(Alert.AlertType.ERROR, "Cannot Remove Manager",
                            "\"" + employee.getFullName() + "\" is currently assigned to a department. "
                            + "Remove the department assignment first before deleting this manager.");
                    return;
                }
                if (employeeRepository.deleteEmployeeCompletely(employeeId)) {
                    showAlert(Alert.AlertType.INFORMATION, "Record Removed",
                            "\"" + employee.getFullName() + "\" (ID: " + employeeId
                                    + ") has been permanently removed from the system.");
                    adminEmployeeField.clear();
                    adminReasonField.clear();
                    loadPendingRequests();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Failed", "Could not remove record.");
                }
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    // ── Admin: approve / reject pending requests ──────────────────────────────

    @FXML
    private void handleApproveRequest() {
        EmployeeRepository.DeactivationRequest req = requestsTable.getSelectionModel().getSelectedItem();
        if (req == null) { setStatus("Select a request row first.", true); return; }
        try {
            String removed = employeeRepository.approveDeactivationRequest(req.getRequestId(), currentUser().getUsername());
            if (removed != null) {
                setStatus("\"" + req.getEmployeeName() + "\" permanently removed from the system.", false);
            } else {
                setStatus("Request not found.", true);
            }
            loadPendingRequests();
        } catch (SQLException e) {
            setStatus("Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleRejectRequest() {
        EmployeeRepository.DeactivationRequest req = requestsTable.getSelectionModel().getSelectedItem();
        if (req == null) { setStatus("Select a request row first.", true); return; }
        try {
            employeeRepository.rejectDeactivationRequest(req.getRequestId(), currentUser().getUsername());
            setStatus("Request for \"" + req.getEmployeeName() + "\" rejected.", false);
            loadPendingRequests();
        } catch (SQLException e) {
            setStatus("Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleRefreshRequests() {
        loadPendingRequests();
    }

    private void loadPendingRequests() {
        try {
            List<EmployeeRepository.DeactivationRequest> list = employeeRepository.getPendingDeactivationRequests();
            requestsTable.setItems(FXCollections.observableArrayList(list));
        } catch (SQLException e) {
            setStatus("Could not load requests: " + e.getMessage(), true);
        }
    }

    private void configureRequestsTable() {
        requestsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colReqEmpId.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        colReqEmpName.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        colReqBy.setCellValueFactory(new PropertyValueFactory<>("requestedBy"));
        colReqReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        colReqAt.setCellValueFactory(new PropertyValueFactory<>("requestedAt"));
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML private void goToDashboard()      { navigate(Main::showDashboard); }
    @FXML private void goToPayroll()        { navigate(Main::showPayroll); }
    @FXML private void goToDocuments()      { navigate(Main::showDocuments); }
    @FXML private void goToAdd()            { navigate(Main::showEmployeeAdd); }
    @FXML private void goToDeactivate()     { /* already here */ }
    @FXML private void goToLeaveApply()     { navigate(Main::showLeaveApplication); }
    @FXML private void goToAttendance()     { navigate(Main::showAttendance); }
    @FXML private void goToEmployeeSearch() { navigate(Main::showEmployeeSearch); }
    @FXML private void goToLeaveApprovals() { navigate(Main::showLeaveApprovals); }
    @FXML private void goToDepartments()    { navigate(Main::showDepartmentManagement); }
    @FXML protected void handleLogout()     { super.handleLogout(); }

    private void navigate(NavigationAction action) {
        try { action.run(); } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", e.getMessage());
        }
    }

    private void configureNavigation() {
        configureSidebarNavigation(userLabel, employeeAddNavButton, employeeDeactivateNavButton,
                employeeSearchNavButton, attendanceNavButton, leaveApplyNavButton, leaveApprovalsNavButton);
        configureAdditionalNavigation(dashboardNavButton, payrollNavButton, documentsNavButton);
        configureDepartmentNavigation(departmentNavButton);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setupAutocomplete(TextField field, ContextMenu menu, boolean allRecords) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (suppressListener || newVal == null) return;
            String upper = newVal.toUpperCase();
            // Restrict input to valid partial format: [ME] followed by 0-3 digits
            if (!upper.isEmpty() && !upper.matches(ID_PATTERN_PARTIAL)) {
                suppressListener = true;
                field.setText(oldVal);
                suppressListener = false;
                return;
            }
            if (!upper.equals(newVal)) {
                suppressListener = true;
                field.setText(upper);
                suppressListener = false;
                return;
            }
            menu.hide();
            if (upper.isEmpty()) return;
            try {
                List<Employee> matches = allRecords
                        ? employeeRepository.findByIdPrefix(upper)
                        : employeeRepository.findByIdPrefixForManager(upper, currentUser().getUsername());
                if (matches.isEmpty()) return;
                menu.getItems().clear();
                for (Employee emp : matches) {
                    String label = emp.getEmployeeId() + "  —  " + emp.getFullName()
                            + "  (" + emp.getRole() + (emp.isActive() ? "" : ", Inactive") + ")";
                    MenuItem item = new MenuItem(label);
                    item.setOnAction(e -> {
                        suppressListener = true;
                        field.setText(emp.getEmployeeId());
                        suppressListener = false;
                        menu.hide();
                    });
                    menu.getItems().add(item);
                }
                menu.show(field, Side.BOTTOM, 0, 0);
            } catch (SQLException ignored) {}
        });
    }

    private boolean isManagerAssignedToDepartment(String employeeId) throws SQLException {
        String sql = "SELECT 1 FROM departments d "
                + "JOIN users u ON u.username = d.manager_username "
                + "WHERE u.employee_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, employeeId);
            try (ResultSet rs = st.executeQuery()) { return rs.next(); }
        }
    }

    private void show(VBox vbox, boolean visible) {
        if (vbox == null) return;
        vbox.setVisible(visible);
        vbox.setManaged(visible);
    }

    private void setStatus(String msg, boolean error) {
        if (requestStatusLabel == null) return;
        requestStatusLabel.getStyleClass().removeAll("status-error", "status-success");
        requestStatusLabel.getStyleClass().add(error ? "status-error" : "status-success");
        requestStatusLabel.setText(msg);
    }

    @FunctionalInterface
    private interface NavigationAction { void run() throws Exception; }
}
