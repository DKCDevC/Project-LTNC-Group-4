package models;

/**
 * Lớp Seller đại diện cho Người bán trong hệ thống đấu giá.
 * Kế thừa từ lớp User và có vai trò mặc định cố định là "SELLER".
 * Người bán có quyền đăng sản phẩm lên sàn đấu giá và quản lý các sản phẩm của mình.
 */
public class Seller extends User {
    
    /**
     * Hàm khởi tạo đối tượng Người bán.
     * @param username Tên đăng nhập của Người bán
     * @param password Mật khẩu của Người bán
     * @param email Địa chỉ email liên hệ
     */
    public Seller(String username, String password, String email) {
        super(username, password, email);
    }

    /**
     * Ghi đè phương thức getRole() để chỉ định rõ vai trò là "SELLER".
     * @return Chuỗi cố định "SELLER"
     */
    @Override
    public String getRole() {
        return "SELLER";
    }
}