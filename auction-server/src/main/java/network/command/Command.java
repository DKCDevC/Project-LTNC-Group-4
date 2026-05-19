// 1. Khai báo package: Định nghĩa không gian tên (namespace) cho file, giúp Java quản lý thư mục và tránh trùng lặp tên class.
package network.command;

// 2. Import lớp JsonObject từ thư viện Google Gson: Dùng để biểu diễn và thao tác dữ liệu dạng đối tượng JSON (cấu trúc key-value) nhận từ Client.
import com.google.gson.JsonObject;

// 3. Import lớp ClientHandler: Đại diện cho luồng xử lý kết nối riêng biệt của mỗi Client kết nối đến Server, chứa thông tin phiên làm việc.
import network.ClientHandler;

// 4. Import lớp PrintWriter từ thư viện Java I/O: Dùng để ghi dữ liệu chuỗi (String) ra luồng xuất của Socket để gửi phản hồi trực tiếp về Client.
import java.io.PrintWriter;

/**
 * Interface Command định nghĩa giao diện chung cho tất cả các Lệnh xử lý mạng (Mẫu thiết kế Command - Command Pattern).
 * Đóng vai trò đóng gói toàn bộ yêu cầu nghiệp vụ và các thông số cần thiết từ phía Client thành một đối tượng xử lý độc lập.
 * Giúp tách biệt hoàn toàn logic định tuyến tin nhắn mạng (Routing) và logic xử lý nghiệp vụ thực tế (Business Logic).
 */
public interface Command {
    
    /**
     * Thực thi lệnh xử lý nghiệp vụ cụ thể dựa trên dữ liệu yêu cầu từ Client gửi lên.
     * 
     * @param requestData Đối tượng chứa dữ liệu yêu cầu dạng JsonObject gửi từ Client (ví dụ: thông tin thầu, tài khoản đăng nhập)
     * @param out Đối tượng PrintWriter dùng để ghi phản hồi trực tiếp xuống Socket của Client tương ứng ngay lập tức
     * @param handler Đối tượng ClientHandler quản lý phiên kết nối hiện tại của Client này (dùng để tra cứu session, username)
     */
    void execute(JsonObject requestData, PrintWriter out, ClientHandler handler);
}
