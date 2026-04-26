package services;

import dao.ItemDAO;
import models.Item;
import models.Seller;
import models.Auction;
import models.AuctionStatus;
import java.util.List;

public class ItemManager {
    private static ItemManager instance;
    private ItemDAO itemDAO;

    private ItemManager() {
        itemDAO = new ItemDAO();
    }

    public static synchronized ItemManager getInstance() {
        if (instance == null) {
            instance = new ItemManager();
        }
        return instance;
    }

    public void addItem(String id, Item item, Seller seller) {
        item.setSeller(seller);
        itemDAO.addItem(id, item);
        System.out.println(">>> Đã lưu sản phẩm " + item.getName() + " vào Database.");
    }

    /**
     * Cập nhật sản phẩm có kiểm tra quyền sở hữu và trạng thái đấu giá.
     */
    public boolean updateItem(String productId, String requesterUsername, String newName, String newDesc, double newStartPrice) {
        Item item = itemDAO.getItemById(productId);
        if (item == null) return false;

        // 1. Kiểm tra quyền sở hữu (Security)
        if (!item.getSeller().getUsername().equals(requesterUsername)) {
            System.out.println("!!! CẢNH BÁO: User " + requesterUsername + " thử cập nhật sản phẩm không thuộc quyền sở hữu!");
            return false;
        }

        // 2. Kiểm tra trạng thái đấu giá (Data Integrity)
        Auction auction = AuctionManager.getInstance().getAuction(productId);
        if (auction != null && auction.getStatus() != AuctionStatus.OPEN) {
            System.out.println("!!! LỖI: Không thể cập nhật sản phẩm khi đấu giá đang diễn ra hoặc đã kết thúc!");
            return false;
        }

        return itemDAO.updateItem(productId, newName, newDesc, newStartPrice);
    }

    /**
     * Xóa sản phẩm có kiểm tra quyền sở hữu và trạng thái đấu giá.
     */
    public boolean deleteItem(String productId, String requesterUsername) {
        Item item = itemDAO.getItemById(productId);
        if (item == null) return false;

        // 1. Kiểm tra quyền sở hữu
        if (!item.getSeller().getUsername().equals(requesterUsername)) {
            System.out.println("!!! CẢNH BÁO: User " + requesterUsername + " thử xóa sản phẩm không thuộc quyền sở hữu!");
            return false;
        }

        // 2. Kiểm tra trạng thái đấu giá
        Auction auction = AuctionManager.getInstance().getAuction(productId);
        if (auction != null && auction.getStatus() != AuctionStatus.OPEN) {
            System.out.println("!!! LỖI: Không thể xóa sản phẩm khi đấu giá đang diễn ra hoặc đã kết thúc!");
            return false;
        }

        return itemDAO.deleteItem(productId);
    }

    public List<Item> getAllItems() {
        return itemDAO.getAllItems();
    }
}