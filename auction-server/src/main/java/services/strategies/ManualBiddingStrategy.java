package services.strategies;

import models.Bidder;
import services.AuctionManager;

public class ManualBiddingStrategy implements BiddingStrategy {
    @Override
    public AuctionManager.BidResult processBid(String auctionId, Bidder bidder, double amount, String itemId) {
        return AuctionManager.getInstance().placeBid(auctionId, bidder, amount, itemId);
    }
}
