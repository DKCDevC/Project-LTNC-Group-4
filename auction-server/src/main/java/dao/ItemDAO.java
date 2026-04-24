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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    // ---------------------------------------------------------------
    // READ
    // ---------------------------------------------------------------

    public List<Item> getAllItems() {
        List<Item> itemList = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM items");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Item item = mapRow(rs);
                if (item != null) itemList.add(item);
            }
        } catch (SQLException e) {
            System.out.println("Lỗi tải danh sách sản phẩm: " + e.getMessage());
        }
        return itemList;
    }

    /** Fetch a single item by ID ??????????? used for GET_ITEM_DETAIL. */
    public Item getItemById(String id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM items WHERE id = ?")) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.out.println("Lỗi getItemById: " + e.getMessage());
        }
        return null;
    }

    private Item mapRow(ResultSet rs) throws SQLException {
        String id            = rs.getString("id");
        String type          = rs.getString("type");
        String name          = rs.getString("name");
        String description   = rs.getString("description");
        double startingPrice = rs.getDouble("starting_price");
        LocalDateTime startTime = LocalDateTime.parse(rs.getString("start_time"));
        LocalDateTime endTime   = LocalDateTime.parse(rs.getString("end_time"));
        String extraInfo     = rs.getString("extra_info");
        String sellerName    = rs.getString("seller_name");
        String saleType      = rs.getString("sale_type");
        String imageUrls     = rs.getString("image_urls");
        double reservePrice  = rs.getDouble("reserve_price");
        double minIncrement  = rs.getDouble("min_increment");

        Item item = ItemFactory.createItem(type, name, description, startingPrice, startTime, endTime, extraInfo);
        item.setId(id);
        if (sellerName != null) item.setSeller(new Seller(sellerName, "", ""));
        item.setSaleType(saleType != null ? saleType : "AUCTION");
        item.setImageUrls(imageUrls != null ? imageUrls : "");
        item.setReservePrice(reservePrice);
        item.setMinIncrement(minIncrement);
        // Seed currentHighestPrice from highest bid persisted in DB
        double highest = getHighestBidAmount(id);
        item.setCurrentHighestPrice(highest > 0 ? highest : startingPrice);
        return item;
    }

    private double getHighestBidAmount(String itemId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT MAX(amount) as max_amt FROM bids WHERE item_id = ?")) {
            stmt.setString(1, itemId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getDouble("max_amt");
        } catch (SQLException ignored) {}
        return 0;
    }

    // ---------------------------------------------------------------
    // WRITE
    // ---------------------------------------------------------------

    public void addItem(String id, Item item) {
        String sql = "INSERT INTO items(id, name, description, starting_price, start_time, end_time, " +
                     "type, extra_info, seller_name, sale_type, image_urls, reserve_price, min_increment) " +
                     "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());
            pstmt.setDouble(4, item.getStartingPrice());
            pstmt.setString(5, item.getStartTime().toString());
            pstmt.setString(6, item.getEndTime().toString());

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
            pstmt.setString(9, item.getSeller() != null ? item.getSeller().getUsername() : "Unknown");
            pstmt.setString(10, item.getSaleType());
            pstmt.setString(11, item.getImageUrls());
            pstmt.setDouble(12, item.getReservePrice());
            pstmt.setDouble(13, item.getMinIncrement());

            pstmt.executeUpdate();
            System.out.println(">>> [DB] Lưu sản phẩm: " + item.getName());

        } catch (SQLException e) {
            System.out.println("Lỗi thêm sản phẩm: " + e.getMessage());
        }
    }

    public boolean updateItem(String productId, String newName, String newDesc, double newStartPrice) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE items SET name=?, description=?, starting_price=? WHERE id=?")) {
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
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM items WHERE id=?")) {
            stmt.setString(1, productId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi xóa sản phẩm: " + e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------------
    // BID PERSISTENCE
    // ---------------------------------------------------------------

    /** Persist a valid bid for audit trail and price recovery on restart. */
    public void saveBid(String itemId, String bidderName, double amount) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO bids(item_id, bidder_name, amount, bid_time) VALUES(?,?,?,?)")) {
            pstmt.setString(1, itemId);
            pstmt.setString(2, bidderName);
            pstmt.setDouble(3, amount);
            pstmt.setString(4, LocalDateTime.now().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Lỗi lưu bid: " + e.getMessage());
        }
    }

    /**
     * Returns the most recent {@code limit} bids for an item, oldest-first.
     * Each entry: [amount (String), bid_time (String)]
     */
    public List<String[]> getBidHistory(String itemId, int limit) {
        List<String[]> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT amount, bid_time FROM bids WHERE item_id=? ORDER BY id DESC LIMIT ?")) {
            pstmt.setString(1, itemId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            List<String[]> tmp = new ArrayList<>();
            while (rs.next()) {
                tmp.add(new String[]{String.valueOf(rs.getDouble("amount")), rs.getString("bid_time")});
            }
            // Reverse so caller gets oldest-first
            for (int i = tmp.size() - 1; i >= 0; i--) result.add(tmp.get(i));
        } catch (SQLException e) {
            System.out.println("Lỗi đọc bid history: " + e.getMessage());
        }
        return result;
    }
}
