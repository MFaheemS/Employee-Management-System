package com.ems;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class EmployeeDeactivateController extends BaseController {

    private final EmployeeRepository employeeRepository = new EmployeeRepository();

    @FXML private TextField employeeField;
    @FXML private TextArea  reasonField;
    @FXML private Label     requestStatusLabel;

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
        }

        configureNavigation();
    }

    // ── Admin: direct deactivation ────────────────────────────────────────────

    @FXML
    private void handleDeactivate() {
        String employeeId = employeeField.getText().trim();
        String reason     = reasonField.getText().trim();

        if (employeeId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter an employee ID.");
            return;
        }
        if (reason.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a reason.");
            return;
        }

        try {
            Employee employee = employeeRepository.findById(employeeId);
            if (employee == null) {
                showAlert(Alert.AlertType.WARNING, "Not Found", "No employee found with ID: " + employeeId);
                return;
            }
            if (!employee.isActive()) {
                showAlert(Alert.AlertType.INFORMATION, "Already Inactive",
                        "Employee \"" + employee.getFullName() + "\" is already inactive.");
                return;
            }

            AppUser user = currentUser();
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
                if (employeeRepository.deleteEmployeeCompletely(employeeId)) {
                    showAlert(Alert.AlertType.INFORMATION, "Record Removed",
                            "\"" + employee.getFullName() + "\" (ID: " + employeeId
                                    + ") has been permanently removed from the system.");
                    employeeField.clear();
                    reasonField.clear();
                    loadPendingRequests();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Failed", "Could not remove employee record.");
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
