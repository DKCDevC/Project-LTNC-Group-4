package services;

import dao.ItemDAO;
import models.Item;
import models.Seller;
import models.Auction;
import models.AuctionStatus;
import java.util.List;

/**
 * Lớp ItemManager đóng vai trò là tầng Dịch vụ (Service Layer) quản lý các sản phẩm.
 * Trung chuyển dữ liệu giữa các Controller/Command và lớp ItemDAO.
 * Thực hiện các kiểm tra nghiệp vụ quan trọng như kiểm tra quyền sở hữu sản phẩm 
 * và an toàn trạng thái đấu giá (không được xóa/sửa khi đang chạy đấu giá).
 * Áp dụng mẫu thiết kế Singleton (Singleton Pattern).
 */
public class ItemManager {
    // Thể hiện duy nhất của ItemManager
    private static ItemManager instance;
    
    // Đối tượng truy cập cơ sở dữ liệu sản phẩm
    private ItemDAO itemDAO;

    /**
     * Hàm khởi tạo riêng tư.
     */
    private ItemManager() {
        itemDAO = new ItemDAO();
    }

    /**
     * Lấy thể hiện duy nhất của ItemManager (Thread-safe).
     * @return Đối tượng ItemManager duy nhất
     */
    public static synchronized ItemManager getInstance() {
        if (instance == null) {
            instance = new ItemManager();
        }
        return instance;
    }

    /**
     * Thêm sản phẩm mới và gán người bán tương ứng.
     * @param id Mã ID của sản phẩm
     * @param item Đối tượng Item cần thêm
     * @param seller Đối tượng người bán
     */
    public void addItem(String id, Item item, Seller seller) {
        item.setSeller(seller);
        itemDAO.addItem(id, item);
        System.out.println(">>> Đã lưu sản phẩm " + item.getName() + " vào Database.");
    }

    /**
     * Cập nhật thông tin sản phẩm có kiểm tra bảo mật quyền sở hữu và tính toàn vẹn trạng thái.
     * @param productId Mã sản phẩm cần cập nhật
     * @param requesterUsername Tên tài khoản gửi yêu cầu cập nhật
     * @param newName Tên mới
     * @param newDesc Mô tả mới
     * @param newStartPrice Giá khởi điểm mới
     * @return true nếu cập nhật thành công và hợp lệ, ngược lại false
     */
    public boolean updateItem(String productId, String requesterUsername, String newName, String newDesc, double newStartPrice) {
        Item item = itemDAO.getItemById(productId);
        if (item == null) return false;

        // 1. KIỂM TRA QUYỀN SỞ HỮU (Security Check)
        // Chỉ cho phép chính người bán đã đăng sản phẩm này thực hiện chỉnh sửa thông tin.
        if (!item.getSeller().getUsername().equals(requesterUsername)) {
            System.out.println("!!! CẢNH BÁO: User " + requesterUsername + " thử cập nhật sản phẩm không thuộc quyền sở hữu!");
            return false;
        }

        // 2. KIỂM TRA TRẠNG THÁI ĐẤU GIÁ (Data Integrity Check)
        // Nếu sản phẩm đã được đưa vào một phiên đấu giá đang diễn ra (RUNNING), tuyệt đối không được phép chỉnh sửa.
        Auction auction = AuctionManager.getInstance().getAuction(productId);
        if (auction != null && auction.getStatus() == AuctionStatus.RUNNING) {
            System.out.println("!!! LỖI: Không thể cập nhật sản phẩm khi đấu giá đang diễn ra!");
            return false;
        }

        // Nếu vượt qua cả 2 bước kiểm duyệt trên, thực hiện gọi xuống DAO để cập nhật database
        return itemDAO.updateItem(productId, newName, newDesc, newStartPrice);
    }

    /**
     * Xóa sản phẩm khỏi hệ thống có kiểm tra bảo mật quyền sở hữu và tính toàn vẹn trạng thái.
     * @param productId Mã sản phẩm cần xóa
     * @param requesterUsername Tên tài khoản yêu cầu xóa sản phẩm
     * @return true nếu xóa thành công và hợp lệ, ngược lại false
     */
    public boolean deleteItem(String productId, String requesterUsername) {
        Item item = itemDAO.getItemById(productId);
        if (item == null) return false;

        // 1. KIỂM TRA QUYỀN SỞ HỮU (Security Check)
        if (!item.getSeller().getUsername().equals(requesterUsername)) {
            System.out.println("!!! CẢNH BÁO: User " + requesterUsername + " thử xóa sản phẩm không thuộc quyền sở hữu!");
            return false;
        }

        // 2. KIỂM TRA TRẠNG THÁI ĐẤU GIÁ (Data Integrity Check)
        // Tránh tình trạng đang đặt thầu dở dang mà sản phẩm đột ngột bị xóa khỏi hệ thống.
        Auction auction = AuctionManager.getInstance().getAuction(productId);
        if (auction != null && auction.getStatus() == AuctionStatus.RUNNING) {
            System.out.println("!!! LỖI: Không thể xóa sản phẩm khi đấu giá đang diễn ra!");
            return false;
        }

        // Gọi xuống DAO để xóa dòng dữ liệu
        return itemDAO.deleteItem(productId);
    }

    /**
     * Lấy toàn bộ danh sách sản phẩm đăng đấu giá.
     * @return Danh sách các Item
     */
    public List<Item> getAllItems() {
        return itemDAO.getAllItems();
    }
}