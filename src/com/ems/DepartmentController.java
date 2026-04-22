package com.ems;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

public class DepartmentController extends BaseController {

    private final DepartmentRepository departmentRepository = new DepartmentRepository();
    private final EmployeeRepository   employeeRepository   = new EmployeeRepository();

    @FXML private Label userLabel;
    @FXML private Label statusLabel;

    @FXML private TextField deptNameField;
    @FXML private ComboBox<String> managerComboBox;

    @FXML private TableView<Department> departmentTable;
    @FXML private TableColumn<Department, String> colDeptName;
    @FXML private TableColumn<Department, String> colManager;
    @FXML private TableColumn<Department, String> colCreatedAt;

    @FXML private Label deleteStatusLabel;
    @FXML private ListView<String> unassignedManagersList;

    // ── Sidebar nav ─────────────────────────────────────────────────────────
    @FXML private Button dashboardNavButton;
    @FXML private Button employeeAddNavButton;
    @FXML private Button employeeDeactivateNavButton;
    @FXML private Button departmentNavButton;
    @FXML private Button employeeSearchNavButton;
    @FXML private Button attendanceNavButton;
    @FXML private Button leaveApplyNavButton;
    @FXML private Button leaveApprovalsNavButton;
    @FXML private Button payrollNavButton;
    @FXML private Button documentsNavButton;

    @FXML
    private void initialize() {
        if (!ensureLoggedIn()) return;
        if (!currentUser().canManageDepartments()) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Access Denied",
                    "Only Administrators can manage departments.");
            goHome();
            return;
        }

        configureSidebarNavigation(userLabel, employeeAddNavButton, employeeDeactivateNavButton,
                employeeSearchNavButton, attendanceNavButton, leaveApplyNavButton, leaveApprovalsNavButton);
        configureAdditionalNavigation(dashboardNavButton, payrollNavButton, documentsNavButton);
        configureDepartmentNavigation(departmentNavButton);

        configureTable();
        loadManagers();
        loadDepartments();
        loadUnassignedManagers();
    }

    @FXML
    private void handleAddDepartment() {
        String name = deptNameField.getText().trim();
        String manager = managerComboBox.getValue();

        if (name.isEmpty()) {
            setError("Department name is required.");
            return;
        }
        if (manager == null || manager.isBlank()) {
            setError("You must assign a manager to this department.");
            return;
        }

        try {
            if (departmentRepository.departmentExists(name)) {
                setError("A department named \"" + name + "\" already exists.");
                return;
            }
            departmentRepository.addDepartment(name, manager);
            // Update the manager's employee record: derive job title from dept name
            employeeRepository.updateManagerAssignment(manager, name, name + " Manager");
            setSuccess("Department \"" + name + "\" created and assigned to " + manager + ".");
            deptNameField.clear();
            managerComboBox.setValue(null);
            loadDepartments();
            loadUnassignedManagers();
        } catch (SQLException e) {
            setError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteDepartment() {
        Department selected = departmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setDeleteError("Please select a department to remove.");
            return;
        }
        try {
            if (departmentRepository.hasEmployeesInDepartment(selected.getDepartmentName())) {
                setDeleteError("Cannot remove \"" + selected.getDepartmentName()
                        + "\" — it still has active employees. Reassign or deactivate them first.");
                return;
            }
            String managerUsername = selected.getManagerUsername();
            departmentRepository.deleteDepartment(selected.getDepartmentId());
            // Reset the manager's record: no dept, generic title, salary stays
            employeeRepository.updateManagerAssignment(managerUsername, "Unassigned", "Manager");
            setDeleteSuccess("Department \"" + selected.getDepartmentName() + "\" has been removed.");
            loadDepartments();
            loadManagers();
            loadUnassignedManagers();
        } catch (SQLException e) {
            setDeleteError("Database error: " + e.getMessage());
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML private void goToDashboard()      { navigate(Main::showDashboard); }
    @FXML private void goToAdd()            { navigate(Main::showEmployeeAdd); }
    @FXML private void goToDeactivate()     { navigate(Main::showEmployeeDeactivate); }
    @FXML private void goToDepartments()    { /* already here */ }
    @FXML private void goToEmployeeSearch() { navigate(Main::showEmployeeSearch); }
    @FXML private void goToAttendance()     { navigate(Main::showAttendance); }
    @FXML private void goToLeaveApply()     { navigate(Main::showLeaveApplication); }
    @FXML private void goToLeaveApprovals() { navigate(Main::showLeaveApprovals); }
    @FXML private void goToPayroll()        { navigate(Main::showPayroll); }
    @FXML private void goToDocuments()      { navigate(Main::showDocuments); }
    @FXML protected void handleLogout()     { super.handleLogout(); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void loadDepartments() {
        try {
            List<Department> departments = departmentRepository.getAllDepartments();
            departmentTable.setItems(FXCollections.observableArrayList(departments));
        } catch (SQLException e) {
            setError("Could not load departments: " + e.getMessage());
        }
    }

    private void loadManagers() {
        try {
            List<String> managers = departmentRepository.getUnassignedManagers();
            managerComboBox.setItems(FXCollections.observableArrayList(managers));
        } catch (SQLException e) {
            setError("Could not load managers: " + e.getMessage());
        }
    }

    private void loadUnassignedManagers() {
        try {
            List<String> unassigned = departmentRepository.getUnassignedManagers();
            unassignedManagersList.setItems(FXCollections.observableArrayList(unassigned));
        } catch (SQLException e) {
            setError("Could not load unassigned managers: " + e.getMessage());
        }
    }

    private void configureTable() {
        departmentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colDeptName.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        colManager.setCellValueFactory(new PropertyValueFactory<>("managerUsername"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    }

    private void setSuccess(String msg) {
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add("status-success");
        statusLabel.setText(msg);
    }

    private void setError(String msg) {
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add("status-error");
        statusLabel.setText(msg);
    }

    private void setDeleteSuccess(String msg) {
        if (deleteStatusLabel == null) return;
        deleteStatusLabel.getStyleClass().removeAll("status-error", "status-success");
        deleteStatusLabel.getStyleClass().add("status-success");
        deleteStatusLabel.setText(msg);
    }

    private void setDeleteError(String msg) {
        if (deleteStatusLabel == null) return;
        deleteStatusLabel.getStyleClass().removeAll("status-error", "status-success");
        deleteStatusLabel.getStyleClass().add("status-error");
        deleteStatusLabel.setText(msg);
    }

    private void navigate(NavigationAction action) {
        try { action.run(); } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error", e.getMessage());
        }
    }

    @FunctionalInterface
    private interface NavigationAction { void run() throws Exception; }
}
