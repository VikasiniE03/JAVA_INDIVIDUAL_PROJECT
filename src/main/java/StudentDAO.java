import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class StudentDAO {
    public record Student(String rollNumber, String name, String department, int currentSemester) {
    }

    public void addStudent(Student student) throws SQLException {
        if (student.rollNumber().isBlank() || student.name().isBlank() || student.department().isBlank()) {
            throw new IllegalArgumentException("Roll number, name, and department are required.");
        }
        if (student.currentSemester() < 1 || student.currentSemester() > 8) {
            throw new IllegalArgumentException("Semester must be between 1 and 8.");
        }

        String sql = """
                INSERT INTO students (roll_number, student_name, department, current_semester)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, student.rollNumber().trim());
            statement.setString(2, student.name().trim());
            statement.setString(3, student.department().trim().toUpperCase());
            statement.setInt(4, student.currentSemester());
            statement.executeUpdate();
        }
    }

    public Student findStudent(String rollNumber) throws SQLException {
        String sql = """
                SELECT roll_number, student_name, department, current_semester
                FROM students WHERE roll_number = ?
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, rollNumber.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new Student(result.getString("roll_number"), result.getString("student_name"),
                        result.getString("department"), result.getInt("current_semester"));
            }
        }
    }

    public List<Student> listStudents() throws SQLException {
        String sql = """
                SELECT roll_number, student_name, department, current_semester
                FROM students ORDER BY department, roll_number
                """;
        List<Student> students = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                students.add(new Student(result.getString("roll_number"), result.getString("student_name"),
                        result.getString("department"), result.getInt("current_semester")));
            }
        }
        return students;
    }
}
