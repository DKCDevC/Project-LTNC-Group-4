package models;

/**
 * Lớp Admin đại diện cho Quản trị viên của hệ thống đấu giá.
 * Kế thừa từ lớp User và có vai trò mặc định cố định là "ADMIN".
 */
public class Admin extends User {
    
    /**
     * Hàm khởi tạo đối tượng Quản trị viên.
     * @param username Tên đăng nhập Admin
     * @param password Mật khẩu Admin
     * @param email Địa chỉ email của Admin
     */
    public Admin(String username, String password, String email) {
        super(username, password, email);
    }

    /**
     * Ghi đè phương thức getRole() để luôn trả về quyền quản trị cao nhất là "ADMIN".
     * @return Chuỗi cố định "ADMIN"
     */
    @Override
    public String getRole() {
        return "ADMIN";
    }
}