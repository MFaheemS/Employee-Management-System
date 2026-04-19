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
        private final int docId;
        private final String employeeId;
        private final String fileName;
        private final String filePath;
        private final String fileType;
        private final String uploadedAt;
        private final String uploadedBy;

        public DocumentRecord(int docId, String employeeId, String fileName,
                              String filePath, String fileType,
                              String uploadedAt, String uploadedBy) {
            this.docId = docId;
            this.employeeId = employeeId;
            this.fileName = fileName;
            this.filePath = filePath;
            this.fileType = fileType;
            this.uploadedAt = uploadedAt;
            this.uploadedBy = uploadedBy;
        }

        public int getDocId() { return docId; }
        public String getEmployeeId() { return employeeId; }
        public String getFileName() { return fileName; }
        public String getFilePath() { return filePath; }
        public String getFileType() { return fileType != null ? fileType : ""; }
        public String getUploadedAt() { return uploadedAt; }
        public String getUploadedBy() { return uploadedBy; }
    }

    public DocumentRecord uploadDocument(AppUser uploader, String employeeId,
                                          File sourceFile) throws SQLException, IOException {
        if (!uploader.canManageDocuments()) {
            throw new IllegalArgumentException("You are not authorized to upload documents.");
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
                    "File type '" + ext + "' is not allowed. Supported: PDF, PNG, JPG, DOC, DOCX, TXT.");
        }

        File destDir = new File(DOCS_DIR + "/" + employeeId);
        if (!destDir.exists()) destDir.mkdirs();

        // Unique filename to avoid collisions
        String storedName = System.currentTimeMillis() + "_" + fileName;
        File destFile = new File(destDir, storedName);
        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        String sql = "INSERT INTO employee_documents "
                + "(employee_id, file_name, file_path, file_type, uploaded_by) "
                + "VALUES (?, ?, ?, ?, ?)";

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
                        destFile.getPath(), ext.toUpperCase(), null, uploader.getUsername());
            }
        }
    }

    public List<DocumentRecord> getDocumentsForEmployee(String employeeId) throws SQLException {
        String sql = "SELECT doc_id, employee_id, file_name, file_path, file_type, uploaded_at, uploaded_by "
                + "FROM employee_documents WHERE employee_id = ? ORDER BY uploaded_at DESC";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, employeeId);
            try (ResultSet rs = statement.executeQuery()) {
                return readRecords(rs);
            }
        }
    }

    public List<DocumentRecord> getAllDocuments() throws SQLException {
        String sql = "SELECT doc_id, employee_id, file_name, file_path, file_type, uploaded_at, uploaded_by "
                + "FROM employee_documents ORDER BY uploaded_at DESC";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
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
                    rs.getString("uploaded_by")
            ));
        }
        return list;
    }
}
