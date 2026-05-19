// 1. Khai báo package: Nằm trong phân hệ models dùng chung.
package models;

/**
 * Lớp Seller đại diện cho Người bán trong hệ thống đấu giá.
 * Kế thừa trực tiếp từ lớp cha User (IS-A) để nhận toàn bộ tài khoản, mật khẩu, email.
 * Người bán có quyền đăng sản phẩm lên sàn đấu giá và quản lý các sản phẩm của mình.
 */
public class Seller extends User {
    
    /**
     * Hàm khởi tạo (Constructor) đối tượng Người bán.
     * 
     * @param username Tên đăng nhập của Người bán
     * @param password Mật khẩu của Người bán
     * @param email Địa chỉ email liên hệ
     */
    public Seller(String username, String password, String email) {
        // 2. super(...): Gọi trực tiếp Constructor 3 tham số của lớp cha User để khởi tạo.
        super(username, password, email);
    }

    /**
     * Ghi đè (Override) phương thức getRole() từ lớp cha User.
     * Đây là kỹ thuật Hardcode Role (Đóng đinh vai trò): Dù người dùng có cố tình set role gì,
     * đối tượng Seller này luôn luôn trả về vai trò "SELLER" cố định trên bộ nhớ.
     * 
     * @return Chuỗi cố định "SELLER"
     */
    @Override
    public String getRole() {
        return "SELLER";
    }
}