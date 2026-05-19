// 1. Khai báo package: Nằm trong phân hệ command thuộc network của Server.
package network.command;

// 2. Import các cấu trúc dữ liệu Map & HashMap của thư viện Java Collections:
import java.util.HashMap;
import java.util.Map;

/**
 * Lớp CommandRouter đóng vai trò là "Bộ định tuyến lệnh trung tâm" (Registry & Router).
 * Quản lý việc đăng ký bản đồ các lệnh (registry map) và tra cứu các đối tượng Command 
 * dựa trên nhãn chuỗi (command name) của gói tin nhận được từ mạng.
 * 
 * Thiết kế áp dụng mẫu thiết kế Singleton (Singleton Design Pattern) để đảm bảo:
 * - Chỉ có DUY NHẤT một đối tượng Router tồn tại trong suốt vòng đời chạy của Server.
 * - Cung cấp một điểm truy cập toàn cục duy nhất (`CommandRouter.getInstance()`) cho toàn bộ máy chủ.
 */
public class CommandRouter {
    // 3. Khai báo biến static riêng tư lưu trữ thể hiện (instance) duy nhất của lớp này trên Heap memory.
    private static CommandRouter instance;
    
    // 4. Khai báo Map lưu trữ ánh xạ động giữa Chuỗi định danh lệnh (Key) và Đối tượng Command xử lý tương ứng (Value).
    // Sử dụng từ khóa final để chống gán đè lại địa chỉ ô nhớ sau khi khởi tạo.
    private final Map<String, Command> commands = new HashMap<>();

    /**
     * Hàm khởi tạo riêng tư (Private Constructor).
     * Ngăn cản tuyệt đối các lớp khác bên ngoài sử dụng từ khóa `new CommandRouter()`.
     * Ràng buộc bắt buộc phải đi qua phương thức getInstance().
     */
    private CommandRouter() {}

    /**
     * Lấy thể hiện duy nhất của CommandRouter.
     * Sử dụng từ khóa `synchronized` ở cấp độ phương thức để đảm bảo an toàn đa luồng (Thread-safe).
     * Khi có nhiều ClientHandler kết nối đồng thời và cùng lúc gọi hàm này lần đầu tiên, 
     * cơ chế khóa Monitor của Java sẽ bắt các Thread xếp hàng đi vào để tránh việc đúc ra 2 đối tượng Router 
     * song song trên RAM (Lazy Initialization Hazard).
     * 
     * @return Đối tượng CommandRouter duy nhất
     */
    public static synchronized CommandRouter getInstance() {
        if (instance == null) {
            instance = new CommandRouter(); // Khởi tạo trì hoãn (Lazy Initialization)
        }
        return instance;
    }

    /**
     * Đăng ký một Lệnh xử lý mới vào bản đồ định tuyến.
     * 
     * @param commandName Tên lệnh mạng (ví dụ: "LOGIN", "BID", "AUTO_BID")
     * @param command Đối tượng Command thực thi lệnh
     */
    public void registerCommand(String commandName, Command command) {
        commands.put(commandName, command); // Lưu liên kết vào HashMap
    }

    /**
     * Tra cứu lấy đối tượng thực thi Command tương ứng với tên lệnh.
     * Tìm kiếm trên cấu trúc HashMap có độ phức tạp thuật toán cực kỳ tối ưu là O(1).
     * 
     * @param commandName Tên lệnh cần tra cứu
     * @return Đối tượng Command nếu đã đăng ký, ngược lại null
     */
    public Command getCommand(String commandName) {
        return commands.get(commandName);
    }
}
