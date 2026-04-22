package dao;

import models.Art;
import models.Electronics;
import models.Item;
import models.Vehicle;
import services.ItemFactory;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    public List<Item> getAllItems() {
        List<Item> itemList = new ArrayList<>();
        String query = "SELECT * FROM items";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String type = rs.getString("type");
                String name = rs.getString("name");
                String description = rs.getString("description");
                double startingPrice = rs.getDouble("starting_price");
                LocalDateTime startTime = LocalDateTime.parse(rs.getString("start_time"));
                LocalDateTime endTime = LocalDateTime.parse(rs.getString("end_time"));
                String extraInfo = rs.getString("extra_info");

                Item item = ItemFactory.createItem(type, name, description, startingPrice, startTime, endTime, extraInfo);

                item.setCurrentHighestPrice(startingPrice);
                itemList.add(item);
            }
        } catch (SQLException e) {
            System.out.println("Lỗi tải danh sách sản phẩm: " + e.getMessage());
        }
        return itemList;
    }

    public boolean insertItem(String productId, Item item) {
        String query = "INSERT INTO items (id, name, description, starting_price, start_time, end_time, type, extra_info) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        String type = "UNKNOWN";
        String extraInfo = "";

        if (item instanceof Electronics) {
            type = "ELECTRONICS";
            extraInfo = String.valueOf(((Electronics) item).getWarrantyMonths());
        } else if (item instanceof Art) {
            type = "ART";
            // Lấy đúng biến artistName theo code của bạn
            extraInfo = ((Art) item).getArtistName();
        } else if (item instanceof Vehicle) {
            type = "VEHICLE";
            extraInfo = ((Vehicle) item).getBrand();
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, productId);
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getDescription());
            stmt.setDouble(4, item.getStartingPrice());
            stmt.setString(5, item.getStartTime().toString());
            stmt.setString(6, item.getEndTime().toString());
            stmt.setString(7, type);
            stmt.setString(8, extraInfo);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi thêm sản phẩm (Có thể trùng ID): " + e.getMessage());
            return false;
        }
    }

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
            System.out.println("Lỗi cập nhật sản phẩm: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteItem(String productId) {
        String query = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, productId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi xóa sản phẩm: " + e.getMessage());
            return false;
        }
    }

}