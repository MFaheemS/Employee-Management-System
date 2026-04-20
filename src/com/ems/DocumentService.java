package com.ems;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DocumentService {

    private static final String DOCS_DIR = "data/documents";

    public static class DocumentRecord {
        private final int    docId;
        private final String employeeId;
        private final String fileName;
        private final String filePath;
        private final String fileType;
        private final String uploadedAt;
        private final String uploadedBy;
        private final String status;

        public DocumentRecord(int docId, String employeeId, String fileName,
                              String filePath, String fileType,
                              String uploadedAt, String uploadedBy, String status) {
            this.docId       = docId;
            this.employeeId  = employeeId;
            this.fileName    = fileName;
            this.filePath    = filePath;
            this.fileType    = fileType;
            this.uploadedAt  = uploadedAt;
            this.uploadedBy  = uploadedBy;
            this.status      = status != null ? status : "Pending";
        }

        public int    getDocId()      { return docId; }
        public String getEmployeeId() { return employeeId; }
        public String getFileName()   { return fileName; }
        public String getFilePath()   { return filePath; }
        public String getFileType()   { return fileType != null ? fileType : ""; }
        public String getUploadedAt() { return uploadedAt; }
        public String getUploadedBy() { return uploadedBy; }
        public String getStatus()     { return status; }
    }

    // ── Upload ───────────────────────────────────────────────────────────────

    /**
     * Upload a document.
     * - Employees may upload documents for their OWN employee ID only.
     * - Admins and Managers cannot upload (they only view/delete).
     */
    public DocumentRecord uploadDocument(AppUser uploader, String employeeId,
                                          File sourceFile) throws SQLException, IOException {

        if (!uploader.canUploadDocuments()) {
            throw new IllegalArgumentException(
                    "Only employees can upload documents. Admins and managers may delete documents.");
        }

        // Employee can only upload to their own profile
        if (uploader.isEmployee()) {
            String ownId = uploader.getEmployeeId();
            if (ownId == null || !ownId.equalsIgnoreCase(employeeId)) {
                throw new IllegalArgumentException(
                        "You may only upload documents for your own employee profile.");
            }
        }

        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Please enter an Employee ID.");
        }

        if (sourceFile == null || !sourceFile.exists()) {
            throw new IllegalArgumentException("Please select a valid file.");
        }

        String fileName = sourceFile.getName();
        String ext = "";
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx >= 0) ext = fileName.substring(dotIdx + 1).toLowerCase();

        List<String> allowed = List.of("pdf", "png", "jpg", "jpeg", "doc", "docx", "txt");
        if (!allowed.contains(ext)) {
            throw new IllegalArgumentException(
                    "File type '" + ext + "' not allowed. Supported: PDF, PNG, JPG, DOC, DOCX, TXT.");
        }

        File destDir = new File(DOCS_DIR + "/" + employeeId);
        if (!destDir.exists()) destDir.mkdirs();

        String storedName = System.currentTimeMillis() + "_" + fileName;
        File destFile = new File(destDir, storedName);
        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        String sql = "INSERT INTO employee_documents "
                + "(employee_id, file_name, file_path, file_type, uploaded_by, status) "
                + "VALUES (?, ?, ?, ?, ?, 'Pending')";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, employeeId);
            statement.setString(2, fileName);
            statement.setString(3, destFile.getPath());
            statement.setString(4, ext.toUpperCase());
            statement.setString(5, uploader.getUsername());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                int newId = keys.next() ? keys.getInt(1) : -1;
                return new DocumentRecord(newId, employeeId, fileName,
                        destFile.getPath(), ext.toUpperCase(), null, uploader.getUsername(), "Pending");
            }
        }
    }

    // ── Approve / Reject ─────────────────────────────────────────────────────

    public void updateDocumentStatus(AppUser requestor, int docId, String status) throws SQLException {
        if (!requestor.canDeleteDocuments()) {
            throw new IllegalArgumentException("You are not authorized to manage documents.");
        }
        String sql = "UPDATE employee_documents SET status = ? WHERE doc_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, status);
            st.setInt(2, docId);
            st.executeUpdate();
        }
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    /**
     * Delete a document record (and its stored file).
     * Managers can delete documents for their department employees.
     */
    public void deleteDocument(AppUser requestor, int docId) throws SQLException, IOException {
        if (!requestor.canDeleteDocuments()) {
            throw new IllegalArgumentException("You are not authorized to delete documents.");
        }

        // Fetch the record first so we can delete the physical file
        String selectSql = "SELECT file_path FROM employee_documents WHERE doc_id = ?";
        String filePath = null;
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(selectSql)) {
            st.setInt(1, docId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) filePath = rs.getString("file_path");
            }
        }

        if (filePath == null) {
            throw new IllegalArgumentException("Document not found (ID: " + docId + ").");
        }

        // Remove DB record
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(
                     "DELETE FROM employee_documents WHERE doc_id = ?")) {
            st.setInt(1, docId);
            st.executeUpdate();
        }

        // Best-effort: delete physical file
        File f = new File(filePath);
        if (f.exists()) f.delete();
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public List<DocumentRecord> getDocumentsForEmployee(String employeeId) throws SQLException {
        String sql = "SELECT doc_id, employee_id, file_name, file_path, file_type, uploaded_at, uploaded_by, status "
                + "FROM employee_documents WHERE employee_id = ? ORDER BY uploaded_at DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, employeeId);
            try (ResultSet rs = st.executeQuery()) { return readRecords(rs); }
        }
    }

    public List<DocumentRecord> getDocumentsForManagerDepartment(String managerUsername) throws SQLException {
        String sql = "SELECT d.doc_id, d.employee_id, d.file_name, d.file_path, d.file_type, "
                + "d.uploaded_at, d.uploaded_by, d.status "
                + "FROM employee_documents d "
                + "JOIN employees e ON d.employee_id = e.employee_id "
                + "WHERE e.manager_username = ? "
                + "ORDER BY d.uploaded_at DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, managerUsername);
            try (ResultSet rs = st.executeQuery()) { return readRecords(rs); }
        }
    }

    public List<DocumentRecord> getAllDocuments() throws SQLException {
        String sql = "SELECT doc_id, employee_id, file_name, file_path, file_type, uploaded_at, uploaded_by, status "
                + "FROM employee_documents ORDER BY uploaded_at DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            return readRecords(rs);
        }
    }

    private List<DocumentRecord> readRecords(ResultSet rs) throws SQLException {
        List<DocumentRecord> list = new ArrayList<>();
        while (rs.next()) {
            list.add(new DocumentRecord(
                    rs.getInt("doc_id"),
                    rs.getString("employee_id"),
                    rs.getString("file_name"),
                    rs.getString("file_path"),
                    rs.getString("file_type"),
                    rs.getString("uploaded_at"),
                    rs.getString("uploaded_by"),
                    rs.getString("status")
            ));
        }
        return list;
    }
}
