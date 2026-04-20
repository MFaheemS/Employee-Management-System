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
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PayrollController extends BaseController {

    private final PayrollService payrollService = new PayrollService();
    private final EmployeeRepository employeeRepository = new EmployeeRepository();
    private final AttendanceService attendanceService = new AttendanceService();

    @FXML private Label userLabel;
    @FXML private VBox generateSection;
    @FXML private TextField empIdField;
    @FXML private TextField monthField;
    @FXML private TextField yearField;
    @FXML private TextField baseSalaryField;
    @FXML private TextField overtimeHoursField;
    @FXML private TextField overtimeRateField;
    @FXML private TextField deductionsField;
    @FXML private TextField notesField;
    @FXML private TextField filterField;
    @FXML private Label salarySlipLabel;
    @FXML private Label statusLabel;

    @FXML private Button dashboardNavButton;
    @FXML private Button employeeAddNavButton;
    @FXML private Button employeeDeactivateNavButton;
    @FXML private Button employeeSearchNavButton;
    @FXML private Button attendanceNavButton;
    @FXML private Button leaveApplyNavButton;
    @FXML private Button leaveApprovalsNavButton;
    @FXML private Button payrollNavButton;
    @FXML private Button documentsNavButton;
    @FXML private Button departmentNavButton;

    @FXML private VBox filterSection;
    @FXML private TableView<PayrollRecord> payrollTable;
    @FXML private TableColumn<PayrollRecord, String> colEmployee;
    @FXML private TableColumn<PayrollRecord, String> colPeriod;
    @FXML private TableColumn<PayrollRecord, Double> colBase;
    @FXML private TableColumn<PayrollRecord, Double> colOvertime;
    @FXML private TableColumn<PayrollRecord, Double> colDeductions;
    @FXML private TableColumn<PayrollRecord, Double> colGross;
    @FXML private TableColumn<PayrollRecord, String> colNet;
    @FXML private TableColumn<PayrollRecord, String> colGeneratedBy;

    @FXML
    private void initialize() {
        if (!ensureLoggedIn()) return;

        configureSidebarNavigation(userLabel, employeeAddNavButton, employeeDeactivateNavButton,
                employeeSearchNavButton, attendanceNavButton, leaveApplyNavButton, leaveApprovalsNavButton);
        configureAdditionalNavigation(dashboardNavButton, payrollNavButton, documentsNavButton);
        configureDepartmentNavigation(departmentNavButton);

        // Only HR Managers can generate payroll; employees see their own slips
        boolean canGenerate = currentUser().canGeneratePayroll();
        generateSection.setVisible(canGenerate);
        generateSection.setManaged(canGenerate);

        // Pre-fill current month/year
        LocalDate now = LocalDate.now();
        monthField.setText(String.valueOf(now.getMonthValue()));
        yearField.setText(String.valueOf(now.getYear()));

        // Hide filter for employees — they only ever see their own records
        if (currentUser().isEmployee()) {
            if (filterSection != null) { filterSection.setVisible(false); filterSection.setManaged(false); }
        }

        configureTable();
        loadPayroll();
    }

    @FXML
    private void handleLoadSalary() {
        String id = empIdField.getText().trim();
        if (id.isEmpty()) {
            setError("Enter an Employee ID first.");
            return;
        }
        try {
            Employee emp = employeeRepository.findById(id);
            if (emp == null) {
                setError("Employee not found: " + id);
                return;
            }
            baseSalaryField.setText(emp.getSalary() > 0 ? String.valueOf((long) emp.getSalary()) : "0");
            overtimeRateField.setText("0");
            overtimeHoursField.setText("0");
            deductionsField.setText("0");
            setSuccess("Loaded salary for " + emp.getFullName() + ". Adjust as needed.");
        } catch (SQLException e) {
            setError("DB error: " + e.getMessage());
        }
    }

    @FXML
    private void handleGeneratePayroll() {
        statusLabel.setText("");
        salarySlipLabel.setText("");
        try {
            String empId = empIdField.getText().trim();
            int month = Integer.parseInt(monthField.getText().trim());
            int year = Integer.parseInt(yearField.getText().trim());
            double base = Double.parseDouble(baseSalaryField.getText().trim());
            double otHours = parseOptional(overtimeHoursField.getText());
            double otRate = parseOptional(overtimeRateField.getText());
            double deductions = parseOptional(deductionsField.getText());
            String notes = notesField.getText().trim();

            PayrollRecord record = payrollService.generatePayroll(
                    currentUser(), empId, month, year, base, otHours, otRate, deductions, notes);

            String slip = String.format(
                    "=== SALARY SLIP ===\nEmployee: %s (%s)\nPeriod: %s\n"
                    + "Base Salary:    PKR %.2f\nOvertime Pay:   PKR %.2f\n"
                    + "Gross Salary:   PKR %.2f\nDeductions:     PKR %.2f\n"
                    + "NET SALARY:     PKR %.2f",
                    record.getEmployeeName(), record.getEmployeeId(), record.getPeriod(),
                    record.getBaseSalary(), record.getOvertimeHours() * record.getOvertimeRate(),
                    record.getGrossSalary(), record.getDeductions(), record.getNetSalary());

            salarySlipLabel.setText(slip);
            setSuccess("Payroll generated successfully for " + record.getPeriod() + ".");
            loadPayroll();
            clearGenerateFields();
        } catch (NumberFormatException e) {
            setError("Please enter valid numeric values for all salary fields.");
        } catch (IllegalArgumentException e) {
            setError(e.getMessage());
        } catch (SQLException e) {
            setError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleAutoFillFromAttendance() {
        String id = empIdField.getText().trim();
        if (id.isEmpty()) { setError("Enter an Employee ID first."); return; }
        String monthText = monthField.getText().trim();
        String yearText = yearField.getText().trim();
        if (monthText.isEmpty() || yearText.isEmpty()) { setError("Enter month and year first."); return; }
        try {
            int month = Integer.parseInt(monthText);
            int year = Integer.parseInt(yearText);
            double totalHours = attendanceService.getTotalHoursForMonth(id, month, year);
            overtimeHoursField.setText(String.format("%.2f", totalHours));
            setSuccess(String.format("Attendance hours for %s/%d: %.2f hrs. Adjust overtime rate and deductions as needed.", monthText, year, totalHours));
        } catch (NumberFormatException e) {
            setError("Month and year must be numeric.");
        } catch (java.sql.SQLException e) {
            setError("DB error: " + e.getMessage());
        }
    }

    @FXML
    private void handleFilter() {
        String filter = filterField.getText().trim();
        if (filter.isEmpty()) {
            loadPayroll();
            return;
        }
        try {
            List<PayrollRecord> records = payrollService.getPayrollForEmployee(filter);
            payrollTable.setItems(FXCollections.observableArrayList(records));
        } catch (SQLException e) {
            setError("Could not filter: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowAll() {
        filterField.clear();
        loadPayroll();
    }

    @FXML private void goToDashboard() { navigate(() -> Main.showDashboard()); }
    @FXML private void goToAdd() { navigate(() -> Main.showEmployeeAdd()); }
    @FXML private void goToDeactivate() { navigate(() -> Main.showEmployeeDeactivate()); }
    @FXML private void goToDepartments() { navigate(() -> Main.showDepartmentManagement()); }
    @FXML private void goToEmployeeSearch() { navigate(() -> Main.showEmployeeSearch()); }
    @FXML private void goToAttendance() { navigate(() -> Main.showAttendance()); }
    @FXML private void goToLeaveApply() { navigate(() -> Main.showLeaveApplication()); }
    @FXML private void goToLeaveApprovals() { navigate(() -> Main.showLeaveApprovals()); }
    @FXML private void goToPayroll() { /* already here */ }
    @FXML private void goToDocuments() { navigate(() -> Main.showDocuments()); }
    @FXML protected void handleLogout() { super.handleLogout(); }

    private void navigate(NavigationAction action) {
        try { action.run(); } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error", e.getMessage());
        }
    }

    private void loadPayroll() {
        try {
            List<PayrollRecord> records;
            if (currentUser().isManager() || currentUser().isAdmin()) {
                records = payrollService.getAllPayroll();
            } else {
                String eid = currentUser().getEmployeeId();
                records = eid != null ? payrollService.getPayrollForEmployee(eid)
                        : java.util.Collections.emptyList();
            }
            payrollTable.setItems(FXCollections.observableArrayList(records));
        } catch (SQLException e) {
            setError("Could not load payroll: " + e.getMessage());
        }
    }

    private void configureTable() {
        payrollTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colEmployee.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        colPeriod.setCellValueFactory(cellData ->
                Bindings.createStringBinding(cellData.getValue()::getPeriod));
        colBase.setCellValueFactory(new PropertyValueFactory<>("baseSalary"));
        colOvertime.setCellValueFactory(cellData ->
                Bindings.createObjectBinding(() ->
                        cellData.getValue().getOvertimeHours() * cellData.getValue().getOvertimeRate()));
        colDeductions.setCellValueFactory(new PropertyValueFactory<>("deductions"));
        colGross.setCellValueFactory(new PropertyValueFactory<>("grossSalary"));
        colNet.setCellValueFactory(cellData ->
                Bindings.createStringBinding(cellData.getValue()::getFormattedNet));
        colGeneratedBy.setCellValueFactory(new PropertyValueFactory<>("generatedBy"));
    }

    private void clearGenerateFields() {
        empIdField.clear();
        baseSalaryField.clear();
        overtimeHoursField.clear();
        overtimeRateField.clear();
        deductionsField.clear();
        notesField.clear();
    }

    private double parseOptional(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) return 0.0;
        return Double.parseDouble(trimmed);
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

    @FunctionalInterface
    private interface NavigationAction {
        void run() throws Exception;
    }
}
