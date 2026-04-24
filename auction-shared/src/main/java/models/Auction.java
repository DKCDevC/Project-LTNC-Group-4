package models;

import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    private Item item;
    private Seller seller;
    private AuctionStatus status;
    private List<BidTransaction> bidHistory;
    private Bidder winner;
    private String auctionId;
    private List<AutoBid> autoBids = new ArrayList<>();

    public String getAuctionId() {
        return this.auctionId;
    }

    public void setAuctionId(String auctionId){
        this.auctionId=auctionId;
    }
    public Auction(Item item, Seller seller) {
        super();
        this.item = item;
        this.seller = seller;
        this.status = AuctionStatus.OPEN;
        this.bidHistory = new ArrayList<>();
    }

    public void addBid(BidTransaction bid) {
        this.bidHistory.add(bid);
        this.item.setCurrentHighestPrice(bid.getAmount());
    }

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
