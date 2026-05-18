package dao;

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
 * Triển khai interface UserRepository.
 */
public class UserDAO implements UserRepository {

    /**
     * Xác thực thông tin người dùng đăng nhập bằng cả username hoặc email.
     * @param identifier Chuỗi đại diện cho Tên đăng nhập hoặc Email
     * @param password Mật khẩu thô nhận được từ phía Client
     * @return Đối tượng User tương ứng với vai trò (Admin, Seller, Bidder) nếu đúng, ngược lại null
     */
    public User loginUser(String identifier, String password) {
        // Hỗ trợ đăng nhập linh hoạt bằng cả username hoặc email
        String query = "SELECT * FROM users WHERE (username = ? OR email = ?) AND password = ?";

        // Lấy kết nối SQLite
        Connection conn = DBConnection.getConnection();
        if (conn == null) return null;

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            // Truyền các giá trị vào câu lệnh SQL được chuẩn bị sẵn
            stmt.setString(1, identifier);
            stmt.setString(2, identifier);
            stmt.setString(3, password);
            ResultSet rs = stmt.executeQuery();

            // Nếu tìm thấy tài khoản khớp thông tin
            if (rs.next()) {
                String username = rs.getString("username");
                String email = rs.getString("email");
                String role = rs.getString("role");
                
                // Lấy thông tin trạng thái khóa và xác thực
                boolean isLocked = false;
                boolean isVerified = false;
                try { isLocked = rs.getBoolean("isLocked"); } catch (SQLException ignore) {}
                try { isVerified = rs.getBoolean("isVerified"); } catch (SQLException ignore) {}

                // Tạo đối tượng cụ thể dựa trên quyền hạn (Role) của người dùng
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
            // Đảm bảo đóng kết nối để tránh rò rỉ tài nguyên (connection leaks)
            try { conn.close(); } catch (SQLException ignore) {}
        }
        return null;
    }

    /**
     * Thêm một người dùng mới vào cơ sở dữ liệu (Đăng ký tài khoản).
     * @param username Tên tài khoản mong muốn
     * @param password Mật khẩu tài khoản
     * @param email Địa chỉ email liên hệ
     * @param role Vai trò người dùng (BIDDER, SELLER)
     * @return true nếu đăng ký thành công (thêm mới dòng), false nếu lỗi hoặc trùng username
     */
    public boolean insertUser(String username, String password, String email, String role) {
        String query = "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)";
        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            // Gán dữ liệu đăng ký
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
            // Đảm bảo đóng kết nối để bảo toàn tài nguyên
            try { conn.close(); } catch (SQLException ignore) {}
        }
    }
}