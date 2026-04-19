package com.ems;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DocumentController extends BaseController {

    private final DocumentService documentService = new DocumentService();

    @FXML private Label userLabel;
    @FXML private VBox uploadSection;
    @FXML private TextField employeeIdField;
    @FXML private TextField filePathField;
    @FXML private TextField filterField;
    @FXML private Label uploadStatusLabel;

    @FXML private Button dashboardNavButton;
    @FXML private Button employeeAddNavButton;
    @FXML private Button employeeDeactivateNavButton;
    @FXML private Button employeeSearchNavButton;
    @FXML private Button attendanceNavButton;
    @FXML private Button leaveApplyNavButton;
    @FXML private Button leaveApprovalsNavButton;
    @FXML private Button payrollNavButton;
    @FXML private Button documentsNavButton;

    @FXML private TableView<DocumentService.DocumentRecord> documentsTable;
    @FXML private TableColumn<DocumentService.DocumentRecord, String> colEmpId;
    @FXML private TableColumn<DocumentService.DocumentRecord, String> colFileName;
    @FXML private TableColumn<DocumentService.DocumentRecord, String> colFileType;
    @FXML private TableColumn<DocumentService.DocumentRecord, String> colUploadedAt;
    @FXML private TableColumn<DocumentService.DocumentRecord, String> colUploadedBy;

    private File selectedFile;

    @FXML
    private void initialize() {
        if (!ensureLoggedIn()) return;

        configureSidebarNavigation(userLabel, employeeAddNavButton, employeeDeactivateNavButton,
                employeeSearchNavButton, attendanceNavButton, leaveApplyNavButton, leaveApprovalsNavButton);
        configureAdditionalNavigation(dashboardNavButton, payrollNavButton, documentsNavButton);

        boolean canUpload = currentUser().canManageDocuments();
        uploadSection.setVisible(canUpload);
        uploadSection.setManaged(canUpload);

        configureTable();

        // Employees see only their own documents
        if (currentUser().isEmployee() && currentUser().getEmployeeId() != null) {
            filterField.setText(currentUser().getEmployeeId());
            handleFilter();
        } else {
            loadAllDocuments();
        }
    }

    @FXML
    private void handleBrowse() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Employee Document");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Supported Files",
                        "*.pdf", "*.png", "*.jpg", "*.jpeg", "*.doc", "*.docx", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = chooser.showOpenDialog(filePathField.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            filePathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleUpload() {
        uploadStatusLabel.setText("");
        String empId = employeeIdField.getText().trim();

        try {
            DocumentService.DocumentRecord record =
                    documentService.uploadDocument(currentUser(), empId, selectedFile);
            setSuccess("Document '" + record.getFileName() + "' uploaded successfully for employee " + empId + ".");
            selectedFile = null;
            filePathField.clear();
            employeeIdField.clear();
            loadAllDocuments();
        } catch (IllegalArgumentException e) {
            setError(e.getMessage());
        } catch (IOException e) {
            setError("File copy error: " + e.getMessage());
        } catch (SQLException e) {
            setError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleFilter() {
        String filter = filterField.getText().trim();
        if (filter.isEmpty()) {
            loadAllDocuments();
            return;
        }
        try {
            List<DocumentService.DocumentRecord> records =
                    documentService.getDocumentsForEmployee(filter);
            documentsTable.setItems(FXCollections.observableArrayList(records));
        } catch (SQLException e) {
            setError("Could not filter: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowAll() {
        filterField.clear();
        loadAllDocuments();
    }

    @FXML private void goToDashboard() { navigate(() -> Main.showDashboard()); }
    @FXML private void goToAdd() { navigate(() -> Main.showEmployeeAdd()); }
    @FXML private void goToDeactivate() { navigate(() -> Main.showEmployeeDeactivate()); }
    @FXML private void goToEmployeeSearch() { navigate(() -> Main.showEmployeeSearch()); }
    @FXML private void goToAttendance() { navigate(() -> Main.showAttendance()); }
    @FXML private void goToLeaveApply() { navigate(() -> Main.showLeaveApplication()); }
    @FXML private void goToLeaveApprovals() { navigate(() -> Main.showLeaveApprovals()); }
    @FXML private void goToPayroll() { navigate(() -> Main.showPayroll()); }
    @FXML private void goToDocuments() { /* already here */ }
    @FXML protected void handleLogout() { super.handleLogout(); }

    private void navigate(NavigationAction action) {
        try { action.run(); } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error", e.getMessage());
        }
    }

    private void loadAllDocuments() {
        try {
            List<DocumentService.DocumentRecord> records;
            if (currentUser().canManageDocuments()) {
                records = documentService.getAllDocuments();
            } else {
                String eid = currentUser().getEmployeeId();
                records = eid != null ? documentService.getDocumentsForEmployee(eid)
                        : java.util.Collections.emptyList();
            }
            documentsTable.setItems(FXCollections.observableArrayList(records));
        } catch (SQLException e) {
            setError("Could not load documents: " + e.getMessage());
        }
    }

    private void configureTable() {
        documentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colEmpId.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        colFileName.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        colFileType.setCellValueFactory(new PropertyValueFactory<>("fileType"));
        colUploadedAt.setCellValueFactory(new PropertyValueFactory<>("uploadedAt"));
        colUploadedBy.setCellValueFactory(new PropertyValueFactory<>("uploadedBy"));
    }

    private void setSuccess(String msg) {
        uploadStatusLabel.getStyleClass().removeAll("status-error", "status-success");
        uploadStatusLabel.getStyleClass().add("status-success");
        uploadStatusLabel.setText(msg);
    }

    private void setError(String msg) {
        uploadStatusLabel.getStyleClass().removeAll("status-error", "status-success");
        uploadStatusLabel.getStyleClass().add("status-error");
        uploadStatusLabel.setText(msg);
    }

    @FunctionalInterface
    private interface NavigationAction {
        void run() throws Exception;
    }
}
