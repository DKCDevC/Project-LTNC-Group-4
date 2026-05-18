package services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dao.OrderDAO;
import models.Item;
import models.Auction;
import services.AuctionManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Lớp DashboardService chịu trách nhiệm thu thập, tính toán và tổng hợp dữ liệu KPI 
 * phục vụ việc hiển thị Dashboard phân tích kinh doanh của Người bán (Seller Dashboard).
 * Dữ liệu trả về được định dạng dưới cấu trúc JsonObject (GSON) để dễ truyền tải mạng socket và phân tích ở phía Client.
 * Áp dụng mẫu Singleton (Singleton Pattern).
 */
public class DashboardService {
    // Thể hiện duy nhất của DashboardService
    private static DashboardService instance;

    /**
     * Hàm khởi tạo riêng tư.
     */
    private DashboardService() {}

    /**
     * Lấy thể hiện duy nhất của DashboardService (Thread-safe).
     * @return Đối tượng DashboardService duy nhất
     */
    public static synchronized DashboardService getInstance() {
        if (instance == null) {
            instance = new DashboardService();
        }
        return instance;
    }

    /**
     * Thu thập toàn bộ dữ liệu thống kê cho Dashboard của một Người bán cụ thể.
     * Bao gồm:
     * - Danh sách sản phẩm đang bán (gồm trạng thái, giá thầu hiện tại).
     * - Chỉ số doanh thu tổng (Revenue).
     * - Số lượng phiên thầu đang mở (Active Auctions).
     * - Danh sách hóa đơn bán thành công gần nhất.
     * - Dữ liệu biểu đồ doanh thu 7 ngày gần đây.
     * @param sellerName Tên tài khoản người bán cần lấy dữ liệu phân tích
     * @return Đối tượng JsonObject chứa đầy đủ dữ liệu tổng hợp
     */
    public JsonObject getSellerDashboardData(String sellerName) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "SUCCESS");

        // 1. Lấy toàn bộ sản phẩm từ DB
        List<Item> allItems = ItemManager.getInstance().getAllItems();
        System.out.println(">>> [DEBUG] DashboardService: Tìm thấy tổng cộng " + allItems.size() + " sản phẩm trong DB.");
        JsonArray productsArray = new JsonArray();

        int activeAuctionsCount = 0;
        double totalRevenue = 0;
        LocalDateTime now = LocalDateTime.now();

        // Duyệt sản phẩm và lọc những sản phẩm thuộc quyền của Người bán này
        for (Item item : allItems) {
            String itemSeller = (item.getSeller() != null) ? item.getSeller().getUsername() : "Unknown";
            System.out.println(">>> [DEBUG] Kiểm tra sản phẩm: " + item.getName() + " | Seller: " + itemSeller + " | Yêu cầu: " + sellerName);
            
            if (itemSeller.equalsIgnoreCase(sellerName) || "Unknown".equalsIgnoreCase(itemSeller)) {
                JsonObject pObj = new JsonObject();
                pObj.addProperty("id", item.getId());
                pObj.addProperty("name", item.getName());
                pObj.addProperty("description", item.getDescription() != null ? item.getDescription() : "");

                // Ánh xạ đa hình kiểu sản phẩm sang chuỗi JSON đơn giản
                String itemType = "GENERAL";
                if (item instanceof models.Electronics) itemType = "ELECTRONICS";
                else if (item instanceof models.Art) itemType = "ART";
                else if (item instanceof models.Vehicle) itemType = "VEHICLE";
                pObj.addProperty("type", itemType);

                // Lấy giá thầu cao nhất hiện tại (nếu đang chạy thầu thì lấy từ AuctionManager)
                double currentVal = item.getStartingPrice();
                Auction activeAuction = AuctionManager.getInstance().getAuction(item.getId());
                if (activeAuction != null) {
                    currentVal = activeAuction.getItem().getCurrentHighestPrice();
                }

                pObj.addProperty("price", String.format("%,.0f", currentVal));

                // Định dạng thời gian kết thúc thầu để hiển thị thân thiện trên UI
                if (item.getEndTime() != null) {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    pObj.addProperty("endTime", item.getEndTime().format(fmt));
                } else {
                    pObj.addProperty("endTime", "");
                }

                // Xác định trạng thái hoạt động dựa trên thời gian kết thúc thầu
                if (now.isAfter(item.getEndTime())) {
                    pObj.addProperty("status", "Đã kết thúc");
                } else {
                    pObj.addProperty("status", "Đang đấu giá");
                    activeAuctionsCount++;
                }
                productsArray.add(pObj);
            }
        }

        // 2. Lấy dữ liệu hóa đơn và tổng doanh thu thực tế từ cơ sở dữ liệu
        OrderDAO orderDao = new OrderDAO();
        totalRevenue = orderDao.getTotalRevenue(sellerName);

        response.addProperty("totalRevenue", String.format("%,.0f", totalRevenue));
        response.addProperty("activeAuctions", String.valueOf(activeAuctionsCount));

        // 3. Đóng gói danh sách đơn hàng đã bán thành công
        JsonArray ordersArray = new JsonArray();
        List<Map<String, String>> orderList = orderDao.getOrdersBySeller(sellerName);
        for (Map<String, String> order : orderList) {
            JsonObject oObj = new JsonObject();
            oObj.addProperty("orderId", order.get("orderId"));
            oObj.addProperty("itemName", order.get("itemName"));
            oObj.addProperty("buyerName", order.get("buyerName"));
            oObj.addProperty("price", order.get("price"));
            oObj.addProperty("orderDate", order.get("orderDate"));
            ordersArray.add(oObj);
        }

        response.addProperty("pendingOrders", String.valueOf(orderList.size()));
        response.add("products", productsArray);
        response.add("orders", ordersArray);

        // 4. Tổng hợp dữ liệu vẽ biểu đồ cột doanh thu 7 ngày gần đây
        // Chuyển đổi tên các ngày trong tuần sang tiếng Việt viết tắt (CN, T2, T3...T7) để giao diện thêm chuyên nghiệp.
        JsonArray chartData = new JsonArray();
        Map<String, Double> realStats = orderDao.getRevenueLast7Days(sellerName);
        for (Map.Entry<String, Double> entry : realStats.entrySet()) {
            JsonObject dayObj = new JsonObject();
            java.time.LocalDate date = java.time.LocalDate.parse(entry.getKey());
            int dayOfWeek = date.getDayOfWeek().getValue();
            
            // LocalDate.getDayOfWeek() trả về 1 (Monday) -> 7 (Sunday)
            String label = (dayOfWeek == 7) ? "CN" : "T" + (dayOfWeek + 1);
            dayObj.addProperty("day", label);
            dayObj.addProperty("revenue", entry.getValue());
            chartData.add(dayObj);
        }
        response.add("chartData", chartData);

        return response;
    }
}
