// 1. Khai báo package: Thuộc phân hệ tiện ích database của Server.
package database;

// 2. Import các thư viện Java Database Connectivity (JDBC):
import java.sql.Connection;        // Đối tượng đại diện cho một phiên kết nối vật lý với Database.
import java.sql.DriverManager;     // Bộ quản lý trình điều khiển, chịu trách nhiệm kết nối các URL Database với driver tương ứng.
import java.sql.SQLException;      // Lớp ngoại lệ xử lý tất cả các lỗi xảy ra trong quá trình truy vấn/kết nối SQL.
import java.sql.Statement;         // Đối tượng dùng để gửi các câu lệnh SQL tĩnh lên Database thực thi.
import java.sql.ResultSet;         // Bảng dữ liệu ảo lưu trữ kết quả trả về từ câu lệnh truy vấn SELECT.

/**
 * Lớp DBConnection chịu trách nhiệm thiết lập và quản lý kết nối đến cơ sở dữ liệu SQLite.
 * Thiết kế theo cơ chế Khởi tạo Tĩnh (Static utility class): Cung cấp các phương thức dùng chung cho toàn bộ Server
 * mà không cần phải khởi tạo đối tượng `new DBConnection()`.
 * Tự động tạo cấu trúc bảng (users, items, orders) và nạp dữ liệu mẫu ban đầu nếu cơ sở dữ liệu trống.
 */
public class DBConnection {
    // 3. Khai báo URL kết nối SQLite:
    // "jdbc:sqlite:" là tiền tố giao thức JDBC của SQLite.
    // "auction_system.db" là đường dẫn tương đối trỏ tới file DB SQLite trong thư mục gốc dự án.
    // SQLite là cơ sở dữ liệu dạng file (File-based DB), không chạy dưới dạng dịch vụ ngầm như MySQL hay SQL Server.
    private static final String URL = "jdbc:sqlite:auction_system.db";

    /**
     * Mở kết nối đến cơ sở dữ liệu SQLite và tự động kiểm tra, khởi tạo bảng.
     * 
     * @return Đối tượng Connection đến Database, hoặc null nếu có lỗi kết nối
     */
    public static Connection getConnection() {
        try {
            // 4. DriverManager.getConnection(URL):
            // Nạp trình điều khiển Driver SQLite ngầm định (qua classpath Maven) và trả về đối tượng kết nối Connection.
            Connection conn = DriverManager.getConnection(URL);
            
            // 5. Khởi tạo cấu trúc bảng: 
            // Mỗi lần lấy kết nối, ta tự động chạy hàm tạo bảng để đảm bảo cấu trúc dữ liệu luôn sẵn sàng.
            createTablesAndDefaultUsers(conn);
            return conn;
        } catch (SQLException e) {
            // Xử lý và ghi nhật ký lỗi nếu không tìm thấy file DB hoặc lỗi phân quyền ổ đĩa
            System.out.println("Lỗi kết nối SQLite: " + e.getMessage());
            return null;
        }
    }

    /**
     * Phương thức nội bộ thực thi các truy vấn tạo bảng (DDL - Data Definition Language) và nạp dữ liệu mẫu.
     * 
     * @param conn Kết nối cơ sở dữ liệu đang hoạt động
     */
    private static void createTablesAndDefaultUsers(Connection conn) {
        // 6. SQL tạo bảng users: Chứa thông tin đăng nhập, vai trò, trạng thái khóa.
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " + // id tự tăng
                "username TEXT UNIQUE NOT NULL, " +       // username duy nhất
                "password TEXT NOT NULL, " +
                "email TEXT NOT NULL, " +
                "role TEXT NOT NULL, " +
                "isLocked INTEGER DEFAULT 0, " +          // SQLite không có kiểu Boolean thực thụ, dùng 0 (false) và 1 (true)
                "isVerified INTEGER DEFAULT 0" +
                ");";

        // 7. SQL tạo bảng items: Lưu trữ thông tin sản phẩm đấu giá, ngày giờ, thông tin bổ sung dạng text.
        String createItemsTable = "CREATE TABLE IF NOT EXISTS items (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "description TEXT, " +
                "starting_price REAL NOT NULL, " +        // REAL tương ứng với kiểu double/float trong Java
                "start_time TEXT NOT NULL, " +            // SQLite lưu DateTime dạng TEXT (ISO 8601)
                "end_time TEXT NOT NULL, " +
                "type TEXT NOT NULL, " +
                "extra_info TEXT, " +
                "seller_name TEXT, " +
                "image_urls TEXT" +
                ");";

        // 8. SQL tạo bảng orders: Lưu trữ đơn hàng thắng cuộc sau khi đấu giá kết thúc thành công.
        String createOrdersTable = "CREATE TABLE IF NOT EXISTS orders (" +
                "order_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_id TEXT, " +
                "seller_name TEXT, " +
                "bidder_name TEXT, " +
                "final_price REAL, " +
                "order_date DATE DEFAULT (DATE('now'))," +
                "status TEXT DEFAULT 'PENDING'" +
                ");";

        // 9. Cú pháp Try-With-Resources:
        // Khởi tạo đối tượng Statement bên trong ngoặc try ().
        // Java sẽ tự động đóng (close) đối tượng Statement này khi khối try kết thúc (dù chạy thành công hay có lỗi xảy ra).
        // Tránh tình trạng rò rỉ tài nguyên bộ nhớ mạng/Cơ sở dữ liệu (Resource Leak).
        try (Statement stmt = conn.createStatement()) {
            // 10. Thực thi các lệnh DDL để tạo bảng nếu chúng chưa tồn tại trong file DB
            stmt.execute(createUsersTable);
            stmt.execute(createItemsTable); 
            stmt.execute(createOrdersTable);

            // 11. Đảm bảo tính tương thích ngược (Backward Compatibility):
            // Thử chạy lệnh ALTER TABLE để thêm cột image_urls nếu bảng items được tạo từ các phiên bản cũ chưa có cột này.
            try {
                stmt.execute("ALTER TABLE items ADD COLUMN image_urls TEXT;");
            } catch (SQLException ignored) {
                // Bỏ qua lỗi nếu cột đã tồn tại sẵn, tránh crash chương trình không cần thiết.
            }

            try {
                stmt.execute("ALTER TABLE orders ADD COLUMN status TEXT DEFAULT 'PENDING';");
            } catch (SQLException ignored) {
                // Bỏ qua lỗi nếu cột đã tồn tại sẵn
            }

            // 12. Kiểm tra dữ liệu mồi (Data Seeding):
            // Đếm tổng số tài khoản trong bảng users. Nếu = 0 (DB hoàn toàn trống), nạp 4 tài khoản mẫu để hệ thống sẵn sàng hoạt động.
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
