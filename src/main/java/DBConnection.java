import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/kec_academic";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = setting("KEC_DB_URL", DEFAULT_URL);
        String user = System.getenv("KEC_DB_USER");
        String password = System.getenv("KEC_DB_PASSWORD");

        if (user == null || user.isBlank()) {
            throw new SQLException("Set the KEC_DB_USER environment variable before starting the application.");
        }
        if (password == null) {
            throw new SQLException("Set the KEC_DB_PASSWORD environment variable before starting the application.");
        }

        return DriverManager.getConnection(url, user, password);
    }

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
