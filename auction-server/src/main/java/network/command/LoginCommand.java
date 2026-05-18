package network.command;

import com.google.gson.JsonObject;
import models.User;
import services.UserManager;
import network.ClientHandler;
import java.io.PrintWriter;

/**
 * Lớp LoginCommand chịu trách nhiệm xử lý yêu cầu đăng nhập hệ thống của người dùng.
 * Triển khai interface Command.
 */
public class LoginCommand implements Command {
    
    /**
     * Thực thi nghiệp vụ xác thực đăng nhập tài khoản.
     * Trích xuất thông tin định danh (tên đăng nhập hoặc địa chỉ email) và mật khẩu từ gói tin Client gửi lên.
     * Kiểm tra trạng thái khóa tài khoản (isLocked) để ngăn chặn truy cập trái phép.
     * Lưu trữ tên người dùng đăng nhập thành công vào session ClientHandler hiện hành để gán quyền cho các tác vụ thầu/đăng thầu tiếp theo.
     */
    @Override
    public void execute(JsonObject loginData, PrintWriter out, ClientHandler handler) {
        // Hỗ trợ linh hoạt: Khớp thông tin đăng nhập theo tên đăng nhập (username) hoặc email
        String identifier = loginData.has("username") ? loginData.get("username").getAsString() : 
                            (loginData.has("email") ? loginData.get("email").getAsString() : "");
        String password = loginData.has("password") ? loginData.get("password").getAsString() : "";

        // Gọi UserManager thực hiện tra cứu cơ sở dữ liệu
        User loggedInUser = UserManager.getInstance().login(identifier, password);

        if (loggedInUser != null) {
            // Kiểm tra bảo mật: Nếu tài khoản đang bị khóa bởi Quản trị viên (Admin)
            if (loggedInUser.isLocked()) {
                out.println("{\"status\":\"FAILED\", \"message\":\"Tài khoản của bạn đã bị khóa bởi Admin!\"}");
                System.out.println(">>> Từ chối đăng nhập: User [" + loggedInUser.getUsername() + "] đang bị khóa.");
                return;
            }
            
            // GHI NHẬN PHIÊN ĐĂNG NHẬP (Session Registry): Lưu username vào đối tượng Handler kết nối Socket
            handler.setCurrentUsername(loggedInUser.getUsername()); 
            
            // Phản hồi thành công kèm vai trò người dùng (ADMIN, SELLER, BIDDER) để Client chuyển trang phù hợp
            out.println("{\"status\":\"SUCCESS\", \"role\":\"" + loggedInUser.getRole() + "\"}");
            System.out.println(">>> User [" + loggedInUser.getUsername() + "] đã đăng nhập thành công!");
        } else {
            // Phản hồi lỗi đăng nhập
            out.println("{\"status\":\"FAILED\", \"message\":\"Sai tai khoản hoặc mật khẩu\"}");
        }
    }
}
