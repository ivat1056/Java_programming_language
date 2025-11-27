import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager 
{
    private static final String DB_URL = "jdbc:sqlite:atm.db";
    private Connection connection;
    //
    public DatabaseManager() 
    {
        try 
        {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            createTables();
            System.out.println("База данных инициализирована.");
        } 
        catch (Exception e) 
        {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    // 
    private void createTables() throws SQLException 
    {
        String users = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "card TEXT UNIQUE," +
                "pin TEXT," +
                "name TEXT," +
                "role TEXT," +
                "balance INTEGER DEFAULT 0)";

        String operations = "CREATE TABLE IF NOT EXISTS operations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "user TEXT," +
                "operation TEXT," +
                "details TEXT," +
                "result TEXT)";

        String cashHolders = "CREATE TABLE IF NOT EXISTS cash_holders (" +
                "denomination INTEGER PRIMARY KEY," +
                "count INTEGER NOT NULL)";

        String cashLog = "CREATE TABLE IF NOT EXISTS cash_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "operation TEXT NOT NULL," +
                "denomination INTEGER NOT NULL," +
                "count INTEGER NOT NULL)";

        try (Statement stmt = connection.createStatement()) 
        {
            stmt.execute(users);
            stmt.execute(operations);
            stmt.execute(cashHolders);
            stmt.execute(cashLog);
        }
    }

    // 
    public User authenticate(String card, String pin) throws InvalidCredentialsException 
    {
        String query = "SELECT name, role, balance FROM users WHERE card = ? AND pin = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) 
        {
            pstmt.setString(1, card);
            pstmt.setString(2, pin);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) 
            {
                return new User(rs.getString("name"), rs.getString("role"), rs.getInt("balance"));
            } 
            else 
            {
                throw new InvalidCredentialsException("Неверные учетные данные");
            }
        } 
        catch (SQLException e) 
        {
            throw new InvalidCredentialsException("Ошибка БД: " + e.getMessage());
        }
    }

    // 
    public void updateBalance(String name, int balance) {
        String query = "UPDATE users SET balance = ? WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) 
        {
            pstmt.setInt(1, balance);
            pstmt.setString(2, name);
            pstmt.executeUpdate();
        } 
        catch (SQLException e) 
        {
            System.out.println("Ошибка обновления баланса: " + e.getMessage());
        }
    }

    // 
    public void logOperation(String user, String operation, String details, String result) 
    {
        String query = "INSERT INTO operations (user, operation, details, result) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) 
        {
            pstmt.setString(1, user);
            pstmt.setString(2, operation);
            pstmt.setString(3, details);
            pstmt.setString(4, result);
            pstmt.executeUpdate();
        } 
        catch (SQLException e) 
        {
            System.out.println("Ошибка логирования: " + e.getMessage());
        }
    }

    // 
    public void printOperations() 
    {
        String query = "SELECT * FROM operations ORDER BY timestamp DESC LIMIT 100";
        try (Statement stmt = connection.createStatement()) 
        {
            ResultSet rs = stmt.executeQuery(query);
            System.out.println("\n--- История операций из БД ---");
            while (rs.next()) 
            {
                System.out.println(rs.getString("timestamp") + " | " +
                        rs.getString("user") + " | " +
                        rs.getString("operation") + " | " +
                        rs.getString("result"));
            }
        } 
        catch (SQLException e) 
        {
            System.out.println("Ошибка чтения операций: " + e.getMessage());
        }
    }

    // 
    public CashHolder[] getAllCashHolders() 
    {
        String query = "SELECT denomination, count FROM cash_holders ORDER BY denomination DESC";
        List<CashHolder> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement()) 
        {
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) 
            {
                list.add(new CashHolder(rs.getInt("denomination"), rs.getInt("count")));
            }
        } catch (SQLException e) 
        {
            System.out.println("Ошибка чтения номиналов: " + e.getMessage());
        }
        return list.toArray(new CashHolder[0]);
    }

    // 
    public void updateCashHolder(int denomination, int count) 
    {
        String update = "UPDATE cash_holders SET count = ? WHERE denomination = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(update)) 
        {
            pstmt.setInt(1, count);
            pstmt.setInt(2, denomination);
            int updated = pstmt.executeUpdate();
            if (updated == 0) 
            {
                String insert = "INSERT INTO cash_holders (denomination, count) VALUES (?, ?)";
                try (PreparedStatement ins = connection.prepareStatement(insert)) 
                {
                    ins.setInt(1, denomination);
                    ins.setInt(2, count);
                    ins.executeUpdate();
                }
            }
        } 
        catch (SQLException e) 
        {
            System.out.println("Ошибка обновления cash_holders: " + e.getMessage());
        }
    }

    // 
    public void logCashChange(String operation, int denomination, int count) 
    {
        String q = "INSERT INTO cash_log (operation, denomination, count) VALUES (?, ?, ?)";
        try (PreparedStatement p = connection.prepareStatement(q)) 
        {
            p.setString(1, operation);
            p.setInt(2, denomination);
            p.setInt(3, count);
            p.executeUpdate();
        } 
        catch (SQLException e) 
        {
            System.out.println("Ошибка записи cash_log: " + e.getMessage());
        }
    }

    // 
    public void close() 
    {
        try 
        {
            if (connection != null && !connection.isClosed()) connection.close();
        } 
        catch (SQLException e) 
        {
            System.out.println("Ошибка закрытия БД: " + e.getMessage());
        }
    }
}