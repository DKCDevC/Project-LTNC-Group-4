package usecase.admin;

import domain.repository.AdminRepository;
import dao.AdminJDBCRepository;
import models.User;
import java.util.List;

/**
 * Lớp UserManagementUseCase đại diện cho tầng Nghiệp vụ Sử dụng (Use Case Layer trong Clean Architecture).
 * Đóng vai trò là cầu nối nghiệp vụ điều phối các hoạt động của Admin liên quan đến quản lý người dùng và phiên đấu giá.
 * Phụ thuộc vào trừu tượng AdminRepository thay vì các lớp triển khai cụ thể (Dependency Inversion Principle).
 * Áp dụng mẫu thiết kế Singleton (Singleton Pattern).
 */
public class UserManagementUseCase {
    // Thể hiện duy nhất của lớp Use Case
    private static UserManagementUseCase instance;
    
    // Tham chiếu đến interface Repository quản lý dữ liệu Admin
    private final AdminRepository repository;

    /**
     * Hàm khởi tạo riêng tư nhận vào một đối tượng Repository trừu tượng.
     * @param repository Nơi lưu trữ dữ liệu quản trị
     */
    private UserManagementUseCase(AdminRepository repository) {
        this.repository = repository;
    }

    /**
     * Lấy thể hiện duy nhất của UserManagementUseCase (Thread-safe).
     * @return Đối tượng UserManagementUseCase duy nhất
     */
    public static synchronized UserManagementUseCase getInstance() {
        if (instance == null) {
            instance = new UserManagementUseCase(AdminJDBCRepository.getInstance());
        }
        return instance;
    }

    /**
     * Lấy danh sách toàn bộ người dùng trong hệ thống.
     * @return Danh sách các đối tượng User
     */
    public List<User> getUsers() {
        return repository.getAllUsers();
    }

    /**
     * Nghiệp vụ KHÓA tài khoản người dùng.
     * @param username Tên tài khoản cần khóa
     * @return true nếu khóa thành công, ngược lại false
     */
    public boolean lockUser(String username) {
        return repository.updateUserStatus(username, true);
    }

    /**
     * Nghiệp vụ MỞ KHÓA tài khoản người dùng.
     * @param username Tên tài khoản cần mở khóa
     * @return true nếu mở khóa thành công, ngược lại false
     */
    public boolean unlockUser(String username) {
        return repository.updateUserStatus(username, false);
    }

    /**
     * Nghiệp vụ DUYỆT XÁC THỰC tài khoản Người bán (Seller).
     * @param username Tên tài khoản người bán cần duyệt
     * @return true nếu duyệt thành công, ngược lại false
     */
    public boolean verifySeller(String username) {
        return repository.verifySeller(username, true);
    }

    /**
     * Nghiệp vụ XÓA tài khoản người dùng khỏi hệ thống.
     * @param username Tên tài khoản cần xóa
     * @return true nếu xóa thành công, ngược lại false
     */
    public boolean removeUser(String username) {
        return repository.deleteUser(username);
    }

    /**
     * Lấy toàn bộ danh sách phiên đấu giá phục vụ hiển thị quản trị.
     * @return Danh sách các phiên đấu giá
     */
    public List<models.Auction> getAuctions() {
        return repository.getAllAuctions();
    }

    /**
     * Nghiệp vụ HỦY BỎ phiên đấu giá sản phẩm.
     * @param auctionId Mã phiên đấu giá cần hủy
     * @return true nếu hủy thành công, ngược lại false
     */
    public boolean removeAuction(String auctionId) {
        return repository.cancelAuction(auctionId);
    }
}
