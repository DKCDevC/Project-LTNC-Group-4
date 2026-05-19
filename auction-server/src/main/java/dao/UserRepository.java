// 1. Khai báo package: Nằm trong phân hệ DAO (Data Access Object) quản lý cơ sở dữ liệu.
package dao;

// 2. Import mô hình User
import models.User;

/**
 * Interface UserRepository định nghĩa các hợp đồng (contracts) truy cập dữ liệu người dùng.
 * Giúp tách biệt phần logic nghiệp vụ (Services) và tầng lưu trữ dữ liệu (DAO).
 * 
 * Ý nghĩa thiết kế của Repository Design Pattern:
 * - Domain-Driven Design (DDD): Đóng vai trò là một hợp đồng trừu tượng (Abstraction layer) che chở cho tầng Domain/Service.
 *   Phía Service (UserManager) chỉ tương tác với UserRepository, hoàn toàn không phụ thuộc hay biết về việc dữ liệu được lưu trữ 
 *   ở đâu (SQLite, MySQL, Oracle hay thậm chí là lưu tạm thời trên Memory Array).
 * - Loose Coupling: Giảm thiểu sự liên kết cứng nhắc, giúp dễ dàng viết các lớp Mock (giả lập dữ liệu) để chạy Unit Test 
 *   mà không cần kết nối cơ sở dữ liệu thực.
 */
public interface UserRepository {
    
    /**
     * Xác thực thông tin người dùng đăng nhập bằng cả username hoặc email.
     * 
     * @param identifier Tên đăng nhập hoặc Địa chỉ Email
     * @param password Mật khẩu
     * @return Đối tượng User tương ứng nếu khớp thông tin trong cơ sở dữ liệu, ngược lại null
     */
    User loginUser(String identifier, String password);
    
    /**
     * Thêm mới một tài khoản người dùng vào kho lưu trữ bền vững.
     * 
     * @param username Tên tài khoản đăng nhập mong muốn (Khóa chính duy nhất)
     * @param password Mật khẩu tài khoản
     * @param email Địa chỉ email liên hệ
     * @param role Vai trò người dùng (Ví dụ: "BIDDER", "SELLER")
     * @return true nếu lưu dòng tài khoản thành công, ngược lại false
     */
    boolean insertUser(String username, String password, String email, String role);
}
