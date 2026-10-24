import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public final class MainApp {
    private final Scanner scanner = new Scanner(System.in);
    private final StudentDAO studentDAO = new StudentDAO();
    private final AcademicDAO academicDAO = new AcademicDAO();

    public static void main(String[] args) {
        try (var ignored = DBConnection.getConnection()) {
            new MainApp().run();
        } catch (SQLException exception) {
            System.err.println("Database error: " + exception.getMessage());
            System.err.println("Check MySQL, kec_schema.sql, and KEC_DB_URL / KEC_DB_USER / KEC_DB_PASSWORD.");
        }
    }

    private void run() {
        while (true) {
            System.out.println("""

                    === KEC R2024 Student Academic & Continuous Evaluation System ===
                    1. Student Management
                    2. Course Management
                    3. Enrollment / Attendance
                    4. Enter Marks
                    5. Student Semester Mark Sheet
                    6. Attendance Defaulters (<65%)
                    7. Department Summary
                    0. Exit
                    """);
            int choice = readInt("Select: ");
            if (choice == 0) {
                System.out.println("Exiting Academic Management System.");
                return;
            }
            try {
                switch (choice) {
                    case 1 -> studentMenu();
                    case 2 -> courseMenu();
                    case 3 -> enrollmentAttendanceMenu();
                    case 4 -> marksMenu();
                    case 5 -> printMarkSheet();
                    case 6 -> printAttendanceDefaulters();
                    case 7 -> printClassSummary();
                    default -> System.out.println("Choose a menu option from 0 to 7.");
                }
            } catch (SQLException exception) {
                System.err.println("Database operation failed: " + exception.getMessage());
            } catch (IllegalArgumentException exception) {
                System.err.println("Input error: " + exception.getMessage());
            }
        }
    }

    private void studentMenu() throws SQLException {
        System.out.println("\n--- Student Management ---\n1. Register student\n2. List students\n0. Back to main menu");
        switch (readInt("Select: ")) {
            case 1 -> registerStudent();
            case 2 -> listStudents();
            case 0 -> { }
            default -> System.out.println("Invalid student-menu option.");
        }
    }

    private void courseMenu() throws SQLException {
        System.out.println("\n--- Course Management ---\n1. Add course\n2. List courses\n0. Back to main menu");
        switch (readInt("Select: ")) {
            case 1 -> addCourse();
            case 2 -> listCourses();
            case 0 -> { }
            default -> System.out.println("Invalid course-menu option.");
        }
    }

    private void enrollmentAttendanceMenu() throws SQLException {
        System.out.println("\n--- Enrollment / Attendance ---\n1. Enroll student in course\n2. Update attendance totals\n0. Back to main menu");
        switch (readInt("Select: ")) {
            case 1 -> enroll();
            case 2 -> updateAttendance();
            case 0 -> { }
            default -> System.out.println("Invalid enrollment/attendance option.");
        }
    }

    private void marksMenu() throws SQLException {
        System.out.println("\n--- Enter Marks ---\n1. CIA Test 1 / CIA Test 2\n2. Phase 2 / assignment score\n3. ESE mark / mark absent\n0. Back to main menu");
        switch (readInt("Select: ")) {
            case 1 -> enterCiaMarks();
            case 2 -> enterPhase2();
            case 3 -> enterEse();
            case 0 -> { }
            default -> System.out.println("Invalid marks-menu option.");
        }
    }

    private void registerStudent() throws SQLException {
        studentDAO.addStudent(new StudentDAO.Student(
                readText("Roll number: "),
                readText("Name: "),
                readText("Department: "),
                readInt("Current semester (1-8): ")));
        System.out.println("Student registered.");
    }

    private void addCourse() throws SQLException {
        String code = readText("Course code: ");
        String title = readText("Course title: ");
        BigDecimal credits = readDecimal("Credits: ");
        EvaluationEngine.CourseType type = EvaluationEngine.CourseType.from(readText("Type (THEORY/PRACTICAL/EMBEDDED): "));
        academicDAO.addCourse(new AcademicDAO.Course(code, title, credits, type));
        System.out.println("Course added.");
    }

    private void enroll() throws SQLException {
        academicDAO.enroll(readText("Roll number: "), readText("Course code: "), readInt("Semester: "));
        System.out.println("Enrollment created.");
    }

    private void updateAttendance() throws SQLException {
        String roll = readText("Roll number: ");
        String course = readText("Course code: ");
        int semester = readInt("Semester: ");
        int held = readInt("Total classes held: ");
        int attended = readInt("Total classes attended: ");
        academicDAO.updateAttendance(roll, course, semester, held, attended);
        System.out.println("Attendance totals updated.");
    }

    private void enterCiaMarks() throws SQLException {
        String roll = readText("Roll number: ");
        String course = readText("Course code: ");
        int semester = readInt("Semester: ");
        BigDecimal cia1 = readMark("CIA Test 1 (0-100): ");
        academicDAO.recordScore(roll, course, semester, "CIA1", cia1, false);
        BigDecimal cia2 = readMark("CIA Test 2 (0-100): ");
        academicDAO.recordScore(roll, course, semester, "CIA2", cia2, false);
        System.out.println("CIA marks recorded.");
    }

    private void enterPhase2() throws SQLException {
        academicDAO.recordScore(
                readText("Roll number: "),
                readText("Course code: "),
                readInt("Semester: "),
                "PHASE2",
                readMark("Assignments / quizzes / lab evaluation score (0-100): "),
                false);
        System.out.println("Phase 2 score recorded.");
    }

    private void enterEse() throws SQLException {
        String roll = readText("Roll number: ");
        String course = readText("Course code: ");
        int semester = readInt("Semester: ");
        String absentInput = readText("Absent in ESE? (y/n): ");
        if ("y".equalsIgnoreCase(absentInput)) {
            academicDAO.recordScore(roll, course, semester, "ESE", null, true);
            System.out.println("ESE absence recorded.");
        } else if ("n".equalsIgnoreCase(absentInput)) {
            academicDAO.recordScore(roll, course, semester, "ESE", readMark("ESE mark (0-100): "), false);
            System.out.println("ESE mark recorded.");
        } else {
            throw new IllegalArgumentException("Enter y or n.");
        }
    }

    private void printMarkSheet() throws SQLException {
        String roll = readText("Roll number: ");
        int semester = readInt("Semester: ");
        List<AcademicDAO.CourseRow> rows = academicDAO.getSemesterRows(semester, roll);
        if (rows.isEmpty()) {
            System.out.println("No enrollment found for that student and semester.");
            return;
        }
        System.out.printf("%nKEC R2024 STUDENT SEMESTER MARK SHEET%n%s | %s | %s | Semester %d%n",
                rows.get(0).rollNumber(), rows.get(0).studentName(), rows.get(0).department(), semester);
        System.out.printf("%-10s %-18s %6s %6s %6s %8s %8s %7s %8s %8s %-6s %-7s %s%n",
                "Course", "Title", "CIA1", "CIA2", "Phase2", "Internal", "CIA Wt.",
                "ESE Raw", "ESE Wt.", "Total", "Grade", "Status", "Attendance");

        List<EvaluationEngine.SemesterCourse> semesterCourses = new ArrayList<>();
        boolean complete = true;
        for (AcademicDAO.CourseRow row : rows) {
            EvaluationEngine.Result result = row.result();
            semesterCourses.add(new EvaluationEngine.SemesterCourse(row.credits(), result));
            complete &= result.complete();
            System.out.printf("%-10s %-18s %6s %6s %6s %8s %8s %7s %8s %8s %-6s %-7s %s%n",
                    row.courseCode(), truncate(row.courseTitle(), 18),
                    format(row.cia1()), format(row.cia2()), format(row.phase2()),
                    format(result.internalScore()), format(result.internalWeighted()),
                    format(row.eseMark()), format(result.eseWeighted()), format(result.total()),
                    result.grade(), result.status(), format(result.attendancePercent()) + "%");
        }

        BigDecimal sgpa = complete ? EvaluationEngine.semesterGpa(semesterCourses) : null;
        System.out.println("SGPA: " + format(sgpa));
        System.out.println("Equivalent percentage: " + (sgpa == null ? "PENDING"
                : sgpa.multiply(BigDecimal.TEN).setScale(2, RoundingMode.HALF_UP) + "%"));
        System.out.println("CIA Wt. and ESE Wt. are weighted contributions to the total out of 100.");
    }

    private void printAttendanceDefaulters() throws SQLException {
        List<AcademicDAO.CourseRow> rows = academicDAO.getAttendanceDefaulters();
        System.out.println("\nATTENDANCE DEFAULTERS (<65%)");
        if (rows.isEmpty()) {
            System.out.println("No attendance defaulters found.");
            return;
        }
        System.out.printf("%-12s %-22s %-10s %-12s %-8s %-12s %-8s%n",
                "Roll", "Name", "Department", "Course", "Semester", "Attendance", "Grade");
        for (AcademicDAO.CourseRow row : rows) {
            EvaluationEngine.Result result = row.result();
            System.out.printf("%-12s %-22s %-10s %-12s %-8d %-12s %-8s%n",
                    row.rollNumber(), truncate(row.studentName(), 22), row.department(),
                    row.courseCode(), row.semester(), format(result.attendancePercent()) + "%", result.grade());
        }
    }

    private void printClassSummary() throws SQLException {
        int semester = readInt("Semester: ");
        List<AcademicDAO.CourseRow> rows = academicDAO.getSemesterRows(semester, null);
        Map<String, Map<String, List<AcademicDAO.CourseRow>>> departments = new LinkedHashMap<>();
        for (AcademicDAO.CourseRow row : rows) {
            departments.computeIfAbsent(row.department(), ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(row.rollNumber(), ignored -> new ArrayList<>()).add(row);
        }
        System.out.printf("%nDEPARTMENT PERFORMANCE SUMMARY - SEMESTER %d%n", semester);
        System.out.printf("%-30s %8s %8s %10s %12s%n", "Department", "Pass", "Fail", "Pending", "Avg SGPA");
        for (Map.Entry<String, Map<String, List<AcademicDAO.CourseRow>>> department : departments.entrySet()) {
            int pass = 0;
            int fail = 0;
            int pending = 0;
            BigDecimal sgpaSum = BigDecimal.ZERO;
            int gpaCount = 0;
            for (List<AcademicDAO.CourseRow> studentRows : department.getValue().values()) {
                List<EvaluationEngine.SemesterCourse> courses = new ArrayList<>();
                boolean allComplete = true;
                boolean hasFailure = false;
                for (AcademicDAO.CourseRow row : studentRows) {
                    EvaluationEngine.Result result = row.result();
                    courses.add(new EvaluationEngine.SemesterCourse(row.credits(), result));
                    allComplete &= result.complete();
                    hasFailure |= "RA".equals(result.grade()) || "SA".equals(result.grade())
                            || "AB".equals(result.grade());
                }
                if (hasFailure) {
                    fail++;
                } else if (!allComplete) {
                    pending++;
                } else {
                    pass++;
                }
                if (allComplete) {
                    BigDecimal gpa = EvaluationEngine.semesterGpa(courses);
                    if (gpa != null) {
                        sgpaSum = sgpaSum.add(gpa);
                        gpaCount++;
                    }
                }
            }
            BigDecimal average = gpaCount == 0 ? null
                    : sgpaSum.divide(BigDecimal.valueOf(gpaCount), 2, RoundingMode.HALF_UP);
            System.out.printf("%-30s %8d %8d %10d %12s%n",
                    department.getKey(), pass, fail, pending, format(average));
        }
        if (departments.isEmpty()) {
            System.out.println("No enrollments found for that semester.");
        }
        System.out.println("Students with any RA/SA/AB are counted as fail; incomplete results as pending.");
    }

    private void listStudents() throws SQLException {
        System.out.println("\nSTUDENTS");
        List<StudentDAO.Student> students = studentDAO.listStudents();
        if (students.isEmpty()) System.out.println("No students found.");
        for (StudentDAO.Student student : students) {
            System.out.printf("%-12s %-24s %-24s semester %d%n",
                    student.rollNumber(), student.name(), student.department(), student.currentSemester());
        }
    }

    private void listCourses() throws SQLException {
        System.out.println("\nCOURSES");
        List<AcademicDAO.Course> courses = academicDAO.listCourses();
        if (courses.isEmpty()) System.out.println("No courses found.");
        for (AcademicDAO.Course course : courses) {
            System.out.printf("%-12s %-32s %s credits %s%n",
                    course.code(), truncate(course.title(), 32), course.type(), course.credits());
        }
    }

    private int readInt(String prompt) {
        try {
            return Integer.parseInt(readText(prompt));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a whole number.");
        }
    }

    private BigDecimal readDecimal(String prompt) {
        try {
            return new BigDecimal(readText(prompt));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a valid decimal number.");
        }
    }

    private BigDecimal readMark(String prompt) {
        BigDecimal mark = readDecimal(prompt);
        if (mark.compareTo(BigDecimal.ZERO) < 0 || mark.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Marks must be between 0 and 100.");
        }
        return mark;
    }

    private String readText(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static String format(BigDecimal value) {
        return value == null ? "--" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }
}
