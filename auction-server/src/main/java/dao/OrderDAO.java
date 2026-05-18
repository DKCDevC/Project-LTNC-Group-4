package dao;

import utils.DBConnection;
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
 */
public class OrderDAO {

    /**
     * Tạo một hóa đơn bán hàng thành công mới vào Database sau khi phiên đấu giá kết thúc.
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
     * @param sellerName Tên tài khoản người bán
     * @return Bản đồ (Map) ánh xạ từ chuỗi ngày (yyyy-MM-dd) sang doanh thu ngày đó
     */
    public Map<String, Double> getRevenueLast7Days(String sellerName) {
        // Sử dụng LinkedHashMap để giữ đúng thứ tự hiển thị thời gian
        Map<String, Double> revenueData = new LinkedHashMap<>();

        // Khởi tạo trước 7 ngày gần nhất với giá trị mặc định bằng 0.0 để tránh bị khuyết ngày trên biểu đồ
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

            // Điền dữ liệu doanh thu thực tế truy vấn được vào Map
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
     * @param sellerName Tên tài khoản người bán
     * @return Danh sách các đơn hàng dạng bản đồ thuộc tính (Map<String, String>) để dễ truyền lên bảng giao diện
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
}