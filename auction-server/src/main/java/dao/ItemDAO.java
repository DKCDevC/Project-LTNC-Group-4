package dao;

import models.Art;
import models.Electronics;
import models.Item;
import models.Vehicle;
import models.Seller;
import services.ItemFactory;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp ItemDAO (Data Access Object) quản lý việc lưu trữ, cập nhật, xóa
 * và truy vấn thông tin sản phẩm (Items) từ cơ sở dữ liệu SQLite.
 */
public class ItemDAO {
    // Định dạng thời gian chuẩn ISO (ví dụ: 2026-05-18T11:00:00)
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    // Định dạng thời gian thông thường có khoảng trắng (ví dụ: 2026-05-18 11:00:00)
    private static final DateTimeFormatter SPACE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Phương thức phân tích chuỗi ký tự ngày tháng thành đối tượng LocalDateTime.
     * Hỗ trợ tự động nhận diện cả 2 định dạng chuỗi phổ biến (chứa "T" hoặc khoảng trắng).
     * @param dateTimeStr Chuỗi thời gian cần phân tích
     * @return Đối tượng LocalDateTime hợp lệ, hoặc thời điểm mặc định (now + 1 ngày) nếu lỗi
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null) return LocalDateTime.now().plusDays(1);
        try {
            if (dateTimeStr.contains("T")) {
                return LocalDateTime.parse(dateTimeStr, ISO_FORMAT);
            } else {
                return LocalDateTime.parse(dateTimeStr, SPACE_FORMAT);
            }
        } catch (DateTimeParseException e) {
            System.out.println("!!! CẢNH BÁO: Không thể parse ngày tháng: " + dateTimeStr + ". Sử dụng mặc định.");
            return LocalDateTime.now().plusDays(1);
        }
    }

    /**
     * Lấy toàn bộ danh sách sản phẩm đăng đấu giá từ Database.
     * Sử dụng ItemFactory để tạo đối tượng con cụ thể (Electronics, Art, Vehicle...) dựa trên cột type.
     * @return Danh sách các Item
     */
    public List<Item> getAllItems() {
        List<Item> itemList = new ArrayList<>();
        String query = "SELECT * FROM items";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                String type = rs.getString("type");
                String name = rs.getString("name");
                String description = rs.getString("description");
                double startingPrice = rs.getDouble("starting_price");
                LocalDateTime startTime = parseDateTime(rs.getString("start_time"));
                LocalDateTime endTime = parseDateTime(rs.getString("end_time"));
                String extraInfo = rs.getString("extra_info");
                String sellerName = rs.getString("seller_name");
                String imageUrls = rs.getString("image_urls");

                // Sử dụng Factory Pattern để sinh đối tượng Item con thích hợp
                Item item = ItemFactory.createItem(type, name, description, startingPrice, startTime, endTime, extraInfo);
                item.setId(id);
                if (sellerName != null) {
                    item.setSeller(new Seller(sellerName, "", ""));
                }
                item.setImageUrls(imageUrls);
                item.setCurrentHighestPrice(startingPrice);
                itemList.add(item);
            }
        } catch (SQLException e) {
            System.out.println("Lỗi tải danh sách sản phẩm: " + e.getMessage());
        }
        return itemList;
    }

    /**
     * Tìm kiếm một sản phẩm theo mã ID.
     * @param productId Mã sản phẩm cần tìm
     * @return Đối tượng Item nếu tìm thấy, ngược lại null
     */
    public Item getItemById(String productId) {
        String query = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, productId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String type = rs.getString("type");
                String name = rs.getString("name");
                String description = rs.getString("description");
                double startingPrice = rs.getDouble("starting_price");
                LocalDateTime startTime = parseDateTime(rs.getString("start_time"));
                LocalDateTime endTime = parseDateTime(rs.getString("end_time"));
                String extraInfo = rs.getString("extra_info");
                String sellerName = rs.getString("seller_name");

                Item item = ItemFactory.createItem(type, name, description, startingPrice, startTime, endTime, extraInfo);
                item.setId(productId);
                if (sellerName != null) {
                    item.setSeller(new Seller(sellerName, "", ""));
                }
                item.setImageUrls(rs.getString("image_urls"));
                item.setCurrentHighestPrice(startingPrice);
                return item;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lưu một sản phẩm mới vào Database.
     * Phân tích kiểu sản phẩm để trích xuất các thuộc tính đặc trưng lưu vào cột extra_info.
     * @param id Mã định danh duy nhất của sản phẩm
     * @param item Đối tượng Item cần thêm
     */
    public void addItem(String id, Item item) {
        String sql = "INSERT INTO items(id, name, description, starting_price, start_time, end_time, type, extra_info, seller_name, image_urls) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());
            pstmt.setDouble(4, item.getStartingPrice());
            pstmt.setString(5, item.getStartTime().toString());
            pstmt.setString(6, item.getEndTime().toString());

            // Phân tích kiểu của Item để xác định giá trị cho type và extra_info trong DB
            String type = "GENERAL";
            String extraInfo = "";
            if (item instanceof Electronics) {
                type = "ELECTRONICS";
                extraInfo = String.valueOf(((Electronics) item).getWarrantyMonths());
            } else if (item instanceof Art) {
                type = "ART";
                extraInfo = ((Art) item).getArtistName();
            } else if (item instanceof Vehicle) {
                type = "VEHICLE";
                extraInfo = ((Vehicle) item).getBrand();
            }

            pstmt.setString(7, type);
            pstmt.setString(8, extraInfo);

            if (item.getSeller() != null) {
                pstmt.setString(9, item.getSeller().getUsername());
            } else {
                pstmt.setString(9, "Unknown");
            }
            pstmt.setString(10, item.getImageUrls());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Lỗi khi thêm sản phẩm: " + e.getMessage());
        }
    }

    /**
     * Cập nhật thông tin cơ bản của một sản phẩm.
     * @param productId Mã sản phẩm cần sửa
     * @param newName Tên mới
     * @param newDesc Mô tả mới
     * @param newStartPrice Giá khởi điểm mới
     * @return true nếu sửa thành công, ngược lại false
     */
    public boolean updateItem(String productId, String newName, String newDesc, double newStartPrice) {
        String query = "UPDATE items SET name = ?, description = ?, starting_price = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, newName);
            stmt.setString(2, newDesc);
            stmt.setDouble(3, newStartPrice);
            stmt.setString(4, productId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Xóa một sản phẩm khỏi Database.
     * @param productId Mã sản phẩm cần xóa
     * @return true nếu xóa thành công, ngược lại false
     */
    public boolean deleteItem(String productId) {
        String query = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, productId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }
}