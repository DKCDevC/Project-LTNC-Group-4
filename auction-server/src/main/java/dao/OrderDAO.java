// 1. Khai báo package: Nằm trong phân hệ DAO (Data Access Object) quản lý cơ sở dữ liệu.
package dao;

// 2. Import các tiện ích kết nối mạng cơ sở dữ liệu và cấu trúc dữ liệu Java
import database.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

/**
 * Lớp OrderDAO (Data Access Object) quản lý việc lưu trữ hóa đơn (orders),
 * tính toán thống kê doanh thu bán hàng và lấy lịch sử giao dịch thành công cho Người bán.
 * 
 * Vai trò kiến trúc:
 * - Persistence Layer (Tầng lưu trữ bền vững): Tương tác trực tiếp với bảng `orders` của SQLite.
 * - Triển khai an toàn hóa: Áp dụng PreparedStatement ngăn chặn SQL Injection, đóng luồng qua try-with-resources.
 */
public class OrderDAO {

    /**
     * Tạo một hóa đơn bán hàng thành công mới vào Database SQLite sau khi phiên đấu giá kết thúc.
     * 
     * Quy trình xử lý (Input -> Process -> Output):
     * 1. Đầu vào (Input): Nhận mã sản phẩm, tài khoản người bán, người mua thắng cuộc, và giá chung cuộc.
     * 2. Xử lý (Process): Nạp các tham số vào câu lệnh INSERT bằng phương thức setString/setDouble an toàn.
     * 3. Đầu ra (Output): Trả về true nếu chèn thành công dòng dữ liệu, ngược lại false.
     * 
     * @param itemId Mã sản phẩm đấu giá thành công
     * @param sellerName Tên tài khoản người bán
     * @param bidderName Tên tài khoản người mua (thắng cuộc)
     * @param finalPrice Giá bán chung cuộc
     * @return true nếu lưu hóa đơn thành công, ngược lại false
     */
    public boolean insertOrder(String itemId, String sellerName, String bidderName, double finalPrice) {
        String query = "INSERT INTO orders (item_id, seller_name, bidder_name, final_price) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, itemId);
            stmt.setString(2, sellerName);
            stmt.setString(3, bidderName);
            stmt.setDouble(4, finalPrice);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi lưu đơn hàng: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tính tổng doanh thu (tổng giá trị tất cả hóa đơn thành công) của một Người bán.
     * Thực hiện hàm gộp SUM SQL trực tiếp ở tầng dữ liệu nhằm tối ưu băng thông đường truyền và RAM.
     * 
     * @param sellerName Tên tài khoản người bán
     * @return Tổng số tiền doanh thu bán hàng
     */
    public double getTotalRevenue(String sellerName) {
        String query = "SELECT SUM(final_price) as total FROM orders WHERE seller_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, sellerName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    /**
     * Lấy dữ liệu thống kê doanh thu theo từng ngày trong 7 ngày gần đây nhất của Người bán.
     * Sử dụng LinkedHashMap để bảo đảm thứ tự thời gian từ cũ nhất đến mới nhất trên biểu đồ.
     * 
     * Kỹ thuật xử lý đệm ngày (Date Padding & LinkedHashMap):
     * - `LinkedHashMap`: Khác với HashMap thông thường (sắp xếp ngẫu nhiên dựa trên mã băm Hashcode), 
     *   LinkedHashMap duy trì một danh sách liên kết đôi (Double-linked list) xuyên qua tất cả các phần tử.
     *   Nó đảm bảo thứ tự duyệt (Iteration Order) luôn trùng khớp với thứ tự chèn phần tử (Insertion Order).
     * - Date Padding (Bù khuyết ngày): Khởi tạo trước 7 ngày gần nhất với giá trị mặc định bằng 0.0. 
     *   Việc này cực kỳ quan trọng để đảm bảo biểu đồ Client vẽ ra liên tục, không bị méo mó hay khuyết cột 
     *   ở những ngày mà người bán đó không bán được sản phẩm nào.
     * - Sử dụng SQLite Date Functions: `DATE('now', '-7 days')` để lọc nhanh dữ liệu dưới 1 tuần.
     * 
     * @param sellerName Tên tài khoản người bán
     * @return Bản đồ (Map) ánh xạ từ chuỗi ngày (yyyy-MM-dd) sang doanh thu ngày đó (giữ đúng thứ tự chèn)
     */
    public Map<String, Double> getRevenueLast7Days(String sellerName) {
        // Sử dụng LinkedHashMap để giữ đúng thứ tự hiển thị thời gian trên biểu đồ
        Map<String, Double> revenueData = new LinkedHashMap<>();

        // Khởi tạo trước 7 ngày gần nhất với giá trị mặc định bằng 0.0 để tránh bị khuyết ngày
        for (int i = 6; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).toString();
            revenueData.put(date, 0.0);
        }

        // Truy vấn tính tổng doanh thu nhóm theo ngày trong khoảng 7 ngày trở lại đây
        String query = "SELECT order_date, SUM(final_price) as daily_sum " +
                "FROM orders WHERE seller_name = ? " +
                "AND order_date >= DATE('now', '-7 days') " +
                "GROUP BY order_date";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, sellerName);
            ResultSet rs = stmt.executeQuery();

            // Điền dữ liệu doanh thu thực tế truy vấn được vào Map, ghi đè giá trị 0.0 khởi tạo
            while (rs.next()) {
                revenueData.put(rs.getString("order_date"), rs.getDouble("daily_sum"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return revenueData;
    }

    /**
     * Lấy danh sách các đơn hàng đã giao dịch thành công của Người bán.
     * Thực hiện LEFT JOIN bảng orders với bảng items để hiển thị tên sản phẩm, ngay cả khi sản phẩm gốc đã bị xóa.
     * 
     * Kỹ thuật SQL & Xử lý Null:
     * - LEFT JOIN: Giúp lấy toàn bộ dòng hóa đơn của bảng bên trái (`orders`) ngay cả khi sản phẩm tương ứng 
     *   trong bảng bên phải (`items`) đã bị xóa khỏi hệ thống.
     * - `COALESCE(i.name, 'Sản phẩm đã xóa')`: Hàm COALESCE của SQL sẽ trả về tham số phi-null đầu tiên. 
     *   Nếu sản phẩm bị người bán xóa mất khỏi SQLite (`i.name` trả về NULL), hệ thống sẽ hiển thị chuỗi thay thế 
     *   "Sản phẩm đã xóa" thay vì bị trống tên hoặc gây lỗi NullPointerException trên giao diện.
     * 
     * @param sellerName Tên tài khoản người bán
     * @return Danh sách các đơn hàng dạng bản đồ thuộc tính (Map<String, String>)
     */
    public List<Map<String, String>> getOrdersBySeller(String sellerName) {
        List<Map<String, String>> orders = new ArrayList<>();

        // Truy vấn LEFT JOIN kết nối bảng hóa đơn và sản phẩm để lấy tên sản phẩm
        String query = "SELECT o.order_id, o.item_id, o.bidder_name, o.final_price, o.order_date, " +
                "COALESCE(i.name, 'Sản phẩm đã xóa') as item_name " +
                "FROM orders o LEFT JOIN items i ON o.item_id = i.id " +
                "WHERE o.seller_name = ? ORDER BY o.order_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, sellerName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, String> order = new HashMap<>();
                order.put("orderId", String.valueOf(rs.getInt("order_id")));
                order.put("itemName", rs.getString("item_name"));
                order.put("buyerName", rs.getString("bidder_name") != null ? rs.getString("bidder_name") : "N/A");
                order.put("price", String.format("%,.0f", rs.getDouble("final_price"))); // Định dạng số tiền có phân tách hàng nghìn
                order.put("orderDate", rs.getString("order_date") != null ? rs.getString("order_date") : "N/A");
                orders.add(order);
            }
        } catch (SQLException e) {
            System.out.println("Lỗi lấy danh sách đơn hàng: " + e.getMessage());
        }
        return orders;
    }

    public boolean markOrderAsCompletedByItemId(String itemId) {
        String query = "UPDATE orders SET status = 'FINISHED' WHERE item_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, itemId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi cập nhật trạng thái đơn hàng: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật tất cả đơn hàng chờ thanh toán của người mua sang FINISHED.
     */
    public boolean markAllOrdersAsFinishedByBidderName(String bidderName) {
        String query = "UPDATE orders SET status = 'FINISHED' WHERE bidder_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, bidderName);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi thanh toán toàn bộ đơn hàng: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy trạng thái của đơn hàng từ Database.
     */
    public String getOrderStatusByItemId(String itemId) {
        String query = "SELECT status FROM orders WHERE item_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, itemId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("status");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "PENDING";
    }

    /**
     * Lấy tên người chiến thắng đấu giá của sản phẩm dựa trên cơ sở dữ liệu hóa đơn.
     */
    public String getWinnerByItemId(String itemId) {
        String query = "SELECT bidder_name FROM orders WHERE item_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, itemId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("bidder_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}