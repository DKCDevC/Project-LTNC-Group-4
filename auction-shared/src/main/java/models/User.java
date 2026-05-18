package models;

/**
 * Lớp đại diện cho một Người dùng (User) chung trong hệ thống eBid.
 * Kế thừa từ lớp Entity để sở hữu một mã định danh ngẫu nhiên duy nhất (id).
 * Lớp này chứa các thuộc tính cơ bản như tài khoản, mật khẩu, email, vai trò, trạng thái khóa tài khoản và trạng thái xác thực.
 */
public class User extends Entity {
    // Tên đăng nhập (username) duy nhất của người dùng
    protected String username;
    
    // Mật khẩu (password) đã được mã hóa hoặc dạng thô để xác thực
    protected String password;
    
    // Địa chỉ email liên hệ của người dùng
    protected String email;
    
    // Vai trò của người dùng (ví dụ: SELLER, BIDDER, ADMIN)
    protected String role;
    
    // Trạng thái khóa tài khoản (true nếu bị Admin khóa, không thể đăng nhập)
    protected boolean isLocked = false;
    
    // Trạng thái xác thực tài khoản (dành cho mục đích bảo mật bổ sung)
    protected boolean isVerified = false;

    /**
     * Hàm khởi tạo cơ bản cho một Người dùng.
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @param email Địa chỉ email
     */
    public User(String username, String password, String email) {
        super();
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPassword(){
        return password;
    }

    /**
     * Kiểm tra xem tài khoản có bị khóa không.
     * @return true nếu tài khoản đang bị khóa, ngược lại false
     */
    public boolean isLocked() { return isLocked; }
    
    /**
     * Thiết lập trạng thái khóa/mở khóa tài khoản.
     * @param locked Giá trị true để khóa, false để mở khóa
     */
    public void setLocked(boolean locked) { isLocked = locked; }
    
    /**
     * Kiểm tra xem tài khoản đã được xác thực chưa.
     * @return true nếu đã xác thực, ngược lại false
     */
    public boolean isVerified() { return isVerified; }
    
    /**
     * Thiết lập trạng thái xác thực cho tài khoản.
     * @param verified Giá trị true hoặc false
     */
    public void setVerified(boolean verified) { isVerified = verified; }
}