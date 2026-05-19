// 1. Khai báo package: Nằm trong phân hệ dịch vụ nghiệp vụ (Services) của Server.
package services;

// 2. Import tầng truy cập dữ liệu DAO và các models nghiệp vụ.
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
 * 
 * Ý nghĩa thiết kế của Service Layer:
 * - Decoupling (Tách biệt trách nhiệm): Che giấu tầng truy cập DB trực tiếp (DAO) khỏi tầng mạng (Command/Socket).
 * - Centralized Business Logic (Tập trung nghiệp vụ): Toàn bộ luật lệ kiểm tra tính hợp lệ trước khi thao tác 
 *   với DB được đặt tập trung tại đây để dễ bảo trì và mở rộng sau này.
 * - Singleton Design Pattern: Đảm bảo chỉ tồn tại duy nhất một đối tượng điều phối sản phẩm trên toàn hệ thống.
 */
public class ItemManager {
    // 3. Khai báo thể hiện duy nhất lưu trữ trên Heap RAM
    private static ItemManager instance;
    
    // Đối tượng truy cập cơ sở dữ liệu sản phẩm (SQLite)
    private ItemDAO itemDAO;

    /**
     * Hàm khởi tạo riêng tư (Private Constructor) chống việc đúc đối tượng tự do.
     */
    private ItemManager() {
        itemDAO = new ItemDAO();
    }

    /**
     * Lấy thể hiện duy nhất của ItemManager (Thread-safe).
     * Sử dụng synchronized cấp lớp để đảm bảo an toàn tuyệt đối khi khởi chạy đa luồng.
     * 
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
     * Liên kết thực thể Item và Seller trước khi đẩy xuống lưu trữ DB đĩa cứng SQLite.
     * 
     * @param id Mã ID của sản phẩm
     * @param item Đối tượng Item cần thêm
     * @param seller Đối tượng người bán
     */
    public void addItem(String id, Item item, Seller seller) {
        // Gắn liên kết 2 chiều giữa thực thể Item và Seller (Object Association)
        item.setSeller(seller);
        itemDAO.addItem(id, item);
        System.out.println(">>> Đã lưu sản phẩm " + item.getName() + " vào Database.");
    }

    /**
     * Cập nhật thông tin sản phẩm có kiểm tra bảo mật quyền sở hữu và tính toàn vẹn trạng thái.
     * 
     * Quy tắc nghiệp vụ (Business Logic Check):
     * 1. Kiểm tra tồn tại: Thực thể Item mục tiêu phải tồn tại trong DB.
     * 2. Bảo mật quyền hạn (Security Owner Audit): Người yêu cầu sửa (`requesterUsername`) phải trùng khớp
     *    với người sở hữu (Seller) của sản phẩm này, tránh việc đối thủ cạnh tranh phá hoại hoặc hack giá khởi điểm.
     * 3. An toàn trạng thái (Data Integrity Check): Tuyệt đối không cho phép cập nhật thông tin (như tên hay giá khởi điểm)
     *    khi sản phẩm đó đang nằm trong một phiên đấu giá đang chạy (`RUNNING`). Việc này ngăn chặn lỗi logic thầu
     *    khi người thắng đặt giá dựa trên thông tin cũ nhưng sản phẩm đã bị thay đổi thông số ở nền.
     * 
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

        // 1. KIỂM TRA QUYỀN SỞ HỮU (Security Owner Audit)
        if (!item.getSeller().getUsername().equals(requesterUsername)) {
            System.out.println("!!! CẢNH BÁO: User " + requesterUsername + " thử cập nhật sản phẩm không thuộc quyền sở hữu!");
            return false;
        }

        // 2. KIỂM TRA TRẠNG THÁI ĐẤU GIÁ (Data Integrity Check)
        Auction auction = AuctionManager.getInstance().getAuction(productId);
        if (auction != null && auction.getStatus() == AuctionStatus.RUNNING) {
            System.out.println("!!! LỖI: Không thể cập nhật sản phẩm khi đấu giá đang diễn ra!");
            return false;
        }

        // Nếu vượt qua cả 2 bước kiểm duyệt trên, thực hiện gọi xuống DAO để ghi đè bền vững xuống SQLite DB
        return itemDAO.updateItem(productId, newName, newDesc, newStartPrice);
    }

    /**
     * Xóa sản phẩm khỏi hệ thống có kiểm tra bảo mật quyền sở hữu và tính toàn vẹn trạng thái.
     * 
     * Quy tắc nghiệp vụ (Business Logic Check):
     * 1. Quyền sở hữu (Security check): requesterUsername phải trùng khớp chủ sản phẩm.
     * 2. An toàn dữ liệu (Data integrity check): Không cho phép xóa sản phẩm khi đang diễn ra đấu giá (RUNNING).
     *    Ngăn chặn lỗi "mất dấu vết thực thể" (Dangling Reference) nếu sản phẩm bị xóa trong khi người dùng khác đang đặt thầu.
     * 
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
        Auction auction = AuctionManager.getInstance().getAuction(productId);
        if (auction != null && auction.getStatus() == AuctionStatus.RUNNING) {
            System.out.println("!!! LỖI: Không thể xóa sản phẩm khi đấu giá đang diễn ra!");
            return false;
        }

        // Gọi xuống DAO để thực thi câu lệnh SQL DELETE xóa dòng dữ liệu khỏi bảng sản phẩm
        return itemDAO.deleteItem(productId);
    }

    /**
     * Lấy toàn bộ danh sách sản phẩm đăng đấu giá từ tầng DB.
     * @return Danh sách các Item
     */
    public List<Item> getAllItems() {
        return itemDAO.getAllItems();
    }
}