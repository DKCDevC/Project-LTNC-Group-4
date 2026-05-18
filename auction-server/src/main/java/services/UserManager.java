package services;

import dao.UserDAO;
import models.User;

/**
 * Lớp UserManager đóng vai trò là tầng Dịch vụ (Service Layer) quản lý các tác vụ người dùng.
 * Phối hợp với lớp truy cập dữ liệu UserDAO để thực hiện đăng nhập và đăng ký.
 * Thiết kế theo mẫu Singleton (Singleton Pattern).
 */
public class UserManager {
    // Thể hiện duy nhất của lớp UserManager
    private static UserManager instance;
    
    // Đối tượng truy cập cơ sở dữ liệu của người dùng
    private UserDAO userDAO;

    /**
     * Hàm khởi tạo riêng tư, khởi tạo đối tượng UserDAO tương ứng.
     */
    private UserManager() {
        userDAO = new UserDAO();
    }

    /**
     * Lấy thể hiện duy nhất của lớp UserManager (Thread-safe).
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
     * @param username Tên tài khoản hoặc Email đăng nhập
     * @param password Mật khẩu tài khoản
     * @return Đối tượng User nếu đúng thông tin đăng nhập, ngược lại null
     */
    public User login(String username, String password) {
        return userDAO.loginUser(username, password);
    }

    /**
     * Phương thức xử lý đăng ký tài khoản người dùng mới.
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @param email Địa chỉ email
     * @param role Vai trò mong muốn (SELLER, BIDDER)
     * @return true nếu đăng ký thành công, ngược lại false
     */
    public boolean registerUser(String username, String password, String email, String role) {
        return userDAO.insertUser(username, password, email, role);
    }
}