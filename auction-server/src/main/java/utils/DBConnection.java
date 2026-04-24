package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

public class DBConnection {
    private static final String URL = "jdbc:sqlite:auction_system.db";
    private static Connection connection;

    private DBConnection() {}

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL);
                createTablesAndDefaultUsers(connection);
            }
            return connection;
        } catch (SQLException e) {
            System.out.println("Lỗi kết nối SQLite: " + e.getMessage());
            return null;
        }
    }

    private static void createTablesAndDefaultUsers(Connection conn) {
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL, " +
                "email TEXT NOT NULL, " +
                "role TEXT NOT NULL" +
                ");";

        String createItemsTable = "CREATE TABLE IF NOT EXISTS items (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "description TEXT, " +
                "starting_price REAL NOT NULL, " +
                "start_time TEXT NOT NULL, " +
                "end_time TEXT NOT NULL, " +
                "type TEXT NOT NULL, " +
                "extra_info TEXT, " +
                "seller_name TEXT, " +
                "sale_type TEXT NOT NULL DEFAULT 'AUCTION', " +
                "image_urls TEXT DEFAULT '', " +
                "reserve_price REAL DEFAULT 0, " +
                "min_increment REAL DEFAULT 1000" +
                ");";

        String createOrdersTable = "CREATE TABLE IF NOT EXISTS orders (" +
                "order_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_id TEXT, " +
                "seller_name TEXT, " +
                "bidder_name TEXT, " +
                "final_price REAL, " +
                "order_date DATE DEFAULT (DATE('now'))" +
                ");";

        String createBidsTable = "CREATE TABLE IF NOT EXISTS bids (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_id TEXT, " +
                "bidder_name TEXT, " +
                "amount REAL, " +
                "bid_time TEXT" +
                ");";

        String createAutoBidsTable = "CREATE TABLE IF NOT EXISTS auto_bids (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_id TEXT, " +
                "bidder_name TEXT, " +
                "max_amount REAL, " +
                "increment REAL" +
                ");";

        String createWatchlistTable = "CREATE TABLE IF NOT EXISTS watchlist (" +
                "username TEXT, " +
                "item_id TEXT, " +
                "PRIMARY KEY (username, item_id)" +
                ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createItemsTable);
            stmt.execute(createOrdersTable);
            stmt.execute(createBidsTable);
            stmt.execute(createAutoBidsTable);
            stmt.execute(createWatchlistTable);

            // Migrate existing DB – ignore errors if column already exists
            try { stmt.execute("ALTER TABLE items ADD COLUMN sale_type TEXT NOT NULL DEFAULT 'AUCTION'"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE items ADD COLUMN image_urls TEXT DEFAULT ''"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE items ADD COLUMN reserve_price REAL DEFAULT 0"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE items ADD COLUMN min_increment REAL DEFAULT 1000"); } catch (Exception ignored) {}

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM users");
            if (rs.next() && rs.getInt("total") == 0) {
                String insertDefaults = "INSERT INTO users (username, password, email, role) VALUES " +
                        "('Bidder_01', 'pass123', 'b1@test.com', 'BIDDER'), " +
                        "('Bidder_02', 'pass123', 'b2@test.com', 'BIDDER'), " +
                        "('Seller_01', 'pass123', 's1@test.com', 'SELLER'), " +
                        "('Admin_01', 'admin123', 'admin@test.com', 'ADMIN');";
                stmt.execute(insertDefaults);
                System.out.println(">>> Đã tự động tạo Database SQLite và thêm 4 tài khoản mẫu.");
            }
        } catch (SQLException e) {
            System.out.println("Lỗi khởi tạo database: " + e.getMessage());
        }
    }
}
