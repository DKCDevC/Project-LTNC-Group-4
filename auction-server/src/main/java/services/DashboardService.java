// 1. Khai báo package: Nằm trong phân hệ dịch vụ nghiệp vụ (Services) của Server.
package services;

// 2. Import các cấu trúc JSON của Google Gson dùng để truyền gói tin mạng
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
 * 
 * Kiến trúc tổng hợp (Analytics Aggregation Logic):
 * - Singleton Design Pattern: Đảm bảo chỉ tồn tại duy nhất một đối tượng điều phối phân tích dữ liệu trên RAM.
 * - HSON Tree Structure: Xây dựng cấu trúc cây lồng nhau (Nested Trees) kết hợp các thuộc tính thô,
 *   bảng JSON Array của sản phẩm, đơn hàng đã bán, và tập giá trị biểu đồ cột 7 ngày qua cổng SQLite.
 */
public class DashboardService {
    // 3. Khai báo thể hiện duy nhất lưu trữ trên Heap RAM
    private static DashboardService instance;

    /**
     * Hàm khởi tạo riêng tư chống tạo đối tượng tự do.
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
     * 
     * Luồng chạy chi tiết (Input -> Process -> Output):
     * 1. Đầu vào (Input): Nhận tên người bán (`sellerName`).
     * 2. Xử lý (Process):
     *    - Bước 2.1: Gọi `ItemManager` lấy danh sách sản phẩm. Duyệt qua để lọc ra sản phẩm thuộc chủ quyền,
     *      ánh xạ đa hình kiểu sản phẩm (Art, Electronics, Vehicle) sang chuỗi String đơn giản.
     *    - Bước 2.2: So khớp thời gian kết thúc thầu với `LocalDateTime.now()` để phân định trạng thái Đã kết thúc / Đang đấu giá.
     *    - Bước 2.3: Truy vấn `OrderDAO` lấy doanh số tổng thực tế bán được qua DB.
     *    - Bước 2.4: Lấy danh sách hóa đơn thành công gần nhất.
     *    - Bước 2.5: Thống kê doanh thu theo 7 ngày gần đây nhất, chuyển đổi định dạng nhãn ngày sang tiếng Việt (T2, T3... CN).
     * 3. Đầu ra (Output): Trả về JsonObject chứa toàn bộ cây thông tin đã tổng hợp để truyền đi qua Socket mạng.
     * 
     * @param sellerName Tên tài khoản người bán cần lấy dữ liệu phân tích
     * @return Đối tượng JsonObject chứa đầy đủ dữ liệu tổng hợp
     */
    public JsonObject getSellerDashboardData(String sellerName) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "SUCCESS");

        // 1. Lấy toàn bộ sản phẩm từ DB qua ItemManager
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
            
            // So khớp bỏ qua chữ hoa chữ thường để lọc chính xác sản phẩm thuộc chủ quyền
            if (itemSeller.equalsIgnoreCase(sellerName) || "Unknown".equalsIgnoreCase(itemSeller)) {
                JsonObject pObj = new JsonObject();
                pObj.addProperty("id", item.getId());
                pObj.addProperty("name", item.getName());
                pObj.addProperty("description", item.getDescription() != null ? item.getDescription() : "");

                // Ánh xạ đa hình kiểu sản phẩm (Polymorphic Type Identification):
                // Dùng từ khóa `instanceof` của Java để xác định lớp con thực tế (Art, Electronics, Vehicle)
                // và chuyển hóa thành thẻ chuỗi đơn giản giúp client hiển thị icon chuyên biệt.
                String itemType = "GENERAL";
                if (item instanceof models.Electronics) itemType = "ELECTRONICS";
                else if (item instanceof models.Art) itemType = "ART";
                else if (item instanceof models.Vehicle) itemType = "VEHICLE";
                pObj.addProperty("type", itemType);

                // Lấy giá thầu cao nhất hiện tại (nếu đang chạy thầu trực tuyến thì lấy từ AuctionManager trong RAM)
                double currentVal = item.getStartingPrice();
                Auction activeAuction = AuctionManager.getInstance().getAuction(item.getId());
                if (activeAuction != null) {
                    currentVal = activeAuction.getItem().getCurrentHighestPrice();
                }

                // Định dạng tiền tệ đẹp mắt không số lẻ thập phân (Ví dụ: 15,000,000)
                pObj.addProperty("price", String.format("%,.0f", currentVal));

                // Định dạng thời gian kết thúc thầu để hiển thị thân thiện trên UI Client (Pattern: dd/MM/yyyy HH:mm)
                if (item.getEndTime() != null) {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    pObj.addProperty("endTime", item.getEndTime().format(fmt));
                } else {
                    pObj.addProperty("endTime", "");
                }

                // Xác định trạng thái hoạt động dựa trên trạng thái phiên đấu giá trong RAM/DB
                String statusStr = "Đang đấu giá";
                activeAuction = AuctionManager.getInstance().getAuction(item.getId());
                if (activeAuction != null) {
                    models.AuctionStatus status = activeAuction.getStatus();
                    if (status == models.AuctionStatus.ENDED_NO_WINNER) {
                        statusStr = "Đã kết thúc";
                    } else if (status == models.AuctionStatus.ENDED_WITH_WINNER) {
                        statusStr = "Chờ thanh toán";
                    } else if (status == models.AuctionStatus.FINISHED) {
                        statusStr = "Hoàn thành";
                    } else if (status == models.AuctionStatus.OPEN) {
                        statusStr = "Chờ mở";
                    } else if (status == models.AuctionStatus.RUNNING) {
                        statusStr = "Đang đấu giá";
                        activeAuctionsCount++;
                    }
                } else {
                    // Check order DB fallback
                    OrderDAO orderDaoTmp = new OrderDAO();
                    String orderStatus = orderDaoTmp.getOrderStatusByItemId(item.getId());
                    String winner = orderDaoTmp.getWinnerByItemId(item.getId());
                    if (winner != null) {
                        if ("FINISHED".equals(orderStatus)) {
                            statusStr = "Hoàn thành";
                        } else {
                            statusStr = "Chờ thanh toán";
                        }
                    } else {
                        if (now.isAfter(item.getEndTime())) {
                            statusStr = "Đã kết thúc";
                        } else {
                            statusStr = "Đang đấu giá";
                            activeAuctionsCount++;
                        }
                    }
                }
                pObj.addProperty("status", statusStr);
                productsArray.add(pObj);
            }
        }

        // 2. Lấy dữ liệu hóa đơn và tổng doanh thu thực tế từ cơ sở dữ liệu qua OrderDAO
        OrderDAO orderDao = new OrderDAO();
        totalRevenue = orderDao.getTotalRevenue(sellerName);

        // Nạp các chỉ số KPI thô đầu tiên vào cây JSON phản hồi
        response.addProperty("totalRevenue", String.format("%,.0f", totalRevenue));
        response.addProperty("activeAuctions", String.valueOf(activeAuctionsCount));

        // 3. Đóng gói danh sách đơn hàng đã bán thành công gần đây nhất
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

        // 4. Tổng hợp dữ liệu vẽ biểu đồ cột doanh thu 7 ngày gần đây (Localized 7-day mapping):
        // Chuyển đổi tên các ngày trong tuần sang tiếng Việt viết tắt (CN, T2, T3...T7) để giao diện biểu đồ thêm chuyên nghiệp.
        JsonArray chartData = new JsonArray();
        Map<String, Double> realStats = orderDao.getRevenueLast7Days(sellerName);
        for (Map.Entry<String, Double> entry : realStats.entrySet()) {
            JsonObject dayObj = new JsonObject();
            java.time.LocalDate date = java.time.LocalDate.parse(entry.getKey());
            int dayOfWeek = date.getDayOfWeek().getValue();
            
            // LocalDate.getDayOfWeek() trả về 1 (Monday) -> 7 (Sunday)
            // Ánh xạ sang cấu trúc ngày viết tắt tiếng Việt:
            String label = (dayOfWeek == 7) ? "CN" : "T" + (dayOfWeek + 1);
            dayObj.addProperty("day", label);
            dayObj.addProperty("revenue", entry.getValue());
            chartData.add(dayObj);
        }
        response.add("chartData", chartData);

        return response;
    }
}
