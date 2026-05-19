// 1. Khai báo package: Nằm trong phân hệ kiến trúc tên miền Domain Repository của Server.
package domain.repository;

// 2. Import các mô hình nghiệp vụ được sử dụng trong quyền hạn quản trị viên
import models.User;
import models.Auction;
import models.BidTransaction;
import java.util.List;

/**
 * Interface AdminRepository định nghĩa các quyền hạn quản trị (Administrator Capabilities).
 * Bao gồm quản lý người dùng, quản lý phiên đấu giá, theo dõi lịch sử đặt giá và giám sát hệ thống.
 * 
 * Ý nghĩa thiết kế Clean Architecture:
 * - Domain Layer Abstraction: Đây là phần lõi trung tâm của hệ thống (Domain Core). Nó định hình các khả năng
 *   hành vi nghiệp vụ của người quản trị mà không quan tâm đến hạ tầng lưu trữ cụ thể.
 * - Tách biệt lo ngại (Separation of Concerns): Giúp tách rời tầng Use Cases quản lý người dùng khỏi các 
 *   lệnh SQL chèn bảng cụ thể, đáp ứng hoàn hảo nguyên lý Dependency Inversion Principle (DIP) của SOLID.
 */
public interface AdminRepository {
    
    // ==========================================
    // QUẢN LÝ NGƯỜI DÙNG (User Management)
    // ==========================================
    
    /**
     * Lấy toàn bộ danh sách người dùng trong hệ thống (gồm cả Admin, Seller, Bidder).
     * @return Danh sách các đối tượng User đa hình
     */
    List<User> getAllUsers();
    
    /**
     * Khóa hoặc mở khóa một tài khoản người dùng cụ thể.
     * @param username Tên tài khoản cần thay đổi trạng thái
     * @param isLocked true để khóa tài khoản, false để mở khóa
     * @return true nếu cập nhật thành công, ngược lại false
     */
    boolean updateUserStatus(String username, boolean isLocked);
    
    /**
     * Xác thực hoặc hủy xác thực tài khoản Người bán (Seller).
     * @param username Tên tài khoản người bán cần duyệt
     * @param isVerified true để xác nhận người bán hợp lệ, false để thu hồi xác nhận
     * @return true nếu duyệt thành công, ngược lại false
     */
    boolean verifySeller(String username, boolean isVerified);
    
    /**
     * Xóa hoàn toàn một tài khoản người dùng ra khỏi cơ sở dữ liệu.
     * @param username Tên tài khoản cần xóa
     * @return true nếu xóa thành công, ngược lại false
     */
    boolean deleteUser(String username);

    // ==========================================
    // QUẢN LÝ PHIÊN ĐẤU GIÁ (Auction Management)
    // ==========================================
    
    /**
     * Lấy toàn bộ danh sách tất cả các phiên đấu giá (đang chạy, đã xong, đã hủy).
     * @return Danh sách các phiên đấu giá
     */
    List<Auction> getAllAuctions();
    
    /**
     * Cập nhật trạng thái của một phiên đấu giá.
     * @param auctionId Mã phiên đấu giá
     * @param status Trạng thái mới dạng chuỗi (ví dụ: "RUNNING", "FINISHED", "CANCELED")
     * @return true nếu cập nhật thành công, ngược lại false
     */
    boolean updateAuctionStatus(String auctionId, String status);
    
    /**
     * Hủy bỏ một phiên đấu giá đang diễn ra.
     * @param auctionId Mã phiên đấu giá cần hủy
     * @return true nếu hủy thành công, ngược lại false
     */
    boolean cancelAuction(String auctionId);

    // ==========================================
    // QUẢN LÝ GIAO DỊCH (Transaction Management)
    // ==========================================
    
    /**
     * Lấy toàn bộ lịch sử các giao dịch đặt giá thầu trong hệ thống.
     * @return Danh sách các giao dịch đấu giá BidTransaction
     */
    List<BidTransaction> getTransactionHistory();
    
    // ==========================================
    // GIÁM SÁT HỆ THỐNG (System Monitoring)
    // ==========================================
    
    /**
     * Lấy các bản ghi nhật ký hoạt động hệ thống.
     * @return Danh sách các chuỗi log ghi nhận hoạt động máy chủ
     */
    List<String> getSystemLogs();
}
