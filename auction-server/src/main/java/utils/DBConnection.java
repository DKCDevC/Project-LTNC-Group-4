package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * Lớp DBConnection chịu trách nhiệm thiết lập và quản lý kết nối đến cơ sở dữ liệu SQLite.
 * Tự động tạo cấu trúc bảng (users, items, orders) và nạp dữ liệu mẫu ban đầu nếu cơ sở dữ liệu trống.
 */
public class DBConnection {
    // Đường dẫn kết nối đến cơ sở dữ liệu SQLite lưu trong thư mục gốc của dự án
    private static final String URL = "jdbc:sqlite:auction_system.db";

    /**
     * Mở kết nối đến cơ sở dữ liệu SQLite và tự động kiểm tra, khởi tạo bảng.
     * @return Đối tượng Connection đến Database, hoặc null nếu có lỗi kết nối
     */
    public static Connection getConnection() {
        try {
            // Sử dụng trình điều khiển DriverManager để mở kết nối
            Connection conn = DriverManager.getConnection(URL);
            
            // Tự động kiểm tra và khởi tạo cấu trúc bảng, thêm dữ liệu mẫu
            createTablesAndDefaultUsers(conn);
            return conn;
        } catch (SQLException e) {
            System.out.println("Lỗi kết nối SQLite: " + e.getMessage());
            return null;
        }
    }

    /**
     * Phương thức nội bộ thực thi các truy vấn tạo bảng (DDL) và nạp dữ liệu người dùng mặc định.
     * @param conn Kết nối cơ sở dữ liệu đang hoạt động
     */
    private static void createTablesAndDefaultUsers(Connection conn) {
        // SQL tạo bảng lưu trữ người dùng (users)
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL, " +
                "email TEXT NOT NULL, " +
                "role TEXT NOT NULL, " +
                "isLocked INTEGER DEFAULT 0, " +
                "isVerified INTEGER DEFAULT 0" +
                ");";

        // SQL tạo bảng lưu trữ sản phẩm đăng đấu giá (items)
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
                "image_urls TEXT" +
                ");";

        // SQL tạo bảng lưu trữ đơn hàng sau khi đấu giá thành công (orders)
        String createOrdersTable = "CREATE TABLE IF NOT EXISTS orders (" +
                "order_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_id TEXT, " +
                "seller_name TEXT, " +
                "bidder_name TEXT, " +
                "final_price REAL, " +
                "order_date DATE DEFAULT (DATE('now'))" +
                ");";

        try (Statement stmt = conn.createStatement()) {
            // Thực thi các lệnh tạo bảng trong SQLite
            stmt.execute(createUsersTable);
            stmt.execute(createItemsTable); // --- ĐÃ SỬA: Thực thi tạo bảng items (trước đó bị thiếu) ---
            stmt.execute(createOrdersTable);

            // Đảm bảo tương thích: Thử thêm cột image_urls nếu bảng items được tạo từ phiên bản cũ hơn
            try {
                stmt.execute("ALTER TABLE items ADD COLUMN image_urls TEXT;");
            } catch (SQLException ignore) {
                // Bỏ qua lỗi nếu cột image_urls đã tồn tại sẵn
            }

            // Kiểm tra xem database đã có tài khoản nào chưa. Nếu chưa, nạp 4 tài khoản mẫu ban đầu.
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