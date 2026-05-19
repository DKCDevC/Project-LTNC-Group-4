// 1. Khai báo package: Định nghĩa không gian tên của class để thuận tiện quản lý mã nguồn dự án.
package models;

// 2. Import lớp ArrayList và List từ gói java.util: Dùng để làm cấu trúc dữ liệu lưu trữ danh sách giao dịch đấu giá và danh sách robot.
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Auction đại diện cho một Phiên đấu giá cụ thể diễn ra trong hệ thống.
 * Kế thừa từ lớp cha trừu tượng Entity (Inheritance - Tính kế thừa): Nhận lại thuộc tính id tự sinh và ngày tạo của Entity.
 * Đóng vai trò là một lớp trung gian (Wrapper/Aggregate) gộp các thông tin liên quan đến quy trình đấu giá của một sản phẩm.
 */
public class Auction extends Entity {
    // 3. Khai báo biến item: Đại diện cho sản phẩm đang được đấu giá trong phiên này (Composition - quan hệ HAS-A).
    private Item item;
    
    // 4. Khai báo biến seller: Đại diện cho người bán sản phẩm này (Composition - quan hệ HAS-A).
    private Seller seller;
    
    // 5. Khai báo biến status: Trạng thái hiện tại của phiên đấu giá (sử dụng kiểu Enum AuctionStatus để tránh nhập sai trạng thái thô).
    private AuctionStatus status;
    
    // 6. Khai báo biến bidHistory: Danh sách lưu trữ lịch sử tất cả các lượt đặt giá (cả thủ công lẫn tự động).
    private List<BidTransaction> bidHistory;
    
    // 7. Khai báo biến winner: Đối tượng người đấu giá tạm thời dẫn đầu hoặc thắng cuộc cuối cùng của phiên này.
    private Bidder winner;
    
    // 8. Khai báo biến auctionId: Mã định danh duy nhất của phiên đấu giá (thường đồng bộ trùng với ID của sản phẩm để dễ quản lý).
    private String auctionId;
    
    // 9. Khai báo biến autoBids: Danh sách quản lý các robot đặt giá tự động (AutoBid) đã được người dùng đăng ký vào phiên này.
    private List<AutoBid> autoBids = new ArrayList<>();

    // 10. Các hàm Getter và Setter cho mã định danh ID phiên đấu giá.
    public String getAuctionId() {
        return this.auctionId;
    }

    public void setAuctionId(String auctionId){
        this.auctionId=auctionId;
    }
    
    /**
     * Hàm khởi tạo (Constructor) một phiên đấu giá hoàn chỉnh.
     * Trạng thái mặc định ban đầu của phiên sẽ là OPEN (Mở).
     * 
     * @param item Đối tượng sản phẩm được mang ra đấu giá
     * @param seller Đối tượng người bán sản phẩm
     */
    public Auction(Item item, Seller seller) {
        super(); // 11. Gọi Constructor mặc định của lớp cha Entity để tự động tạo ID duy nhất cho thực thể này.
        this.item = item; // 12. Liên kết đối tượng sản phẩm truyền vào với phiên đấu giá.
        this.seller = seller; // 13. Liên kết đối tượng người bán truyền vào với phiên đấu giá.
        this.status = AuctionStatus.OPEN; // 14. Thiết lập trạng thái ban đầu của phiên đấu giá là mở (OPEN).
        this.bidHistory = new ArrayList<>(); // 15. Cấp phát vùng nhớ cho ArrayList để bắt đầu ghi nhận lịch sử các giao dịch đặt thầu.
    }

    /**
     * Ghi nhận một giao dịch đặt giá thầu mới thành công vào lịch sử của phiên.
     * Đồng thời tự động cập nhật mức giá thầu cao nhất hiện tại của sản phẩm liên kết.
     * 
     * @param bid Giao dịch đặt thầu mới cần ghi nhận
     */
    public void addBid(BidTransaction bid) {
        // 16. Thêm đối tượng giao dịch đặt thầu mới vào cuối danh sách lịch sử mảng động.
        this.bidHistory.add(bid);
        // 17. Cập nhật mức giá thầu cao nhất hiện tại của sản phẩm bằng số tiền thầu vừa giao dịch.
        this.item.setCurrentHighestPrice(bid.getAmount());
    }

    /**
     * Đăng ký thêm một robot đặt giá tự động (AutoBid) của người dùng vào phiên đấu giá này.
     * 
     * @param autoBid Cấu hình Auto-bid của người đấu giá
     */
    public void registerAutoBid(AutoBid autoBid) {
        // 18. Thêm robot đặt giá tự động mới vào danh sách theo dõi của phiên đấu giá.
        this.autoBids.add(autoBid);
    }

    // 19. Các phương thức Getter/Setter thông dụng để truy xuất thông tin của đối tượng (đảm bảo tính đóng gói).
    public Item getItem() { return item; }
    public Seller getSeller() { return seller; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public List<BidTransaction> getBidHistory() { return bidHistory; }

    public Bidder getWinner() { return winner; }
    public void setWinner(Bidder winner) { this.winner = winner; }
    public List<AutoBid> getAutoBids() { return autoBids; }
}