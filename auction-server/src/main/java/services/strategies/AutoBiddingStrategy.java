package services.strategies;

import models.Bidder;
import services.AuctionManager;

public class AutoBiddingStrategy implements BiddingStrategy {
    private final double maxBid;
    private final double increment;

    public AutoBiddingStrategy(double maxBid, double increment) {
        this.maxBid = maxBid;
        this.increment = increment;
    }

    @Override
    public AuctionManager.BidResult processBid(String auctionId, Bidder bidder, double amount, String itemId) {
        return AuctionManager.getInstance().registerAutoBid(auctionId, bidder, maxBid, increment, itemId);
    }
}
