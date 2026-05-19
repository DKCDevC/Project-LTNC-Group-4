// 1. Khai báo package: Nằm trong phân hệ models dùng chung (shared) của toàn bộ hệ thống eBid.
package models;

/**
 * Lớp đại diện cho một Người dùng (User) chung trong hệ thống eBid.
 * Kế thừa từ lớp cha Entity (IS-A) để tự động thừa hưởng thuộc tính mã định danh duy nhất (id).
 * Thiết lập theo mô hình Hướng đối tượng (OOP) làm lớp cha cơ sở cho các loại người dùng phân quyền cụ thể
 * (như Admin, Seller, Bidder).
 */
public class User extends Entity {
    
    // 2.protected String username / password / email / role:
    // Sử dụng phạm vi truy cập protected thay vì private để cho phép các lớp con trực tiếp kế thừa
    // (như Seller, Bidder, Admin) truy cập nhanh và gán giá trị mà không cần thông qua hàm Getter/Setter,
    // giúp tối ưu hóa hiệu năng viết code trong khi vẫn giấu kín với thế giới bên ngoài package.
    protected String username;          // Tên đăng nhập (username) duy nhất của người dùng
    protected String password;          // Mật khẩu (password) dạng thô hoặc mã hóa để xác thực
    protected String email;             // Địa chỉ email liên hệ của người dùng
    protected String role;              // Vai trò của người dùng (ví dụ: SELLER, BIDDER, ADMIN)
    
    // 3. Khai báo biến boolean cho trạng thái tài khoản:
    // Mặc định khởi tạo các giá trị an toàn ban đầu ngay trên thanh RAM khi đối tượng được đúc.
    protected boolean isLocked = false;   // Trạng thái khóa tài khoản (true nếu bị Admin khóa)
    protected boolean isVerified = false; // Trạng thái xác thực tài khoản (dành cho bảo mật mở rộng)

    /**
     * Hàm khởi tạo (Constructor) cơ bản cho đối tượng Người dùng.
     * Thực hiện cấp phát các thuộc tính định danh cơ bản của người dùng trên thanh Heap Memory.
     * 
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @param email Địa chỉ email
     */
    public User(String username, String password, String email) {
        super(); // 4. super(): Gọi Constructor của Entity để đúc ra id ngẫu nhiên duy nhất cho tài khoản này.
        this.username = username;
        this.password = password;
        this.email = email;
    }

    // 5. Các phương thức Getter và Setter (Encapsulation): 
    // Giúp các lớp khác đọc/ghi dữ liệu an toàn dưới sự kiểm soát của đối tượng.
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
     * 
     * @return true nếu tài khoản đang bị khóa, ngược lại false
     */
    public boolean isLocked() { return isLocked; }
    
    /**
     * Thiết lập trạng thái khóa/mở khóa tài khoản.
     * 
     * @param locked Giá trị true để khóa, false để mở khóa
     */
    public void setLocked(boolean locked) { isLocked = locked; }
    
    /**
     * Kiểm tra xem tài khoản đã được xác thực chưa.
     * 
     * @return true nếu đã xác thực, ngược lại false
     */
    public boolean isVerified() { return isVerified; }
    
    /**
     * Thiết lập trạng thái xác thực cho tài khoản.
     * 
     * @param verified Giá trị true hoặc false
     */
    public void setVerified(boolean verified) { isVerified = verified; }
}