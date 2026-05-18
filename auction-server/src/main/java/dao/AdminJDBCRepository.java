package dao;

import domain.repository.AdminRepository;
import models.*;
import utils.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp AdminJDBCRepository triển khai các nghiệp vụ quản lý của Admin (AdminRepository).
 * Giao tiếp trực tiếp với SQLite sử dụng JDBC PreparedStatement và Statement.
 * Sử dụng mẫu thiết kế Singleton (Singleton Pattern).
 */
public class AdminJDBCRepository implements AdminRepository {
    // Thể hiện duy nhất của lớp (Singleton Instance)
    private static AdminJDBCRepository instance;

    /**
     * Hàm khởi tạo riêng tư để ngăn việc khởi tạo từ bên ngoài.
     */
    private AdminJDBCRepository() {}

    /**
     * Lấy thể hiện duy nhất của lớp AdminJDBCRepository (Thread-safe).
     * @return Đối tượng AdminJDBCRepository duy nhất
     */
    public static synchronized AdminJDBCRepository getInstance() {
        if (instance == null) {
            instance = new AdminJDBCRepository();
        }
        return instance;
    }

    /**
     * Lấy toàn bộ danh sách tất cả người dùng trong hệ thống từ Database.
     * Tự động khởi tạo đối tượng thích hợp (Admin, Seller, Bidder) dựa trên cột role.
     * @return Danh sách các User
     */
    @Override
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String username = rs.getString("username");
                String email = rs.getString("email");
                String role = rs.getString("role");
                boolean isLocked = false;
                boolean isVerified = false;
                try { isLocked = rs.getBoolean("isLocked"); } catch (SQLException ignore) {}
                try { isVerified = rs.getBoolean("isVerified"); } catch (SQLException ignore) {}
                
                // Khởi tạo đối tượng dựa trên vai trò
                User user;
                if ("ADMIN".equalsIgnoreCase(role)) user = new Admin(username, "", email);
                else if ("SELLER".equalsIgnoreCase(role)) user = new Seller(username, "", email);
                else user = new Bidder(username, "", email);
                
                user.setRole(role); // Cần thiết lập trường role cho việc chuyển đổi dạng GSON JSON
                user.setLocked(isLocked);
                user.setVerified(isVerified);
                users.add(user);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return users;
    }

    /**
     * Thay đổi trạng thái khóa/mở khóa của người dùng.
     * @param username Tên tài khoản
     * @param isLocked true để khóa, false để mở khóa
     * @return true nếu thay đổi thành công, ngược lại false
     */
    @Override
    public boolean updateUserStatus(String username, boolean isLocked) {
        String query = "UPDATE users SET isLocked = ? WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setBoolean(1, isLocked);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /**
     * Duyệt xác minh hoặc hủy xác minh cho Người bán (Seller).
     * @param username Tên tài khoản người bán
     * @param isVerified true để duyệt, false để hủy duyệt
     * @return true nếu thành công, ngược lại false
     */
    @Override
    public boolean verifySeller(String username, boolean isVerified) {
        String query = "UPDATE users SET isVerified = ? WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setBoolean(1, isVerified);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /**
     * Xóa một người dùng cụ thể khỏi cơ sở dữ liệu.
     * @param username Tên tài khoản cần xóa
     * @return true nếu xóa thành công, ngược lại false
     */
    @Override
    public boolean deleteUser(String username) {
        String query = "DELETE FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /**
     * Lấy toàn bộ danh sách phiên đấu giá dựa trên danh sách sản phẩm lấy từ ItemDAO.
     * @return Danh sách các phiên đấu giá tương ứng
     */
    @Override
    public List<Auction> getAllAuctions() {
        ItemDAO itemDAO = new ItemDAO();
        List<Item> items = itemDAO.getAllItems();
        List<Auction> auctions = new ArrayList<>();
        
        for (Item item : items) {
            Auction auction = new Auction(item, item.getSeller());
            auction.setAuctionId(item.getId());
            auctions.add(auction);
        }
        return auctions;
    }

    /**
     * Cập nhật trạng thái của phiên đấu giá (Chờ triển khai thêm nếu cần lưu trạng thái vào DB).
     * @return Luôn trả về true
     */
    @Override
    public boolean updateAuctionStatus(String auctionId, String status) {
        return true; 
    }

    /**
     * Hủy bỏ một phiên đấu giá bằng cách xóa sản phẩm khỏi Database thông qua ItemDAO.
     * @param auctionId Mã phiên đấu giá (cũng là mã sản phẩm)
     * @return true nếu hủy thành công, ngược lại false
     */
    @Override
    public boolean cancelAuction(String auctionId) {
        ItemDAO itemDAO = new ItemDAO();
        return itemDAO.deleteItem(auctionId);
    }

    /**
     * Lấy lịch sử giao dịch thầu (Dữ liệu tạm thời hoặc chờ nâng cấp DB lưu lịch sử thầu).
     * @return Danh sách rỗng
     */
    @Override
    public List<BidTransaction> getTransactionHistory() {
        return new ArrayList<>();
    }

    /**
     * Lấy log giám sát hoạt động hệ thống (Giả lập logs mẫu).
     * @return Danh sách các dòng logs hệ thống
     */
    @Override
    public List<String> getSystemLogs() {
        List<String> logs = new ArrayList<>();
        logs.add("2026-04-25 10:00:00 - User admin logged in");
        logs.add("2026-04-25 10:05:00 - Seller 'john' added new item 'Vintage Watch'");
        return logs;
    }
}
