package services.strategies;

import models.Bidder;
import services.AuctionManager;

public interface BiddingStrategy {
    AuctionManager.BidResult processBid(String auctionId, Bidder bidder, double amount, String itemId);
}
