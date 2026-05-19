// 1. Khai báo package: Nằm trong phân hệ dịch vụ nghiệp vụ (Services) của Server.
package services;

// 2. Import tầng truy cập cơ sở dữ liệu UserDAO và mô hình User
import dao.UserDAO;
import models.User;

/**
 * Lớp UserManager đóng vai trò là tầng Dịch vụ (Service Layer) quản lý các tác vụ người dùng.
 * Phối hợp với lớp truy cập dữ liệu UserDAO để thực hiện đăng nhập và đăng ký.
 * 
 * Ý nghĩa thiết kế:
 * - Singleton Design Pattern: Đảm bảo chỉ tồn tại duy nhất một đối tượng điều khiển luồng đăng nhập/đăng ký trên RAM.
 * - Service Layer: Đóng vai trò trung gian che chắn UserDAO, nơi sau này có thể tích hợp thêm các giải thuật băm mật khẩu 
 *   (ví dụ: BCrypt) hoặc kiểm duyệt định dạng email Regex trước khi đẩy xuống cơ sở dữ liệu SQLite.
 */
public class UserManager {
    // 3. Thể hiện duy nhất của lớp UserManager
    private static UserManager instance;
    
    // Đối tượng truy cập cơ sở dữ liệu của người dùng (SQLite)
    private UserDAO userDAO;

    /**
     * Hàm khởi tạo riêng tư, khởi tạo đối tượng UserDAO tương ứng.
     */
    private UserManager() {
        userDAO = new UserDAO();
    }

    /**
     * Lấy thể hiện duy nhất của lớp UserManager (Thread-safe).
     * Sử dụng synchronized để đảm bảo an toàn đa luồng lúc khởi dựng.
     * 
     * @return Đối tượng UserManager duy nhất
     */
    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    /**
     * Phương thức xử lý đăng nhập người dùng.
     * Ánh xạ tham số xuống UserDAO để kiểm tra khớp định danh và mật khẩu thô trong SQLite.
     * 
     * @param username Tên tài khoản hoặc Email đăng nhập
     * @param password Mật khẩu tài khoản
     * @return Đối tượng User chứa đầy đủ thông tin phân vai nếu khớp thông tin đăng nhập, ngược lại trả về null
     */
    public User login(String username, String password) {
        return userDAO.loginUser(username, password);
    }

    /**
     * Phương thức xử lý đăng ký tài khoản người dùng mới.
     * Thực hiện kiểm duyệt trung chuyển và gọi xuống tầng DAO để thực thi SQL INSERT thêm dòng mới.
     * 
     * @param username Tên đăng nhập
     * @param password Mật khẩu thô
     * @param email Địa chỉ email
     * @param role Vai trò mong muốn (SELLER, BIDDER)
     * @return true nếu ghi nhận dòng mới thành công trong SQLite, ngược lại false (ví dụ trùng khóa chính username)
     */
    public boolean registerUser(String username, String password, String email, String role) {
        return userDAO.insertUser(username, password, email, role);
    }
}