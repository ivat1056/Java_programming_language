// --- ДОБАВЛЕНО: Класс для работы с базой данных SQLite ---
import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:atm.db";
    private Connection connection;

    // --- ДОБАВЛЕНО: Инициализация БД при создании объекта ---
    public DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            createTables();
            System.out.println("База данных инициализирована.");
        } catch (Exception e) {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    // --- ДОБАВЛЕНО: Создание таблиц если их нет ---
    private void createTables() throws SQLException {
        String users = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY," +
                "card TEXT UNIQUE," +
                "pin TEXT," +
                "name TEXT," +
                "role TEXT," +
                "balance INTEGER)";
        
        String operations = "CREATE TABLE IF NOT EXISTS operations (" +
                "id INTEGER PRIMARY KEY," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "user TEXT," +
                "operation TEXT," +
                "details TEXT," +
                "result TEXT)";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(users);
            stmt.execute(operations);
        }
    }

    // --- ДОБАВЛЕНО: Авторизация пользователя из БД ---
    public User authenticate(String card, String pin) throws InvalidCredentialsException {
        String query = "SELECT name, role, balance FROM users WHERE card = ? AND pin = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, card);
            pstmt.setString(2, pin);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new User(rs.getString("name"), rs.getString("role"), rs.getInt("balance"));
            } else {
                throw new InvalidCredentialsException("Неверные учетные данные");
            }
        } catch (SQLException e) {
            throw new InvalidCredentialsException("Ошибка БД: " + e.getMessage());
        }
    }

    // --- ДОБАВЛЕНО: Обновить баланс пользователя в БД ---
    public void updateBalance(String name, int balance) {
        String query = "UPDATE users SET balance = ? WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, balance);
            pstmt.setString(2, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Ошибка обновления баланса: " + e.getMessage());
        }
    }

    // --- ДОБАВЛЕНО: Записать операцию в БД ---
    public void logOperation(String user, String operation, String details, String result) {
        String query = "INSERT INTO operations (user, operation, details, result) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, user);
            pstmt.setString(2, operation);
            pstmt.setString(3, details);
            pstmt.setString(4, result);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Ошибка логирования: " + e.getMessage());
        }
    }

    // --- ДОБАВЛЕНО: Получить все операции из БД ---
    public void printOperations() {
        String query = "SELECT * FROM operations ORDER BY timestamp DESC LIMIT 100";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            System.out.println("\n--- История операций из БД ---");
            while (rs.next()) {
                System.out.println(rs.getString("timestamp") + " | " + 
                                 rs.getString("user") + " | " + 
                                 rs.getString("operation") + " | " + 
                                 rs.getString("result"));
            }
        } catch (SQLException e) {
            System.out.println("Ошибка чтения операций: " + e.getMessage());
        }
    }

    // --- ДОБАВЛЕНО: Закрытие соединения ---
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.out.println("Ошибка закрытия БД: " + e.getMessage());
        }
    }
}