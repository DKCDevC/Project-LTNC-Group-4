// 1. Khai báo package: Nằm trong phân hệ models dùng chung.
package models;

/**
 * Lớp Bidder đại diện cho Người đấu giá/Người mua trong hệ thống đấu giá.
 * Kế thừa trực tiếp từ lớp cha User (IS-A).
 * Người đấu giá có thể theo dõi, đặt thầu thủ công hoặc cài đặt bot tự động nâng giá (Auto-bid).
 */
public class Bidder extends User {
    
    /**
     * Hàm khởi tạo (Constructor) đối tượng Người đấu giá.
     * 
     * @param username Tên đăng nhập của Người đấu giá
     * @param password Mật khẩu
     * @param email Địa chỉ email liên hệ
     */
    public Bidder(String username, String password, String email) {
        // 2. super(...): Gọi trực tiếp Constructor 3 tham số của lớp cha User để khởi tạo.
        super(username, password, email);
    }

    /**
     * Ghi đè (Override) phương thức getRole() từ lớp cha User.
     * Đây là kỹ thuật Hardcode Role (Đóng đinh vai trò): Dù người dùng có cố tình set role gì,
     * đối tượng Bidder này luôn luôn trả về vai trò "BIDDER" cố định trên bộ nhớ.
     * 
     * @return Chuỗi cố định "BIDDER"
     */
    @Override
    public String getRole() {
        return "BIDDER";
    }
}