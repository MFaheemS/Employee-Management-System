# Employee Management System

A desktop HR management application built with **JavaFX** and **SQLite**. It supports three roles — Admin, Manager, and Employee — each with a scoped set of features enforced at every level of the application.

---

## How to Run

**Requirements:** JDK 17 or later on your `PATH`. No other installation needed — JavaFX and the SQLite driver are bundled.

```
1. Double-click  compile.bat   (downloads SQLite driver if missing, then compiles)
2. Double-click  run.bat       (launches the application)
```

The database is created automatically at `data/ems.db` on first launch and seeded with sample accounts.

### Default Accounts

| Role     | Username  | Password | Employee ID |
|----------|-----------|----------|-------------|
| Admin    | `admin`   | `123`    | —           |
| Manager  | `alice.m` | `123`    | M001        |
| Manager  | `bob.k`   | `123`    | M002        |
| Employee | `j.smith` | `123`    | E001        |
| Employee | `s.jones` | `123`    | E002        |
| Employee | `r.patel` | `123`    | E003        |
| Employee | `l.chen`  | `123`    | E004        |
| Employee | `a.malik` | `123`    | E005        |

---

## Key Features

### Admin
- **Manager Management** — Add managers with auto-assigned `M###` IDs; permanently delete records from the Deactivate section (cascades across all tables)
- **Department Management** — Create departments (each requires a manager); remove departments that have no active employees; the assigned manager's job title auto-updates to `"[Dept] Manager"`
- **Unassigned Manager Tracking** — Dashboard and Department page show managers not yet assigned to any department; they have restricted access until assigned
- **Deactivation Approvals** — Approve or reject manager-submitted deactivation requests; approval permanently removes the record and frees the ID and username for reuse
- **Payroll Generation** — Generate monthly payroll slips for any employee

### Manager
- **Employee Management** — Add employees to their department with auto-assigned `E###` IDs
- **Leave Approvals** — Approve or reject leave requests submitted by their team
- **Deactivation Requests** — Submit requests to the admin to remove an employee
- **Team Dashboard** — Live KPIs: team size, pending leaves, today's attendance, unpaid employees this month
- **Documents** — View and manage documents uploaded by department employees
- *Managers without a department assignment see a restricted dashboard and cannot access any team features*

### Employee
- **Leave Applications** — Submit annual, sick, or casual leave requests
- **Attendance** — Check in and check out; view personal attendance history
- **Payroll Slips** — View own payroll records
- **Documents** — Upload personal documents for manager review
- **Profile Dashboard** — View current salary, leave balance, last net pay, and today's attendance status

---

## Project Structure

```
src/com/ems/
├── Main.java                     # Application entry point & scene router
├── Database.java                 # SQLite schema creation and seed data
├── AppSession.java / AppUser.java # Session & permission model
├── *Repository.java              # Data access layer (Employee, Department)
├── *Service.java                 # Business logic (Leave, Attendance, Payroll)
├── *Controller.java              # JavaFX UI controllers
└── *.fxml / styles.css           # UI layouts and stylesheet
data/
└── ems.db                        # SQLite database (auto-created)
lib/
└── sqlite-jdbc-*.jar             # Downloaded by compile.bat
javafx-sdk-25.0.2/               # Bundled JavaFX runtime
```
