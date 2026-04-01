package com.ems;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;

public class AttendanceController extends BaseController {

    private final AttendanceService attendanceService = new AttendanceService();
    private final EmployeeRepository employeeRepository = new EmployeeRepository();

    @FXML
    private Label userLabel;

    @FXML
    private Label employeeSummaryLabel;

    @FXML
    private Label todayStatusLabel;

    @FXML
    private Label todayCheckInLabel;

    @FXML
    private Label todayCheckOutLabel;

    @FXML
    private Label todayHoursLabel;

    @FXML
    private Label actionStatusLabel;

    @FXML
    private Button employeeAddNavButton;

    @FXML
    private Button employeeDeactivateNavButton;

    @FXML
    private Button leaveApprovalsNavButton;

    @FXML
    private Button leaveApplyNavButton;

    @FXML
    private Button employeeSearchNavButton;

    @FXML
    private VBox employeeView;

    @FXML
    private VBox managerView;

    @FXML
    private TableView<AttendanceRecord> historyTable;

    @FXML
    private TableColumn<AttendanceRecord, String> historyDateColumn;

    @FXML
    private TableColumn<AttendanceRecord, String> historyCheckInColumn;

    @FXML
    private TableColumn<AttendanceRecord, String> historyCheckOutColumn;

    @FXML
    private TableColumn<AttendanceRecord, String> historyHoursColumn;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TableView<AttendanceRecord> managerTable;

    @FXML
    private TableColumn<AttendanceRecord, String> mgrEmployeeIdColumn;

    @FXML
    private TableColumn<AttendanceRecord, String> mgrEmployeeNameColumn;

    @FXML
    private TableColumn<AttendanceRecord, String> mgrCheckInColumn;

    @FXML
    private TableColumn<AttendanceRecord, String> mgrCheckOutColumn;

    @FXML
    private TableColumn<AttendanceRecord, String> mgrHoursColumn;

    @FXML
    private void initialize() {
        if (!ensureAttendanceAccess()) {
            return;
        }

        configureNavigation();

        if (currentUser().isManager()) {
            employeeView.setVisible(false);
            employeeView.setManaged(false);
            managerView.setVisible(true);
            managerView.setManaged(true);
            configureManagerTable();
            datePicker.setValue(LocalDate.now());
            loadManagerRecords(LocalDate.now());
        } else {
            configureTable();
            loadAttendanceData();
        }
    }

    @FXML
    private void handleMarkAttendance() {
        actionStatusLabel.setText("");

        try {
            AttendanceRecord updated = attendanceService.markAttendance(currentUser());
            if (updated.getCheckOutAt() == null || updated.getCheckOutAt().isBlank()) {
                setSuccessStatus("Check-in recorded at " + updated.getCheckInAt());
            } else {
                setSuccessStatus("Check-out recorded at " + updated.getCheckOutAt()
                        + ". Total hours: " + updated.getDisplayTotalHours());
            }

            loadAttendanceData();
        } catch (IllegalArgumentException e) {
            setErrorStatus(e.getMessage());
        } catch (SQLException e) {
            setErrorStatus("Could not mark attendance: " + e.getMessage());
        }
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
    private void goToAttendance() {
        // already on this page
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
        try {
            Main.showEmployeeSearch();
        } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
        }
    }

    @FXML
    protected void handleLogout() {
        super.handleLogout();
    }

    private void configureNavigation() {
        configureSidebarNavigation(
                userLabel,
                employeeAddNavButton,
                employeeDeactivateNavButton,
                employeeSearchNavButton,
                null,
                leaveApplyNavButton,
                leaveApprovalsNavButton
        );
    }

    @FXML
    private void handleViewRecords() {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            return;
        }
        loadManagerRecords(date);
    }

    private void configureManagerTable() {
        managerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        mgrEmployeeIdColumn.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        mgrEmployeeNameColumn.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        mgrCheckInColumn.setCellValueFactory(new PropertyValueFactory<>("checkInAt"));
        mgrCheckOutColumn.setCellValueFactory(new PropertyValueFactory<>("checkOutAt"));
        mgrHoursColumn.setCellValueFactory(cellData -> Bindings.createStringBinding(
                cellData.getValue()::getDisplayTotalHours));
    }

    private void loadManagerRecords(LocalDate date) {
        try {
            managerTable.setItems(FXCollections.observableArrayList(
                    attendanceService.getRecordsByDate(date.toString())
            ));
        } catch (SQLException e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Database Error",
                    "Could not load records: " + e.getMessage());
        }
    }

    private void configureTable() {
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        historyDateColumn.setCellValueFactory(new PropertyValueFactory<>("attendanceDate"));
        historyCheckInColumn.setCellValueFactory(new PropertyValueFactory<>("checkInAt"));
        historyCheckOutColumn.setCellValueFactory(new PropertyValueFactory<>("checkOutAt"));
        historyHoursColumn.setCellValueFactory(cellData -> Bindings.createStringBinding(
                cellData.getValue()::getDisplayTotalHours));
    }

    private void loadAttendanceData() {
        try {
            Employee employee = employeeRepository.findById(currentUser().getEmployeeId());
            if (employee == null) {
                setErrorStatus("Employee profile not found.");
                return;
            }

            employeeSummaryLabel.setText(employee.getFullName() + " | ID: " + employee.getEmployeeId());

            AttendanceRecord todayRecord = attendanceService.getTodayRecord(currentUser().getEmployeeId());
            if (todayRecord == null) {
                todayStatusLabel.setText("No attendance marked today yet.");
                todayCheckInLabel.setText("Check-in: -");
                todayCheckOutLabel.setText("Check-out: -");
                todayHoursLabel.setText("Total Hours: -");
            } else {
                todayStatusLabel.setText(todayRecord.getCheckOutAt() == null || todayRecord.getCheckOutAt().isBlank()
                        ? "Checked in. Waiting for check-out."
                        : "Attendance completed for today.");
                todayCheckInLabel.setText("Check-in: " + safe(todayRecord.getCheckInAt()));
                todayCheckOutLabel.setText("Check-out: " + safe(todayRecord.getCheckOutAt()));
                todayHoursLabel.setText("Total Hours: " + todayRecord.getDisplayTotalHours());
            }

            historyTable.setItems(FXCollections.observableArrayList(
                    attendanceService.getRecentRecords(currentUser().getEmployeeId(), 10)
            ));
        } catch (SQLException e) {
            setErrorStatus("Could not load attendance data: " + e.getMessage());
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void setSuccessStatus(String message) {
        actionStatusLabel.getStyleClass().removeAll("status-error", "status-success");
        actionStatusLabel.getStyleClass().add("status-success");
        actionStatusLabel.setText(message);
    }

    private void setErrorStatus(String message) {
        actionStatusLabel.getStyleClass().removeAll("status-error", "status-success");
        actionStatusLabel.getStyleClass().add("status-error");
        actionStatusLabel.setText(message);
    }
}