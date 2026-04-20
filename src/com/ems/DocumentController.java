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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DocumentController extends BaseController {

    private final DocumentService documentService = new DocumentService();

    @FXML private Label userLabel;
    @FXML private Label roleInfoLabel;

    /** Upload section – shown to Employees only (upload their own docs). */
    @FXML private VBox uploadSection;
    @FXML private TextField filePathField;
    @FXML private Label uploadStatusLabel;

    /** Filter section – shown to Admin/Manager to filter by any employee ID. */
    @FXML private VBox filterSection;
    @FXML private TextField filterField;

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

    @FXML private TableView<DocumentService.DocumentRecord> documentsTable;
    @FXML private TableColumn<DocumentService.DocumentRecord, String> colEmpId;
    @FXML private TableColumn<DocumentService.DocumentRecord, String> colFileName;
    @FXML private TableColumn<DocumentService.DocumentRecord, String> colFileType;
    @FXML private TableColumn<DocumentService.DocumentRecord, String> colUploadedAt;
    @FXML private TableColumn<DocumentService.DocumentRecord, String> colUploadedBy;
    @FXML private TableColumn<DocumentService.DocumentRecord, String> colStatus;

    @FXML private VBox managerActionsSection;
    @FXML private Button approveDocButton;
    @FXML private Button deleteDocButton;

    @FXML private VBox employeeActionsSection;
    @FXML private Button openFileButton;

    private File selectedFile;

    @FXML
    private void initialize() {
        if (!ensureLoggedIn()) return;

        configureSidebarNavigation(userLabel, employeeAddNavButton, employeeDeactivateNavButton,
                employeeSearchNavButton, attendanceNavButton, leaveApplyNavButton, leaveApprovalsNavButton);
        configureAdditionalNavigation(dashboardNavButton, payrollNavButton, documentsNavButton);
        configureDepartmentNavigation(departmentNavButton);

        AppUser user = currentUser();

        // Role banner
        if (roleInfoLabel != null) {
            if (user.isEmployee()) {
                roleInfoLabel.setText("Upload documents to your own profile below.");
            } else if (user.isManager()) {
                roleInfoLabel.setText("Viewing documents uploaded by your department employees.");
            } else {
                roleInfoLabel.setText("Documents are managed by employees and viewed by their managers.");
            }
        }

        // Upload section: Employee only
        boolean canUpload = user.canUploadDocuments();
        if (uploadSection != null) {
            uploadSection.setVisible(canUpload);
            uploadSection.setManaged(canUpload);
        }

        // Filter section: Manager only (filter within their dept)
        boolean canViewAll = user.canViewAllDocuments();
        if (filterSection != null) {
            filterSection.setVisible(canViewAll);
            filterSection.setManaged(canViewAll);
        }

        // Manager actions: Approve / Reject / Delete
        boolean isManager = user.isManager();
        if (managerActionsSection != null) {
            managerActionsSection.setVisible(isManager);
            managerActionsSection.setManaged(isManager);
        }

        // Employee action: Open file
        boolean isEmployee = user.isEmployee();
        if (employeeActionsSection != null) {
            employeeActionsSection.setVisible(isEmployee);
            employeeActionsSection.setManaged(isEmployee);
        }

        configureTable();
        loadDocuments();
    }

    @FXML
    private void handleBrowse() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Document to Upload");
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
        String empId = currentUser().getEmployeeId();

        try {
            DocumentService.DocumentRecord record =
                    documentService.uploadDocument(currentUser(), empId, selectedFile);
            setSuccess("Document '" + record.getFileName() + "' uploaded successfully.");
            selectedFile = null;
            filePathField.clear();
            loadDocuments();
        } catch (IllegalArgumentException e) {
            setError(e.getMessage());
        } catch (IOException e) {
            setError("File error: " + e.getMessage());
        } catch (SQLException e) {
            setError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteDocument() {
        DocumentService.DocumentRecord selected = documentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setError("Select a document row first.");
            return;
        }
        try {
            documentService.deleteDocument(currentUser(), selected.getDocId());
            setSuccess("Document '" + selected.getFileName() + "' deleted.");
            loadDocuments();
        } catch (IllegalArgumentException e) {
            setError(e.getMessage());
        } catch (IOException | SQLException e) {
            setError("Delete failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleApproveDocument() {
        updateSelectedDocStatus("Approved");
    }

    private void updateSelectedDocStatus(String status) {
        DocumentService.DocumentRecord selected = documentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setError("Select a document row first."); return; }
        try {
            documentService.updateDocumentStatus(currentUser(), selected.getDocId(), status);
            setSuccess("Document '" + selected.getFileName() + "' marked as " + status + ".");
            loadDocuments();
        } catch (IllegalArgumentException e) {
            setError(e.getMessage());
        } catch (SQLException e) {
            setError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenFile() {
        DocumentService.DocumentRecord selected = documentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setError("Select a document row first."); return; }
        File file = new File(selected.getFilePath());
        if (!file.exists()) { setError("File not found on disk: " + selected.getFilePath()); return; }
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            setError("Could not open file: " + e.getMessage());
        }
    }

    @FXML
    private void handleFilter() {
        String filter = filterField != null ? filterField.getText().trim() : "";
        if (filter.isEmpty()) { loadDocuments(); return; }
        try {
            List<DocumentService.DocumentRecord> records = documentService.getDocumentsForEmployee(filter);
            documentsTable.setItems(FXCollections.observableArrayList(records));
        } catch (SQLException e) {
            setError("Could not filter: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowAll() {
        if (filterField != null) filterField.clear();
        loadDocuments();
    }

    @FXML private void goToDashboard()       { navigate(Main::showDashboard); }
    @FXML private void goToAdd()             { navigate(Main::showEmployeeAdd); }
    @FXML private void goToDeactivate()      { navigate(Main::showEmployeeDeactivate); }
    @FXML private void goToDepartments()     { navigate(Main::showDepartmentManagement); }
    @FXML private void goToEmployeeSearch()  { navigate(Main::showEmployeeSearch); }
    @FXML private void goToAttendance()      { navigate(Main::showAttendance); }
    @FXML private void goToLeaveApply()      { navigate(Main::showLeaveApplication); }
    @FXML private void goToLeaveApprovals()  { navigate(Main::showLeaveApprovals); }
    @FXML private void goToPayroll()         { navigate(Main::showPayroll); }
    @FXML private void goToDocuments()       { /* already here */ }
    @FXML protected void handleLogout()      { super.handleLogout(); }

    private void navigate(NavigationAction action) {
        try { action.run(); } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error", e.getMessage());
        }
    }

    private void loadDocuments() {
        try {
            List<DocumentService.DocumentRecord> records;
            AppUser user = currentUser();
            if (user.isManager()) {
                records = documentService.getDocumentsForManagerDepartment(user.getUsername());
            } else {
                String eid = user.getEmployeeId();
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
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void setSuccess(String msg) {
        if (uploadStatusLabel == null) return;
        uploadStatusLabel.getStyleClass().removeAll("status-error", "status-success");
        uploadStatusLabel.getStyleClass().add("status-success");
        uploadStatusLabel.setText(msg);
    }

    private void setError(String msg) {
        if (uploadStatusLabel == null) return;
        uploadStatusLabel.getStyleClass().removeAll("status-error", "status-success");
        uploadStatusLabel.getStyleClass().add("status-error");
        uploadStatusLabel.setText(msg);
    }

    @FunctionalInterface
    private interface NavigationAction { void run() throws Exception; }
}
