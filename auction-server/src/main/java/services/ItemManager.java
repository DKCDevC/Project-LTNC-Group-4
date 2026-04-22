package services;

import dao.ItemDAO;
import models.Item;
import models.Seller;
import java.util.List;

public class ItemManager {
    private static ItemManager instance;
    private ItemDAO itemDAO;

    private ItemManager() {
        // Khởi tạo DAO thay vì HashMap
        itemDAO = new ItemDAO();
    }

    public static synchronized ItemManager getInstance() {
        if (instance == null) {
            instance = new ItemManager();
        }
        return instance;
    }

    public boolean addItem(String productId, Item newItem, Seller owner) {
        boolean success = itemDAO.insertItem(productId, newItem);
        if (success) {
            System.out.println(">>> Seller [" + owner.getUsername() + "] đã lưu sản phẩm xuống Database: " + newItem.getName());
        }
        return success;
    }

    public boolean updateItem(String productId, String newName, String newDesc, double newStartPrice) {
        // Cập nhật thẳng vào Database
        return itemDAO.updateItem(productId, newName, newDesc, newStartPrice);
    }

    public boolean deleteItem(String productId) {
        return itemDAO.deleteItem(productId);
    }

    public List<Item> getAllItems() {
        return itemDAO.getAllItems();
    }
}