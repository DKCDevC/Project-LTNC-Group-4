package dao;

import models.User;

/**
 * Interface UserRepository định nghĩa các hợp đồng (contracts) truy cập dữ liệu người dùng.
 * Giúp tách biệt phần logic nghiệp vụ (Services) và tầng lưu trữ dữ liệu (DAO).
 */
public interface UserRepository {
    
    /**
     * Xác thực thông tin người dùng đăng nhập.
     * @param identifier Tên tài khoản hoặc Email
     * @param password Mật khẩu
     * @return Đối tượng User tương ứng nếu khớp, ngược lại null
     */
    User loginUser(String identifier, String password);
    
    /**
     * Thêm mới một tài khoản người dùng vào kho lưu trữ.
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @param email Địa chỉ email
     * @param role Vai trò người dùng (BIDDER, SELLER...)
     * @return true nếu lưu thành công, ngược lại false
     */
    boolean insertUser(String username, String password, String email, String role);
}
