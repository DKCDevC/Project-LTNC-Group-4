package network.command;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import services.DashboardService;
import utils.GsonConfig;
import network.ClientHandler;
import java.io.PrintWriter;

/**
 * Lớp GetSellerDashboardCommand xử lý yêu cầu tải dữ liệu thống kê phân tích (Dashboard) của Người bán.
 * Triển khai interface Command.
 */
public class GetSellerDashboardCommand implements Command {
    // Đối tượng GSON cấu hình định dạng ngày tháng tương thích
    private final Gson gson = GsonConfig.createGson();

    /**
     * Thực thi nghiệp vụ lấy dữ liệu Dashboard người bán.
     * Trích xuất tên người bán từ phiên kết nối hoặc từ tham số gói tin.
     * Gọi DashboardService để tổng hợp số liệu (Doanh thu, danh sách đơn hàng đã bán, biểu đồ doanh thu 7 ngày).
     * Phản hồi dạng JSON hoàn chỉnh về cho giao diện Client hiển thị đồ thị và bảng chỉ số.
     */
    @Override
    public void execute(JsonObject requestData, PrintWriter out, ClientHandler handler) {
        // Ưu tiên lấy tên người bán từ phiên đăng nhập thực tế của kết nối Socket
        String sellerName = (handler.getCurrentUsername() != null) ? 
                            handler.getCurrentUsername() : 
                            (requestData.has("username") ? requestData.get("username").getAsString() : "");
        
        // Kiểm duyệt hợp lệ đầu vào
        if (sellerName.isEmpty()) {
            out.println("{\"status\":\"FAILED\", \"message\":\"Username is required\"}");
            return;
        }

        // Gọi DashboardService để tính toán số liệu và gửi kết quả dạng JSON chuỗi
        JsonObject dashboardData = DashboardService.getInstance().getSellerDashboardData(sellerName);
        out.println(gson.toJson(dashboardData));
    }
}
