// 1. Khai báo package: Nằm trong phân hệ command quản lý các lớp lệnh mạng của Server.
package network.command;

// 2. Import Gson xử lý JSON, DashboardService tầng nghiệp vụ phân tích dữ liệu.
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import services.DashboardService;
import utils.GsonConfig;
import network.ClientHandler;
import java.io.PrintWriter;

/**
 * Lớp GetSellerDashboardCommand xử lý yêu cầu tải dữ liệu thống kê phân tích (Dashboard) của Người bán.
 * Triển khai interface Command (Command Design Pattern).
 * Lớp này chịu trách nhiệm thu thập số liệu doanh thu trực quan, biểu đồ tăng trưởng 7 ngày,
 * danh sách đơn hàng đã thanh lý từ SQLite qua DashboardService và gửi về client hiển thị các biểu đồ (Charts).
 */
public class GetSellerDashboardCommand implements Command {
    // 3. Đối tượng GSON cấu hình định dạng ngày tháng tương thích dùng chung
    private final Gson gson = GsonConfig.createGson();

    /**
     * Thực thi nghiệp vụ lấy dữ liệu Dashboard người bán.
     * Trích xuất tên người bán từ phiên kết nối hoặc từ tham số gói tin.
     * Gọi DashboardService để tổng hợp số liệu (Doanh thu, danh sách đơn hàng đã bán, biểu đồ doanh thu 7 ngày).
     * Phản hồi dạng JSON hoàn chỉnh về cho giao diện Client hiển thị đồ thị và bảng chỉ số.
     * 
     * @param requestData Gói dữ liệu yêu cầu gửi từ client dạng JSON
     * @param out Luồng ghi dữ liệu mạng gửi phản hồi về client
     * @param handler Session quản lý socket tương ứng
     */
    @Override
    public void execute(JsonObject requestData, PrintWriter out, ClientHandler handler) {
        // 4. Ưu tiên lấy tên người bán từ phiên đăng nhập thực tế (Session Tracking):
        // Nếu không có, mới dùng phương án dự phòng lấy trực tiếp từ thuộc tính "username" do client truyền lên.
        String sellerName = (handler.getCurrentUsername() != null) ? 
                            handler.getCurrentUsername() : 
                            (requestData.has("username") ? requestData.get("username").getAsString() : "");
        
        // 5. Kiểm duyệt hợp lệ đầu vào (Validation Check):
        if (sellerName.isEmpty()) {
            out.println("{\"status\":\"FAILED\", \"message\":\"Username is required\"}");
            return;
        }

        // 6. Tính toán chỉ số và phản hồi (Analytics Engine and Payload response):
        // DashboardService sẽ thực thi các truy vấn SQL phức tạp kết hợp SUM, COUNT và GROUP BY 
        // để thống kê doanh số bán ra trong tuần của người dùng này, đúc thành cấu trúc JsonObject lồng nhau.
        JsonObject dashboardData = DashboardService.getInstance().getSellerDashboardData(sellerName);
        
        // Tuần tự hóa JSON hoàn chỉnh và đẩy qua cổng mạng
        out.println(gson.toJson(dashboardData));
    }
}
