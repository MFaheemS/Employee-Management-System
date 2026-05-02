package com.ems;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * White-box test runner — no JUnit, no JavaFX.
 * Uses an in-memory SQLite database so it never touches data/ems.db.
 * Run via:  test.bat
 */
public class WhiteBoxTestRunner {

    // ── Test state ────────────────────────────────────────────────────────────
    private static int passed = 0;
    private static int failed = 0;
    private static final List<String> failures = new ArrayList<>();

    // ── In-memory DB URL ──────────────────────────────────────────────────────
    private static final String TEST_DB = "jdbc:sqlite::memory:";
    private static Connection sharedConn; // keep open so :memory: persists

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println("  EMS White-Box Test Suite");
        System.out.println("=================================================\n");

        sharedConn = DriverManager.getConnection(TEST_DB);
        TestDatabase.bootstrap(sharedConn);
        overrideDbUrl(sharedConn);

        runPayrollServiceTests();
        runDocumentServiceTests();
        runAttendanceServiceTests();
        runDepartmentRepositoryTests();
        runEmployeeRepositoryTests();
        runLeaveServiceTests();

        sharedConn.close();

        System.out.println("\n=================================================");
        System.out.printf("  Results: %d passed, %d failed%n", passed, failed);
        System.out.println("=================================================");
        if (!failures.isEmpty()) {
            System.out.println("\nFailed tests:");
            failures.forEach(f -> System.out.println("  FAIL  " + f));
        }
        System.exit(failed > 0 ? 1 : 0);
    }

    // =========================================================================
    // PayrollService tests
    // =========================================================================
    private static void runPayrollServiceTests() throws Exception {
        System.out.println("--- PayrollService.generatePayroll ---");
        TestDatabase.reset(sharedConn);

        PayrollService svc = new PayrollService();
        AppUser admin    = new AppUser("admin",  "Admin",    null);
        AppUser manager  = new AppUser("alice.m","Manager",  "M001");
        AppUser employee = new AppUser("j.smith","Employee", "E001");

        // WB-P1: non-admin role blocked
        assertThrows("WB-P1 non-admin cannot generate payroll",
                () -> svc.generatePayroll(manager, "E001", 1, 2025, 50000, 0, 0, 0, null));

        // WB-P2: blank employee ID
        assertThrows("WB-P2 blank employeeId rejected",
                () -> svc.generatePayroll(admin, "", 1, 2025, 50000, 0, 0, 0, null));

        // WB-P3: employee not found
        assertThrows("WB-P3 unknown employee ID rejected",
                () -> svc.generatePayroll(admin, "E999", 1, 2025, 50000, 0, 0, 0, null));

        // WB-P4: inactive employee
        TestDatabase.insertEmployee(sharedConn, "E001", "John Smith", "E001", false);
        assertThrows("WB-P4 inactive employee blocked",
                () -> svc.generatePayroll(admin, "E001", 1, 2025, 50000, 0, 0, 0, null));

        // WB-P5: invalid month (0)
        TestDatabase.setActive(sharedConn, "E001", true);
        assertThrows("WB-P5 month=0 rejected",
                () -> svc.generatePayroll(admin, "E001", 0, 2025, 50000, 0, 0, 0, null));

        // WB-P6: invalid month (13)
        assertThrows("WB-P6 month=13 rejected",
                () -> svc.generatePayroll(admin, "E001", 13, 2025, 50000, 0, 0, 0, null));

        // WB-P7: invalid year
        assertThrows("WB-P7 year=1999 rejected",
                () -> svc.generatePayroll(admin, "E001", 1, 1999, 50000, 0, 0, 0, null));

        // WB-P8: negative salary value
        assertThrows("WB-P8 negative base salary rejected",
                () -> svc.generatePayroll(admin, "E001", 1, 2025, -1, 0, 0, 0, null));

        // WB-P9: negative deductions
        assertThrows("WB-P9 negative deductions rejected",
                () -> svc.generatePayroll(admin, "E001", 1, 2025, 50000, 0, 0, -500, null));

        // WB-P10: valid payroll — happy path, check net salary calculation
        PayrollRecord rec = svc.generatePayroll(admin, "E001", 1, 2025, 50000, 5, 200, 2000, "Test");
        assertEqual("WB-P10 gross = base + overtime", 51000.0, rec.getGrossSalary());
        assertEqual("WB-P10 net = gross - deductions", 49000.0, rec.getNetSalary());

        // WB-P11: duplicate payroll for same period
        assertThrows("WB-P11 duplicate period blocked",
                () -> svc.generatePayroll(admin, "E001", 1, 2025, 50000, 0, 0, 0, null));

        // WB-P12: deductions exceed gross — net floored at 0
        PayrollRecord floored = svc.generatePayroll(admin, "E001", 2, 2025, 50000, 0, 0, 999999, null);
        assertEqual("WB-P12 net floored at 0", 0.0, floored.getNetSalary());
    }

    // =========================================================================
    // DocumentService tests
    // =========================================================================
    private static void runDocumentServiceTests() throws Exception {
        System.out.println("\n--- DocumentService.uploadDocument ---");
        TestDatabase.reset(sharedConn);
        TestDatabase.insertEmployee(sharedConn, "E001", "John Smith", "E001", true);

        DocumentService svc   = new DocumentService();
        AppUser admin    = new AppUser("admin",  "Admin",    null);
        AppUser manager  = new AppUser("alice.m","Manager",  "M001");
        AppUser emp      = new AppUser("j.smith","Employee", "E001");
        AppUser empOther = new AppUser("s.jones","Employee", "E002");

        File pdf  = createTempFile("test.pdf");
        File exe  = createTempFile("malware.exe");
        File png  = createTempFile("photo.png");

        // WB-D1: admin cannot upload (only employees can)
        assertThrows("WB-D1 admin upload blocked",
                () -> svc.uploadDocument(admin, "E001", pdf));

        // WB-D2: manager cannot upload
        assertThrows("WB-D2 manager upload blocked",
                () -> svc.uploadDocument(manager, "E001", pdf));

        // WB-D3: employee uploads to another employee's profile
        assertThrows("WB-D3 employee cannot upload to other profile",
                () -> svc.uploadDocument(emp, "E002", pdf));

        // WB-D4: disallowed file extension
        assertThrows("WB-D4 .exe extension rejected",
                () -> svc.uploadDocument(emp, "E001", exe));

        // WB-D5: null file
        assertThrows("WB-D5 null file rejected",
                () -> svc.uploadDocument(emp, "E001", null));

        // WB-D6: non-existent file
        assertThrows("WB-D6 missing file rejected",
                () -> svc.uploadDocument(emp, "E001", new File("no_such_file.pdf")));

        // WB-D7: valid upload — PDF allowed
        DocumentService.DocumentRecord rec = svc.uploadDocument(emp, "E001", pdf);
        assertNotNull("WB-D7 valid PDF upload returns record", rec);
        assertEqual("WB-D7 file type stored as PDF", "PDF", rec.getFileType());

        // WB-D8: valid upload — PNG allowed
        DocumentService.DocumentRecord rec2 = svc.uploadDocument(emp, "E001", png);
        assertNotNull("WB-D8 valid PNG upload returns record", rec2);

        // Cleanup temp dirs
        new File("data/documents/E001").listFiles(f -> { f.delete(); return false; });
        new File("data/documents/E001").delete();
        new File("data/documents").delete();
        pdf.delete(); exe.delete(); png.delete();
    }

    // =========================================================================
    // AttendanceService tests
    // =========================================================================
    private static void runAttendanceServiceTests() throws Exception {
        System.out.println("\n--- AttendanceService.markAttendance + getTotalHoursForMonth ---");
        TestDatabase.reset(sharedConn);
        TestDatabase.insertEmployee(sharedConn, "E001", "John Smith", "E001", true);
        TestDatabase.insertEmployee(sharedConn, "E002", "Inactive Emp", "E002", false);

        AttendanceService svc = new AttendanceService();
        AppUser active   = new AppUser("j.smith", "Employee", "E001");
        AppUser inactive = new AppUser("s.jones", "Employee", "E002");
        AppUser noProfile= new AppUser("admin",   "Admin",    null);

        // WB-A1: account not linked to employee
        assertThrows("WB-A1 no employee profile blocked",
                () -> svc.markAttendance(noProfile));

        // WB-A2: inactive employee blocked
        assertThrows("WB-A2 inactive employee blocked",
                () -> svc.markAttendance(inactive));

        // WB-A3: first mark of day = check-in
        AttendanceRecord checkin = svc.markAttendance(active);
        assertNotNull("WB-A3 check-in record created", checkin);
        assertNotNull("WB-A3 check_in_at populated",   checkin.getCheckInAt());
        assertNull("WB-A3 check_out_at null after check-in", checkin.getCheckOutAt());

        // WB-A4: second mark = check-out
        AttendanceRecord checkout = svc.markAttendance(active);
        assertNotNull("WB-A4 check-out record returned",    checkout);
        assertNotNull("WB-A4 check_out_at populated",       checkout.getCheckOutAt());

        // WB-A5: third mark = already completed
        assertThrows("WB-A5 attendance already completed",
                () -> svc.markAttendance(active));

        // WB-A6: getTotalHoursForMonth returns 0 for month with no records
        double hours = svc.getTotalHoursForMonth("E001", 6, 2025);
        assertCondition("WB-A6 total hours >= 0 for month with record", hours >= 0);

        // WB-A7: month with no records returns exactly 0.0
        double noHours = svc.getTotalHoursForMonth("E001", 1, 2020);
        assertEqual("WB-A7 no records returns 0.0 hours", 0.0, noHours);
    }

    // =========================================================================
    // DepartmentRepository tests
    // =========================================================================
    private static void runDepartmentRepositoryTests() throws Exception {
        System.out.println("\n--- DepartmentRepository ---");
        TestDatabase.reset(sharedConn);
        TestDatabase.insertEmployee(sharedConn, "E001", "John Smith", "Engineering", true);
        TestDatabase.insertUser(sharedConn, "alice.m", "Manager", "M001");
        TestDatabase.insertDepartment(sharedConn, "Engineering", "alice.m");

        DepartmentRepository repo = new DepartmentRepository();

        // WB-DR1: hasEmployeesInDepartment — dept with active employee → true
        assertCondition("WB-DR1 dept with active employee returns true",
                repo.hasEmployeesInDepartment("Engineering"));

        // WB-DR2: hasEmployeesInDepartment — non-existent dept → false
        assertCondition("WB-DR2 unknown dept returns false",
                !repo.hasEmployeesInDepartment("NonExistentDept"));

        // WB-DR3: hasEmployeesInDepartment — dept exists but employee inactive
        TestDatabase.setActive(sharedConn, "E001", false);
        assertCondition("WB-DR3 dept with only inactive employees returns false",
                !repo.hasEmployeesInDepartment("Engineering"));
        TestDatabase.setActive(sharedConn, "E001", true);

        // WB-DR4: hasDepartment — assigned manager → true
        assertCondition("WB-DR4 assigned manager hasDepartment=true",
                repo.hasDepartment("alice.m"));

        // WB-DR5: hasDepartment — unassigned manager → false
        TestDatabase.insertUser(sharedConn, "bob.k", "Manager", "M002");
        assertCondition("WB-DR5 unassigned manager hasDepartment=false",
                !repo.hasDepartment("bob.k"));

        // WB-DR6: getUnassignedManagers lists only unassigned managers
        List<String> unassigned = repo.getUnassignedManagers();
        assertCondition("WB-DR6 bob.k in unassigned list", unassigned.contains("bob.k"));
        assertCondition("WB-DR6 alice.m NOT in unassigned list", !unassigned.contains("alice.m"));

        // WB-DR7: deleteDepartment removes the row
        List<Department> before = repo.getAllDepartments();
        int deptId = before.get(0).getDepartmentId();
        repo.deleteDepartment(deptId);
        assertCondition("WB-DR7 department deleted",
                repo.getAllDepartments().isEmpty());
    }

    // =========================================================================
    // EmployeeRepository tests
    // =========================================================================
    private static void runEmployeeRepositoryTests() throws Exception {
        System.out.println("\n--- EmployeeRepository ---");
        TestDatabase.reset(sharedConn);

        EmployeeRepository repo = new EmployeeRepository();

        // WB-ER1: getNextAvailableId — no records → M001
        assertEqual("WB-ER1 first available ID is M001",
                "M001", repo.getNextAvailableId("M"));

        // WB-ER2: getNextAvailableId — M001 exists → M002
        TestDatabase.insertEmployee(sharedConn, "M001", "Alice Manager", "Unassigned", true);
        assertEqual("WB-ER2 next available after M001 is M002",
                "M002", repo.getNextAvailableId("M"));

        // WB-ER3: getNextAvailableId — gap at M002 reused after deletion
        TestDatabase.insertEmployee(sharedConn, "M002", "Bob Manager", "Unassigned", true);
        TestDatabase.insertUser(sharedConn, "bob.k", "Manager", "M002");
        repo.deleteEmployeeCompletely("M002");
        assertEqual("WB-ER3 deleted ID M002 becomes available again",
                "M002", repo.getNextAvailableId("M"));

        // WB-ER4: deleteEmployeeCompletely returns false for non-existent ID
        assertCondition("WB-ER4 deleting non-existent ID returns false",
                !repo.deleteEmployeeCompletely("E999"));

        // WB-ER5: findById returns null for unknown ID
        assertNull("WB-ER5 findById unknown returns null",
                repo.findById("E999"));

        // WB-ER6: findById returns correct record
        Employee found = repo.findById("M001");
        assertNotNull("WB-ER6 findById known returns record", found);
        assertEqual("WB-ER6 correct name returned", "Alice Manager", found.getFullName());

        // WB-ER7: E-series and M-series are independent
        assertEqual("WB-ER7 E-series starts at E001 independent of M-series",
                "E001", repo.getNextAvailableId("E"));
    }

    // =========================================================================
    // LeaveManagementService tests
    // =========================================================================
    private static void runLeaveServiceTests() throws Exception {
        System.out.println("\n--- LeaveManagementService ---");
        TestDatabase.reset(sharedConn);
        TestDatabase.insertUser(sharedConn,     "alice.m", "Manager",  "M001");
        TestDatabase.insertEmployee(sharedConn, "M001", "Alice Manager", "Engineering", true);
        TestDatabase.insertEmployee(sharedConn, "E001", "John Smith",    "Engineering", true);
        TestDatabase.setManager(sharedConn, "E001", "alice.m");

        LeaveManagementService svc = new LeaveManagementService();
        AppUser emp     = new AppUser("j.smith", "Employee", "E001");
        AppUser manager = new AppUser("alice.m", "Manager",  "M001");
        AppUser noId    = new AppUser("admin",   "Admin",    null);

        LocalDate today    = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        // WB-L1: user without employee profile blocked
        assertThrows("WB-L1 no employee profile blocked",
                () -> svc.submitLeaveRequest(noId, "Annual", today, tomorrow));

        // WB-L2: blank leave type
        assertThrows("WB-L2 blank leave type rejected",
                () -> svc.submitLeaveRequest(emp, "", today, tomorrow));

        // WB-L3: end before start
        assertThrows("WB-L3 end before start rejected",
                () -> svc.submitLeaveRequest(emp, "Annual", tomorrow, today));

        // WB-L4: insufficient balance (request more than balance)
        TestDatabase.setLeaveBalance(sharedConn, "E001", 2);
        assertThrows("WB-L4 insufficient balance rejected",
                () -> svc.submitLeaveRequest(emp, "Annual", today, today.plusDays(9)));

        // WB-L5: valid request — single day
        TestDatabase.setLeaveBalance(sharedConn, "E001", 20);
        String mgr = svc.submitLeaveRequest(emp, "Annual", today, today);
        assertEqual("WB-L5 returns manager username", "alice.m", mgr);

        // WB-L6: decideLeaveRequest — non-manager blocked
        AppUser empApprover = new AppUser("j.smith", "Employee", "E001");
        List<LeaveRequest> pending = svc.getPendingRequestsForApprover(manager);
        assertCondition("WB-L6 pending list not empty", !pending.isEmpty());
        int reqId = pending.get(0).getRequestId();
        assertThrows("WB-L6 employee cannot decide leave",
                () -> svc.decideLeaveRequest(empApprover, reqId, "Approved", null));

        // WB-L7: invalid action string
        assertThrows("WB-L7 invalid action rejected",
                () -> svc.decideLeaveRequest(manager, reqId, "Maybe", null));

        // WB-L8: valid approval — balance deducted
        svc.decideLeaveRequest(manager, reqId, "Approved", "OK");
        // re-query balance via repo
        EmployeeRepository repo = new EmployeeRepository();
        Employee updated = repo.findById("E001");
        assertEqual("WB-L8 leave balance deducted after approval", 19, updated.getLeaveBalance());

        // WB-L9: approving already-decided request throws
        assertThrows("WB-L9 already-processed request rejected",
                () -> svc.decideLeaveRequest(manager, reqId, "Approved", null));
    }

    // =========================================================================
    // Assertion helpers
    // =========================================================================
    private static void assertThrows(String name, ThrowingRunnable action) {
        try {
            action.run();
            fail(name + " — expected exception but none was thrown");
        } catch (Exception e) {
            pass(name);
        }
    }

    private static void assertEqual(String name, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            pass(name);
        } else {
            fail(name + " — expected [" + expected + "] but got [" + actual + "]");
        }
    }

    private static void assertEqual(String name, double expected, double actual) {
        if (Math.abs(expected - actual) < 0.001) {
            pass(name);
        } else {
            fail(name + " — expected [" + expected + "] but got [" + actual + "]");
        }
    }

    private static void assertEqual(String name, int expected, int actual) {
        if (expected == actual) {
            pass(name);
        } else {
            fail(name + " — expected [" + expected + "] but got [" + actual + "]");
        }
    }

    private static void assertNotNull(String name, Object obj) {
        if (obj != null) pass(name);
        else fail(name + " — expected non-null but got null");
    }

    private static void assertNull(String name, Object obj) {
        if (obj == null) pass(name);
        else fail(name + " — expected null but got [" + obj + "]");
    }

    private static void assertCondition(String name, boolean condition) {
        if (condition) pass(name);
        else fail(name + " — condition was false");
    }

    private static void pass(String name) {
        passed++;
        System.out.println("  PASS  " + name);
    }

    private static void fail(String name) {
        failed++;
        failures.add(name);
        System.out.println("  FAIL  " + name);
    }

    private static File createTempFile(String name) throws IOException {
        File f = File.createTempFile(name.replace(".", "_"), "." + name.substring(name.lastIndexOf('.') + 1));
        f.deleteOnExit();
        return f;
    }

    @FunctionalInterface
    interface ThrowingRunnable { void run() throws Exception; }

    // =========================================================================
    // Test database helpers — isolated from main data/ems.db
    // =========================================================================
    static class TestDatabase {

        /** Create schema tables in the shared in-memory connection. */
        static void bootstrap(Connection conn) throws SQLException {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("PRAGMA foreign_keys = OFF");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS users ("
                        + "username TEXT PRIMARY KEY, password TEXT NOT NULL, "
                        + "role TEXT NOT NULL DEFAULT 'Employee', employee_id TEXT)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS employees ("
                        + "employee_id TEXT PRIMARY KEY, full_name TEXT NOT NULL, "
                        + "job_title TEXT NOT NULL DEFAULT 'Staff', "
                        + "department TEXT NOT NULL DEFAULT 'General', "
                        + "email TEXT NOT NULL DEFAULT 'test@test.com', "
                        + "phone TEXT, is_active INTEGER NOT NULL DEFAULT 1, "
                        + "role TEXT NOT NULL DEFAULT 'Employee', "
                        + "manager_username TEXT, leave_balance INTEGER NOT NULL DEFAULT 20, "
                        + "salary REAL NOT NULL DEFAULT 0, last_net_salary REAL DEFAULT 0, "
                        + "deactivation_reason TEXT, deactivated_at TEXT)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS departments ("
                        + "department_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "department_name TEXT NOT NULL, manager_username TEXT, created_at TEXT)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS leave_requests ("
                        + "request_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "employee_id TEXT NOT NULL, manager_username TEXT NOT NULL, "
                        + "leave_type TEXT NOT NULL, start_date TEXT NOT NULL, "
                        + "end_date TEXT NOT NULL, days_requested INTEGER NOT NULL, "
                        + "status TEXT NOT NULL DEFAULT 'Pending', comments TEXT, "
                        + "requested_at TEXT DEFAULT CURRENT_TIMESTAMP, "
                        + "decision_at TEXT, decision_by TEXT)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS attendance_records ("
                        + "record_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "employee_id TEXT NOT NULL, attendance_date TEXT NOT NULL, "
                        + "check_in_at TEXT, check_out_at TEXT, total_hours REAL)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS payroll_records ("
                        + "record_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "employee_id TEXT NOT NULL, month INTEGER NOT NULL, year INTEGER NOT NULL, "
                        + "base_salary REAL, overtime_hours REAL DEFAULT 0, overtime_rate REAL DEFAULT 0, "
                        + "deductions REAL DEFAULT 0, gross_salary REAL, net_salary REAL, "
                        + "generated_at TEXT DEFAULT CURRENT_TIMESTAMP, "
                        + "generated_by TEXT, notes TEXT)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS employee_documents ("
                        + "doc_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "employee_id TEXT NOT NULL, file_name TEXT NOT NULL, "
                        + "file_path TEXT NOT NULL, file_type TEXT, "
                        + "uploaded_at TEXT DEFAULT CURRENT_TIMESTAMP, "
                        + "uploaded_by TEXT, status TEXT DEFAULT 'Pending')");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS deactivation_requests ("
                        + "request_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "employee_id TEXT NOT NULL, requested_by TEXT, reason TEXT, "
                        + "status TEXT DEFAULT 'Pending', decided_by TEXT, "
                        + "requested_at TEXT DEFAULT CURRENT_TIMESTAMP, decided_at TEXT)");
            }
        }

        /** Wipe all rows between test groups. */
        static void reset(Connection conn) throws SQLException {
            try (Statement st = conn.createStatement()) {
                for (String t : new String[]{
                        "deactivation_requests","employee_documents","payroll_records",
                        "attendance_records","leave_requests","departments","employees","users"}) {
                    st.executeUpdate("DELETE FROM " + t);
                }
            }
        }

        static void insertEmployee(Connection conn, String id, String name,
                                   String dept, boolean active) throws SQLException {
            try (PreparedStatement st = conn.prepareStatement(
                    "INSERT OR IGNORE INTO employees "
                    + "(employee_id, full_name, job_title, department, email, is_active, role, leave_balance, salary) "
                    + "VALUES (?, ?, 'Staff', ?, 'test@test.com', ?, "
                    + "(CASE WHEN ? LIKE 'M%' THEN 'Manager' ELSE 'Employee' END), 20, 50000)")) {
                st.setString(1, id);
                st.setString(2, name);
                st.setString(3, dept);
                st.setInt(4, active ? 1 : 0);
                st.setString(5, id);
                st.executeUpdate();
            }
        }

        static void insertUser(Connection conn, String username, String role,
                               String empId) throws SQLException {
            try (PreparedStatement st = conn.prepareStatement(
                    "INSERT OR IGNORE INTO users (username, password, role, employee_id) VALUES (?, '123', ?, ?)")) {
                st.setString(1, username);
                st.setString(2, role);
                st.setString(3, empId);
                st.executeUpdate();
            }
        }

        static void insertDepartment(Connection conn, String name, String manager) throws SQLException {
            try (PreparedStatement st = conn.prepareStatement(
                    "INSERT INTO departments (department_name, manager_username) VALUES (?, ?)")) {
                st.setString(1, name);
                st.setString(2, manager);
                st.executeUpdate();
            }
        }

        static void setActive(Connection conn, String empId, boolean active) throws SQLException {
            try (PreparedStatement st = conn.prepareStatement(
                    "UPDATE employees SET is_active = ? WHERE employee_id = ?")) {
                st.setInt(1, active ? 1 : 0);
                st.setString(2, empId);
                st.executeUpdate();
            }
        }

        static void setLeaveBalance(Connection conn, String empId, int balance) throws SQLException {
            try (PreparedStatement st = conn.prepareStatement(
                    "UPDATE employees SET leave_balance = ? WHERE employee_id = ?")) {
                st.setInt(1, balance);
                st.setString(2, empId);
                st.executeUpdate();
            }
        }

        static void setManager(Connection conn, String empId, String managerUsername) throws SQLException {
            try (PreparedStatement st = conn.prepareStatement(
                    "UPDATE employees SET manager_username = ? WHERE employee_id = ?")) {
                st.setString(1, managerUsername);
                st.setString(2, empId);
                st.executeUpdate();
            }
        }
    }

    // =========================================================================
    // Redirect Database.getConnection() to use the shared in-memory connection
    // =========================================================================
    static void overrideDbUrl(Connection conn) {
        // We pass the shared connection to the TestConnectionProvider so all
        // service classes hit the same in-memory DB without touching ems.db.
        TestConnectionProvider.setConnection(conn);
    }
}
