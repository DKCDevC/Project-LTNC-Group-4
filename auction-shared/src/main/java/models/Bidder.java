package models;

/**
 * Lớp Bidder đại diện cho Người đấu giá/Người mua trong hệ thống đấu giá.
 * Kế thừa từ lớp User và có vai trò mặc định cố định là "BIDDER".
 * Người đấu giá có thể theo dõi, đặt thầu thủ công hoặc cài đặt bot tự động nâng giá (Auto-bid).
 */
public class Bidder extends User {
    
    /**
     * Hàm khởi tạo đối tượng Người đấu giá.
     * @param username Tên đăng nhập của Người đấu giá
     * @param password Mật khẩu
     * @param email Địa chỉ email liên hệ
     */
    public Bidder(String username, String password, String email) {
        super(username, password, email);
    }

    /**
     * Ghi đè phương thức getRole() để xác định quyền người dùng là "BIDDER".
     * @return Chuỗi cố định "BIDDER"
     */
    @Override
    public String getRole() {
        return "BIDDER";
    }
}