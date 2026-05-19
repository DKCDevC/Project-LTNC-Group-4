// 1. Khai báo package: Nằm trong phân hệ command quản lý các lớp lệnh mạng của Server.
package network.command;

// 2. Import Gson xử lý JSON, cấu trúc dữ liệu User và UserManager tầng nghiệp vụ.
import com.google.gson.JsonObject;
import models.User;
import services.UserManager;
import network.ClientHandler;
import java.io.PrintWriter;

/**
 * Lớp LoginCommand chịu trách nhiệm xử lý yêu cầu đăng nhập hệ thống của người dùng.
 * Triển khai interface Command (Command Design Pattern).
 * Lớp này thực hiện kiểm tra thông tin danh tính, xác thực trạng thái tài khoản,
 * duy trì phiên kết nối (Stateful Session) và định hướng điều hướng giao diện phía Client.
 */
public class LoginCommand implements Command {
    
    /**
     * Thực thi nghiệp vụ xác thực đăng nhập tài khoản.
     * Trích xuất thông tin định danh (tên đăng nhập hoặc địa chỉ email) và mật khẩu từ gói tin Client gửi lên.
     * Kiểm tra trạng thái khóa tài khoản (isLocked) để ngăn chặn truy cập trái phép.
     * Lưu trữ tên người dùng đăng nhập thành công vào session ClientHandler hiện hành để gán quyền cho các tác vụ thầu/đăng thầu tiếp theo.
     * 
     * @param loginData Dữ liệu cấu hình tài khoản gửi lên từ client dạng JSON
     * @param out Luồng ghi dữ liệu mạng gửi phản hồi về client
     * @param handler Session quản lý socket tương ứng
     */
    @Override
    public void execute(JsonObject loginData, PrintWriter out, ClientHandler handler) {
        // 3. Khớp thông tin đăng nhập linh hoạt (Flexible Authentication Identifier):
        // Hỗ trợ cả hai hình thức: Đăng nhập bằng tên tài khoản (username) hoặc bằng địa chỉ hòm thư (email).
        // Sử dụng toán tử ba ngôi để trích xuất trường dữ liệu một cách an toàn.
        String identifier = loginData.has("username") ? loginData.get("username").getAsString() : 
                            (loginData.has("email") ? loginData.get("email").getAsString() : "");
        String password = loginData.has("password") ? loginData.get("password").getAsString() : "";

        // 4. Chuyển tiếp nghiệp vụ xuống UserManager để tra cứu đĩa cơ sở dữ liệu SQLite:
        User loggedInUser = UserManager.getInstance().login(identifier, password);

        if (loggedInUser != null) {
            // 5. Kiểm tra kiểm toán bảo mật (Security & Audit check):
            // Nếu tài khoản hợp lệ về thông tin nhưng đang trong danh sách đen bị khóa bởi Admin (isLocked == 1),
            // Server lập tức chặn đứng, không cấp phiên đăng nhập, in log và gửi phản hồi lỗi về Client.
            if (loggedInUser.isLocked()) {
                out.println("{\"status\":\"FAILED\", \"message\":\"Tài khoản của bạn đã bị khóa bởi Admin!\"}");
                System.out.println(">>> Từ chối đăng nhập: User [" + loggedInUser.getUsername() + "] đang bị khóa.");
                return;
            }
            
            // 6. Đăng ký duy trì trạng thái phiên (Stateful Session Registry):
            // Lưu giữ trực tiếp tên tài khoản vào biến `currentUsername` của ClientHandler hiện hành trên Heap RAM.
            // Điều này biến kết nối TCP Socket thô từ "Stateless" thành "Stateful", giúp Server nhận diện được 
            // ai là người đang gửi các gói tin Đặt thầu (BID) hay Đăng thầu (ADD_ITEM) tiếp theo mà không cần client gửi lại pass.
            handler.setCurrentUsername(loggedInUser.getUsername()); 
            
            // 7. Giao thức phản hồi bảo mật vai trò (Role-based Navigation Protocol):
            // Gửi chuỗi JSON xác thực thành công chứa Vai trò (ADMIN, SELLER, BIDDER) 
            // để phía giao diện JavaFX của Client nhận diện và tải đúng trang Dashboard tương thích cho người dùng.
            out.println("{\"status\":\"SUCCESS\", \"role\":\"" + loggedInUser.getRole() + "\"}");
            System.out.println(">>> User [" + loggedInUser.getUsername() + "] đã đăng nhập thành công!");
        } else {
            // Phản hồi lỗi đăng nhập nếu sai tài khoản/mật khẩu
            out.println("{\"status\":\"FAILED\", \"message\":\"Sai tai khoản hoặc mật khẩu\"}");
        }
    }
}
