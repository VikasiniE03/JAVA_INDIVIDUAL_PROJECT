import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class AcademicDAO {
    public record Course(String code, String title, BigDecimal credits, EvaluationEngine.CourseType type) {
    }

    public record CourseRow(
            String rollNumber,
            String studentName,
            String department,
            String courseCode,
            String courseTitle,
            BigDecimal credits,
            EvaluationEngine.CourseType courseType,
            int semester,
            int classesHeld,
            int classesAttended,
            BigDecimal cia1,
            BigDecimal cia2,
            BigDecimal phase2,
            BigDecimal eseMark,
            boolean eseAbsent) {

        public EvaluationEngine.Result result() {
            return EvaluationEngine.evaluate(courseType, classesHeld, classesAttended,
                    cia1, cia2, phase2, eseMark, eseAbsent);
        }
    }

    public void addCourse(Course course) throws SQLException {
        if (course.code().isBlank() || course.title().isBlank() || course.credits().signum() <= 0) {
            throw new IllegalArgumentException("Course code and title are required; credits must be positive.");
        }
        if (course.credits().stripTrailingZeros().scale() > 1) {
            throw new IllegalArgumentException("Credits may have at most one decimal place.");
        }
        String sql = "INSERT INTO courses (course_code, course_title, credits, course_type) VALUES (?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, course.code().trim().toUpperCase());
            statement.setString(2, course.title().trim());
            statement.setBigDecimal(3, course.credits());
            statement.setString(4, course.type().name());
            statement.executeUpdate();
        }
    }

    public List<Course> listCourses() throws SQLException {
        String sql = "SELECT course_code, course_title, credits, course_type FROM courses ORDER BY course_code";
        List<Course> courses = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                courses.add(new Course(result.getString("course_code"), result.getString("course_title"),
                        result.getBigDecimal("credits"),
                        EvaluationEngine.CourseType.from(result.getString("course_type"))));
            }
        }
        return courses;
    }

    public void enroll(String rollNumber, String courseCode, int semester) throws SQLException {
        if (semester < 1 || semester > 8) {
            throw new IllegalArgumentException("Semester must be between 1 and 8.");
        }
        String findIds = """
                SELECT s.student_id, c.course_id
                FROM students s CROSS JOIN courses c
                WHERE s.roll_number = ? AND c.course_code = ?
                """;
        String addEnrollment = """
                INSERT INTO enrollments (student_id, course_id, semester)
                VALUES (?, ?, ?)
                """;
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long studentId;
                long courseId;
                try (PreparedStatement statement = connection.prepareStatement(findIds)) {
                    statement.setString(1, rollNumber.trim());
                    statement.setString(2, courseCode.trim().toUpperCase());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("Student or course was not found.");
                        }
                        studentId = result.getLong("student_id");
                        courseId = result.getLong("course_id");
                    }
                }
                long enrollmentId;
                try (PreparedStatement statement = connection.prepareStatement(addEnrollment, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setLong(1, studentId);
                    statement.setLong(2, courseId);
                    statement.setInt(3, semester);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("The database did not return an enrollment ID.");
                        }
                        enrollmentId = keys.getLong(1);
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO assessment_marks (enrollment_id) VALUES (?)")) {
                    statement.setLong(1, enrollmentId);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void updateAttendance(String rollNumber, String courseCode, int semester, int held, int attended)
            throws SQLException {
        if (held < 0 || attended < 0 || attended > held) {
            throw new IllegalArgumentException("Attendance must satisfy 0 <= attended <= classes held.");
        }
        String sql = """
                UPDATE enrollments e
                JOIN students s ON s.student_id = e.student_id
                JOIN courses c ON c.course_id = e.course_id
                SET e.classes_held = ?, e.classes_attended = ?
                WHERE s.roll_number = ? AND c.course_code = ? AND e.semester = ?
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement exists = connection.prepareStatement("""
                     SELECT 1
                     FROM enrollments e
                     JOIN students s ON s.student_id = e.student_id
                     JOIN courses c ON c.course_id = e.course_id
                     WHERE s.roll_number = ? AND c.course_code = ? AND e.semester = ?
                     """)) {
            exists.setString(1, rollNumber.trim());
            exists.setString(2, courseCode.trim().toUpperCase());
            exists.setInt(3, semester);
            try (ResultSet result = exists.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("Enrollment was not found.");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, held);
                statement.setInt(2, attended);
                statement.setString(3, rollNumber.trim());
                statement.setString(4, courseCode.trim().toUpperCase());
                statement.setInt(5, semester);
                statement.executeUpdate();
            }
        }
    }

    public void recordScore(
            String rollNumber,
            String courseCode,
            int semester,
            String assessment,
            BigDecimal mark,
            boolean absent) throws SQLException {
        String column = switch (assessment) {
            case "CIA1" -> "cia_test_1";
            case "CIA2" -> "cia_test_2";
            case "PHASE2" -> "phase_2_score";
            case "ESE" -> "ese_mark";
            default -> throw new IllegalArgumentException("Unknown assessment.");
        };
        if (!absent && (mark == null || mark.compareTo(BigDecimal.ZERO) < 0
                || mark.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("Marks must be between 0 and 100.");
        }
        if (!absent && mark.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Marks may have at most two decimal places.");
        }
        if (absent && !"ESE".equals(assessment)) {
            throw new IllegalArgumentException("Only the ESE can be recorded as absent.");
        }

        String findEnrollment = """
                SELECT e.enrollment_id, e.classes_held, e.classes_attended
                FROM enrollments e
                JOIN students s ON s.student_id = e.student_id
                JOIN courses c ON c.course_id = e.course_id
                WHERE s.roll_number = ? AND c.course_code = ? AND e.semester = ?
                FOR UPDATE
                """;
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long enrollmentId;
                int held;
                int attended;
                try (PreparedStatement statement = connection.prepareStatement(findEnrollment)) {
                    statement.setString(1, rollNumber.trim());
                    statement.setString(2, courseCode.trim().toUpperCase());
                    statement.setInt(3, semester);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("Enrollment was not found.");
                        }
                        enrollmentId = result.getLong("enrollment_id");
                        held = result.getInt("classes_held");
                        attended = result.getInt("classes_attended");
                    }
                }
                if ("ESE".equals(assessment)
                        && (held == 0 || (long) attended * 100 < (long) held * 75)) {
                    throw new IllegalArgumentException(
                            "ESE marks require at least 75% attendance; condonation approval is not implemented.");
                }

                String update = absent
                        ? "UPDATE assessment_marks SET ese_mark = NULL, ese_absent = TRUE WHERE enrollment_id = ?"
                        : "ESE".equals(assessment)
                                ? "UPDATE assessment_marks SET ese_mark = ?, ese_absent = FALSE WHERE enrollment_id = ?"
                                : "UPDATE assessment_marks SET " + column + " = ? WHERE enrollment_id = ?";
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT 1 FROM assessment_marks WHERE enrollment_id = ? FOR UPDATE")) {
                    statement.setLong(1, enrollmentId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Assessment record was not found for the enrollment.");
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(update)) {
                    if (absent) {
                        statement.setLong(1, enrollmentId);
                    } else {
                        statement.setBigDecimal(1, mark);
                        statement.setLong(2, enrollmentId);
                    }
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<CourseRow> getSemesterRows(int semester, String rollNumber) throws SQLException {
        String sql = """
                SELECT s.roll_number, s.student_name, s.department,
                       c.course_code, c.course_title, c.credits, c.course_type,
                       e.semester, e.classes_held, e.classes_attended,
                       a.cia_test_1, a.cia_test_2, a.phase_2_score, a.ese_mark, a.ese_absent
                FROM enrollments e
                JOIN students s ON s.student_id = e.student_id
                JOIN courses c ON c.course_id = e.course_id
                JOIN assessment_marks a ON a.enrollment_id = e.enrollment_id
                WHERE e.semester = ? AND (? IS NULL OR s.roll_number = ?)
                ORDER BY s.department, s.roll_number, c.course_code
                """;
        List<CourseRow> rows = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, semester);
            statement.setString(2, rollNumber);
            statement.setString(3, rollNumber);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(new CourseRow(
                            result.getString("roll_number"),
                            result.getString("student_name"),
                            result.getString("department"),
                            result.getString("course_code"),
                            result.getString("course_title"),
                            result.getBigDecimal("credits"),
                            EvaluationEngine.CourseType.from(result.getString("course_type")),
                            result.getInt("semester"),
                            result.getInt("classes_held"),
                            result.getInt("classes_attended"),
                            result.getBigDecimal("cia_test_1"),
                            result.getBigDecimal("cia_test_2"),
                            result.getBigDecimal("phase_2_score"),
                            result.getBigDecimal("ese_mark"),
                            result.getBoolean("ese_absent")));
                }
            }
        }
        return rows;
    }

    public List<CourseRow> getAttendanceDefaulters() throws SQLException {
        String sql = """
                SELECT s.roll_number, s.student_name, s.department,
                       c.course_code, c.course_title, c.credits, c.course_type,
                       e.semester, e.classes_held, e.classes_attended,
                       a.cia_test_1, a.cia_test_2, a.phase_2_score, a.ese_mark, a.ese_absent
                FROM enrollments e
                JOIN students s ON s.student_id = e.student_id
                JOIN courses c ON c.course_id = e.course_id
                JOIN assessment_marks a ON a.enrollment_id = e.enrollment_id
                WHERE e.classes_held > 0 AND e.classes_attended * 100 < e.classes_held * 65
                ORDER BY s.department, s.roll_number, e.semester, c.course_code
                """;
        List<CourseRow> rows = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                rows.add(new CourseRow(
                        result.getString("roll_number"),
                        result.getString("student_name"),
                        result.getString("department"),
                        result.getString("course_code"),
                        result.getString("course_title"),
                        result.getBigDecimal("credits"),
                        EvaluationEngine.CourseType.from(result.getString("course_type")),
                        result.getInt("semester"),
                        result.getInt("classes_held"),
                        result.getInt("classes_attended"),
                        result.getBigDecimal("cia_test_1"),
                        result.getBigDecimal("cia_test_2"),
                        result.getBigDecimal("phase_2_score"),
                        result.getBigDecimal("ese_mark"),
                        result.getBoolean("ese_absent")));
            }
        }
        return rows;
    }
}
