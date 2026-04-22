package models;

import java.time.LocalDateTime;

public class AutoBid {
    private Bidder bidder;
    private double maxBid;
    private double increment;
    private LocalDateTime registerTime;

    public AutoBid(Bidder bidder, double maxBid, double increment) {
        this.bidder = bidder;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registerTime = LocalDateTime.now();
    }

    public Bidder getBidder() { return bidder; }
    public double getMaxBid() { return maxBid; }
    public double getIncrement() { return increment; }
    public LocalDateTime getRegisterTime() { return registerTime; }
}