// 1. Khai báo package: Nằm trong phân hệ DAO (Data Access Object) quản lý cơ sở dữ liệu.
package dao;

// 2. Import các mô hình phân vai người dùng và tiện ích kết nối
import models.Admin;
import models.Bidder;
import models.Seller;
import models.User;
import utils.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Lớp UserDAO (Data Access Object) chịu trách nhiệm thao tác dữ liệu người dùng (users) trong Database.
 * Triển khai interface UserRepository (Khớp thiết kế giao diện hạ tầng).
 * 
 * Ý nghĩa thiết kế:
 * - Data Persistence decoupling: Đóng kín toàn bộ nghiệp vụ kiểm tra tài khoản, đăng ký xuống SQLite.
 * - Polymorphic User Hydration: Đọc vai trò "role" từ DB để sinh đúng lớp con đa hình (Admin, Seller, Bidder).
 */
public class UserDAO implements UserRepository {

    /**
     * Xác thực thông tin người dùng đăng nhập bằng cả username hoặc email.
     * 
     * Quy trình xử lý (Input -> Process -> Output):
     * 1. Đầu vào (Input): Nhận định danh đăng nhập (username hoặc email) và mật khẩu thô.
     * 2. Xử lý (Process):
     *    - Tạo câu truy vấn SQL tìm kiếm tài khoản khớp định danh: `(username = ? OR email = ?) AND password = ?`.
     *    - Kỹ thuật JDBC PreparedStatement: Tham số hóa 3 tham số giữ chỗ an toàn chống phá hoại SQL Injection.
     *    - Hydration: Nếu có tài khoản khớp, đọc thông tin và chuyển đổi kiểu bool SQLite (SQLite lưu bool dưới dạng integer 0/1).
     *    - Phân vai OOP: Khởi tạo đúng thực thể con cụ thể (Admin, Seller, Bidder) kế thừa từ lớp cha `User`.
     * 3. Đầu ra (Output): Trả về đối tượng `User` đa hình hoàn chỉnh nếu đúng, ngược lại null.
     * 
     * @param identifier Chuỗi đại diện cho Tên đăng nhập hoặc Email
     * @param password Mật khẩu thô nhận được từ phía Client
     * @return Đối tượng User tương ứng với vai trò (Admin, Seller, Bidder) nếu đúng, ngược lại null
     */
    public User loginUser(String identifier, String password) {
        // Hỗ trợ đăng nhập linh hoạt bằng cả tên tài khoản hoặc địa chỉ email
        String query = "SELECT * FROM users WHERE (username = ? OR email = ?) AND password = ?";

        // Lấy kết nối SQLite
        Connection conn = DBConnection.getConnection();
        if (conn == null) return null;

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            // Truyền các giá trị vào câu lệnh SQL được chuẩn bị sẵn phòng ngừa SQL Injection
            stmt.setString(1, identifier);
            stmt.setString(2, identifier);
            stmt.setString(3, password);
            ResultSet rs = stmt.executeQuery();

            // Nếu tìm thấy tài khoản khớp thông tin
            if (rs.next()) {
                String username = rs.getString("username");
                String email = rs.getString("email");
                String role = rs.getString("role");
                
                // Lấy thông tin trạng thái khóa và xác thực:
                // SQLite không có kiểu Boolean thuần túy, thường ánh xạ qua cột Integer (0 đại diện false, 1 đại diện true).
                // JDBC hỗ trợ getBoolean() tự động dịch từ giá trị số nguyên này sang boolean của Java.
                boolean isLocked = false;
                boolean isVerified = false;
                try { isLocked = rs.getBoolean("isLocked"); } catch (SQLException ignore) {}
                try { isVerified = rs.getBoolean("isVerified"); } catch (SQLException ignore) {}

                // Tạo đối tượng cụ thể dựa trên quyền hạn (Polymorphic Role Resolution):
                // Chuyển hóa dòng dữ liệu tĩnh phẳng (Flat relational row) thành đối tượng OOP phân vai chuyên biệt.
                User user;
                if ("ADMIN".equalsIgnoreCase(role)) {
                    user = new Admin(username, password, email);
                } else if ("SELLER".equalsIgnoreCase(role)) {
                    user = new Seller(username, password, email);
                } else {
                    user = new Bidder(username, password, email);
                }
                
                // Cài đặt trạng thái tài khoản
                user.setRole(role);
                user.setLocked(isLocked);
                user.setVerified(isVerified);
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Đảm bảo đóng kết nối thủ công trong khối finally phòng rò rỉ tài nguyên mạng (Connection Leak)
            try { conn.close(); } catch (SQLException ignore) {}
        }
        return null;
    }

    /**
     * Thêm một người dùng mới vào cơ sở dữ liệu (Đăng ký tài khoản).
     * 
     * @param username Tên tài khoản mong muốn
     * @param password Mật khẩu tài khoản
     * @param email Địa chỉ email liên hệ
     * @param role Vai trò người dùng (BIDDER, SELLER)
     * @return true nếu đăng ký thành công (thêm mới dòng), false nếu lỗi hoặc trùng username (khóa chính)
     */
    public boolean insertUser(String username, String password, String email, String role) {
        String query = "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)";
        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            // Gán dữ liệu đăng ký thô
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, email);
            stmt.setString(4, role);

            // Thực thi chèn dòng mới vào Database
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("--- LỖI TẠI DATABASE ---");
            System.out.println(e.getMessage());
            return false;
        } finally {
            // Bảo toàn tài nguyên kết nối an toàn tối đa
            try { conn.close(); } catch (SQLException ignore) {}
        }
    }
}