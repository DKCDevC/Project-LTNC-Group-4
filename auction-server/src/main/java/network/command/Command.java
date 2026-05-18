package network.command;

import com.google.gson.JsonObject;
import network.ClientHandler;
import java.io.PrintWriter;

/**
 * Interface Command định nghĩa giao diện chung cho tất cả các Lệnh xử lý mạng (Command Pattern).
 * Đóng vai trò đóng gói yêu cầu nghiệp vụ từ phía Client thành một đối tượng độc lập.
 */
public interface Command {
    
    /**
     * Thực thi lệnh xử lý nghiệp vụ cụ thể dựa trên dữ liệu yêu cầu từ Client.
     * @param requestData Đối tượng dữ liệu yêu cầu dạng JsonObject
     * @param out Bộ ghi dữ liệu ra Socket của Client gửi yêu cầu để phản hồi lập tức
     * @param handler Bộ quản lý phiên kết nối của Client tương ứng (chứa session thông tin)
     */
    void execute(JsonObject requestData, PrintWriter out, ClientHandler handler);
}
