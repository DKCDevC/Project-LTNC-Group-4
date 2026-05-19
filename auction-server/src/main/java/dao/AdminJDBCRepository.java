// 1. Khai báo package: Nằm trong phân hệ DAO (Data Access Object) quản lý cơ sở dữ liệu.
package dao;

// 2. Import tầng trừu tượng Domain Repository và các thực thể của Server
import domain.repository.AdminRepository;
import models.*;
import utils.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp AdminJDBCRepository triển khai các nghiệp vụ quản lý của Admin (AdminRepository).
 * Giao tiếp trực tiếp với SQLite sử dụng JDBC PreparedStatement và Statement.
 * 
 * Ý nghĩa thiết kế:
 * - Singleton Design Pattern: Đảm bảo chỉ tồn tại duy nhất một đối tượng điều phối nghiệp vụ quản trị viên trên RAM máy chủ.
 * - Triển khai Domain Interface (Dependency Inversion): Kế thừa hợp đồng `AdminRepository` định hình ở tầng Domain, 
 *   giúp tầng nghiệp vụ Use Case không bị phụ thuộc trực tiếp vào phương thức truy xuất SQLite thô.
 */
public class AdminJDBCRepository implements AdminRepository {
    // Thể hiện duy nhất của lớp (Singleton Instance)
    private static AdminJDBCRepository instance;

    /**
     * Hàm khởi tạo riêng tư để ngăn việc khởi tạo từ bên ngoài tự do.
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
     * Lấy toàn bộ danh sách tất cả người dùng trong hệ thống từ Database SQLite.
     * Tự động khởi tạo đối tượng thích hợp (Admin, Seller, Bidder) dựa trên cột role.
     * 
     * Kỹ thuật JDBC & OOP:
     * - `Statement` vs `PreparedStatement`: Ở đây sử dụng `Statement` thô vì câu SQL là tĩnh (`SELECT * FROM users`), 
     *   hoàn toàn không chứa bất kỳ tham số đầu vào động nào của người dùng. Không có nguy cơ bị SQL Injection.
     * - Try-with-resources: Tự động đóng cả `Connection`, `Statement` và `ResultSet` khi khối `try` kết thúc, 
     *   phòng chống triệt để Connection Leak (Rò rỉ tài nguyên mạng kết nối).
     * - Polymorphic Hydration: Đọc trường `role` và sinh đối tượng con cụ thể (Admin, Seller, Bidder)
     *   được đúc dưới kiểu dữ liệu cha `User`.
     * 
     * @return Danh sách các User đa hình
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
                
                // Khởi tạo đối tượng đa hình dựa trên vai trò thực tế
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
     * 
     * Kỹ thuật an toàn:
     * - Áp dụng `PreparedStatement` để truyền biến an toàn tuyệt đối.
     * 
     * @param username Tên tài khoản người dùng
     * @param isLocked true để khóa, false để mở khóa
     * @return true nếu số dòng được cập nhật trong DB > 0, ngược lại false
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
     * 
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
     * Xóa một người dùng cụ thể khỏi cơ sở dữ liệu SQLite.
     * 
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
     * Thực hiện lắp ráp (Assembly) cấu trúc Item và Seller sang cấu trúc phiên đấu giá `Auction`.
     * 
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
     * Hiện tại được thiết kế như một cổng chờ mở (Open Endpoint Extension).
     * 
     * @return Luôn trả về true
     */
    @Override
    public boolean updateAuctionStatus(String auctionId, String status) {
        return true; 
    }

    /**
     * Hủy bỏ một phiên đấu giá bằng cách xóa sản phẩm khỏi Database thông qua ItemDAO.
     * 
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
     * Hiện tại trả về danh sách rỗng để tương thích hợp đồng giao tiếp.
     * 
     * @return Danh sách rỗng
     */
    @Override
    public List<BidTransaction> getTransactionHistory() {
        return new ArrayList<>();
    }

    /**
     * Lấy log giám sát hoạt động hệ thống (Mô phỏng dữ liệu Logs mẫu phục vụ đồ án).
     * 
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
