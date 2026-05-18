package models;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Auction đại diện cho một Phiên đấu giá cụ thể trong hệ thống.
 * Chứa thông tin về sản phẩm, người bán, trạng thái phiên, lịch sử các lượt đặt giá,
 * người thắng cuộc hiện tại và các cấu hình Auto-bid của phiên này.
 */
public class Auction extends Entity {
    // Sản phẩm (Item) đang được đấu giá trong phiên này
    private Item item;
    
    // Người bán (Seller) sở hữu sản phẩm và tạo phiên đấu giá này
    private Seller seller;
    
    // Trạng thái hiện tại của phiên đấu giá (OPEN, RUNNING, FINISHED...)
    private AuctionStatus status;
    
    // Lịch sử các giao dịch đặt giá thầu (manual hoặc auto-bid)
    private List<BidTransaction> bidHistory;
    
    // Người đấu giá (Bidder) tạm thời dẫn đầu hoặc thắng cuộc cuối cùng
    private Bidder winner;
    
    // Mã định danh riêng của phiên đấu giá (thường trùng khớp với mã ID của sản phẩm để dễ đồng bộ)
    private String auctionId;
    
    // Danh sách các bot tự động nâng giá (AutoBid) đăng ký vào phiên này
    private List<AutoBid> autoBids = new ArrayList<>();

    public String getAuctionId() {
        return this.auctionId;
    }

    public void setAuctionId(String auctionId){
        this.auctionId=auctionId;
    }
    
    /**
     * Hàm khởi tạo một phiên đấu giá mới liên kết với sản phẩm và người bán.
     * Trạng thái mặc định ban đầu là OPEN.
     * @param item Sản phẩm đấu giá
     * @param seller Người bán sản phẩm
     */
    public Auction(Item item, Seller seller) {
        super();
        this.item = item;
        this.seller = seller;
        this.status = AuctionStatus.OPEN;
        this.bidHistory = new ArrayList<>();
    }

    /**
     * Thêm một lượt đặt thầu mới vào lịch sử của phiên đấu giá này.
     * Đồng thời tự động cập nhật giá cao nhất hiện tại (currentHighestPrice) của sản phẩm.
     * @param bid Giao dịch đặt thầu mới
     */
    public void addBid(BidTransaction bid) {
        this.bidHistory.add(bid);
        this.item.setCurrentHighestPrice(bid.getAmount());
    }

    /**
     * Đăng ký một cấu hình đặt thầu tự động (Auto-bid) từ người dùng vào phiên đấu giá này.
     * @param autoBid Cấu hình Auto-bid của người đấu giá
     */
    public void registerAutoBid(AutoBid autoBid) {
        this.autoBids.add(autoBid);
    }

    public Item getItem() { return item; }
    public Seller getSeller() { return seller; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public List<BidTransaction> getBidHistory() { return bidHistory; }

    public Bidder getWinner() { return winner; }
    public void setWinner(Bidder winner) { this.winner = winner; }
    public List<AutoBid> getAutoBids() { return autoBids; }
}