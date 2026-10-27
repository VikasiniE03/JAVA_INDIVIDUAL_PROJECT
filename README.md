# KEC R2024 Student Academic & Continuous Evaluation System

## Project Information

- **Project Title:** Academic Management System
- **Institution:** Kongu Engineering College
- **Department:** Information Technology
- **Course:** 24ITT32 – Programming in Java
- **Developed by:** VIKASINI E
- **Roll Number:** 25ITR172

## Overview

The KEC R2024 Student Academic & Continuous Evaluation System is a Java console application that manages student academic information using JDBC and MySQL.

The system supports student registration, course management, enrollment, attendance tracking, assessment mark entry, semester mark sheets, attendance defaulter reports, and department performance summaries.

## Features

1. **Student Management** – Register students and list existing records.
2. **Course Management** – Manage course details.
3. **Enrollment / Attendance** – Enroll students and maintain attendance totals.
4. **Enter Marks** – Record CIA-1, CIA-2, Phase 2, and ESE marks.
5. **Student Semester Mark Sheet** – View student academic results.
6. **Attendance Defaulters** – Identify students whose attendance is below 65%.
7. **Department Summary** – View department academic performance.
8. **ESE Absence Handling** – Record ESE absence where supported by the application.

## Technology Stack

- Java 17
- JDBC
- MySQL
- MySQL Connector/J

## Project Structure

```text
PROJECT_JAVA/
├── src/main/java/
│   ├── MainApp.java
│   ├── DBConnection.java
│   ├── StudentDAO.java
│   ├── AcademicDAO.java
│   └── EvaluationEngine.java
├── kec_schema.sql
├── pom.xml
├── .gitignore
└── README.md
```

## Database Configuration

The application uses the existing MySQL database named `kec_academic`. The database contains these tables:

- `students`
- `courses`
- `enrollments`
- `assessment_marks`

To inspect the database tables, run:

```sql
USE kec_academic;
SHOW TABLES;
```

## Configure the Database Connection

In a PowerShell terminal, set these environment variables for the current terminal session. Replace the example values with your own MySQL credentials:

```powershell
$env:KEC_DB_URL = "jdbc:mysql://localhost:3306/kec_academic"
$env:KEC_DB_USER = "your_mysql_username"
$env:KEC_DB_PASSWORD = "your_mysql_password"
```

## Run the Application

Open a terminal in the directory containing `pom.xml`, then run:

```powershell
mvn clean compile exec:java
```


The console menu provides access to student management, course management, enrollment and attendance, marks entry, semester mark sheets, attendance defaulters, department summary, and exit.

## Database Tables

| Table | Purpose |
|---|---|
| `students` | Student records and identifying information |
| `courses` | Course codes, titles, credits, and course types |
| `enrollments` | Student-course enrollment and attendance totals |
| `assessment_marks` | CIA, Phase 2, ESE marks, and ESE absence status |

## Testing Checklist

1. Register a student using a new roll number.
2. Verify that duplicate roll numbers are rejected.
3. Add a course and enroll a student.
4. Update attendance and check the attendance percentage
5. Enter assessment marks and inspect the mark sheet.
6. Check the attendance defaulter report.
7. Check the department summary.
8. Exit the application normally.

## Notes

- This is a command-line console application; it does not require a web dashboard.
- The application is configured to use the existing `kec_academic` database.
- Verify attendance and grading calculations against the official KEC R2024 rules before final academic submission.

## Links
 - Project document link - [VIKASINI_JAVA (1) (1).pdf](https://github.com/user-attachments/files/34007767/VIKASINI_JAVA.1.1.pdf)
 




## BY

**VIKASINI E**  
**Roll Number: 25ITR172**
