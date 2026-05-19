package models;

/**
 * Lớp Admin đại diện cho Quản trị viên quản lý cấp cao trong hệ thống đấu giá eBid.
 * 
 * Các nguyên lý thiết kế hướng đối tượng (OOP) áp dụng:
 * 1. Kế thừa (Inheritance): Lớp Admin kế thừa toàn bộ thuộc tính và hành vi cốt lõi từ lớp cha `User` 
 *    (như thông tin định danh, mật khẩu bảo mật, địa chỉ liên lạc email).
 * 2. Liskov Substitution Principle (Nguyên lý thay thế Liskov - LSP): Lớp Admin có thể thay thế hoàn toàn 
 *    cho lớp cha `User` trong mọi ngữ cảnh nghiệp vụ mà không làm thay đổi tính đúng đắn của chương trình.
 * 3. Constructor Chaining (Liên kết Hàm khởi tạo): Sử dụng từ khóa `super(...)` để gọi hàm dựng của lớp cha, 
 *    đảm bảo thuộc tính kế thừa được khởi tạo đồng bộ, an toàn trước khi chạy các thiết lập con.
 * 4. Method Overriding (Ghi đè phương thức): Ghi đè phương thức getRole() để đặc tả hóa quyền hạn "ADMIN".
 */
public class Admin extends User {
    
    /**
     * Hàm khởi tạo đối tượng Quản trị viên (Admin).
     * Thực hiện liên kết ngược lên hàm khởi tạo của lớp cha `User` thông qua `super()`.
     * 
     * @param username Tên đăng nhập Admin duy nhất
     * @param password Mật khẩu mã hóa bảo mật của Admin
     * @param email Địa chỉ email chính thức liên lạc của Admin
     */
    public Admin(String username, String password, String email) {
        super(username, password, email);
    }

    /**
     * Ghi đè phương thức getRole() để định nghĩa quyền hành chính cao cấp nhất trong hệ thống.
     * 
     * @return Chuỗi vai trò cố định "ADMIN"
     */
    @Override
    public String getRole() {
        return "ADMIN";
    }
}