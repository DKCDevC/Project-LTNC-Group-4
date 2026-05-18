package network.command;

import java.util.HashMap;
import java.util.Map;

/**
 * Lớp CommandRouter đóng vai trò là "Bộ định tuyến lệnh trung tâm".
 * Quản lý việc đăng ký bản đồ các lệnh (registry map) và tra cứu các đối tượng Command 
 * dựa trên nhãn chuỗi (command name) của gói tin nhận được từ mạng.
 * Thiết kế áp dụng mẫu thiết kế Singleton (Singleton Pattern).
 */
public class CommandRouter {
    // Thể hiện duy nhất của bộ định tuyến
    private static CommandRouter instance;
    
    // Bản đồ lưu trữ liên kết tên lệnh và đối tượng thực thi tương ứng
    private final Map<String, Command> commands = new HashMap<>();

    /**
     * Hàm khởi tạo riêng tư.
     */
    private CommandRouter() {}

    /**
     * Lấy thể hiện duy nhất của CommandRouter (Thread-safe).
     * @return Đối tượng CommandRouter duy nhất
     */
    public static synchronized CommandRouter getInstance() {
        if (instance == null) {
            instance = new CommandRouter();
        }
        return instance;
    }

    /**
     * Đăng ký một Lệnh xử lý mới vào bản đồ định tuyến.
     * @param commandName Tên lệnh mạng (ví dụ: "LOGIN", "BID")
     * @param command Đối tượng Command thực thi lệnh
     */
    public void registerCommand(String commandName, Command command) {
        commands.put(commandName, command);
    }

    /**
     * Tra cứu lấy đối tượng thực thi Command tương ứng với tên lệnh.
     * @param commandName Tên lệnh cần tra cứu
     * @return Đối tượng Command nếu đã đăng ký, ngược lại null
     */
    public Command getCommand(String commandName) {
        return commands.get(commandName);
    }
}
