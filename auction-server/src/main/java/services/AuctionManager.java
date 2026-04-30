package services;

import models.Auction;
import models.AuctionStatus;
import models.BidTransaction;
import models.Bidder;
import models.AutoBid;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import dao.OrderDAO;

public class AuctionManager {

    private static AuctionManager instance;
    private Map<String, Auction> activeAuctions;
    private AuctionNotificationService notificationService;
    private OrderDAO orderDAO;

    private AuctionManager() {
        activeAuctions = new ConcurrentHashMap<>();
        orderDAO = new OrderDAO();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void setNotificationService(AuctionNotificationService service) {
        this.notificationService = service;
    }

    private void notifyObservers(String message) {
        if (notificationService != null) {
            notificationService.notifyAll(message);
        }
    }

    public void addAuction(Auction auction) {
        auction.setStatus(AuctionStatus.RUNNING);
        activeAuctions.put(auction.getAuctionId(), auction);
    }

    public boolean placeBid(String auctionId, Bidder bidder, double bidAmount) {
        Auction auction = activeAuctions.get(auctionId);

        if (auction == null) {
            return false;
        }

        synchronized (auction) {
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                return false;
            }

            if (bidAmount <= auction.getItem().getCurrentHighestPrice()) {
                return false;
            }

            BidTransaction newBid = new BidTransaction(bidder, bidAmount);
            auction.addBid(newBid);
            auction.setWinner(bidder);

            System.out.println("Thành công: " + bidder.getUsername() + " đã đặt giá " + bidAmount);

            handleAntiSniping(auction);
            triggerAutoBids(auction);

            return true;
        }
    }

    private void handleAntiSniping(Auction auction) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime endTime = auction.getItem().getEndTime();

        if (java.time.Duration.between(now, endTime).getSeconds() <= 30) {
            java.time.LocalDateTime newEndTime = endTime.plusSeconds(60);
            auction.getItem().setEndTime(newEndTime);

            String snipeMsg = "{\"command\":\"UPDATE_TIME\", \"message\":\"[🔥 HOT] Phiên đấu giá được gia hạn thêm 1 phút do có thầu mới!\"}";
            notifyObservers(snipeMsg);
        }
    }

    private void triggerAutoBids(Auction auction) {
        boolean autoBidTriggered = true;

        while (autoBidTriggered) {
            autoBidTriggered = false;
            double currentPrice = auction.getItem().getCurrentHighestPrice();
            Bidder currentWinner = auction.getWinner();

            List<AutoBid> eligibleBots = new ArrayList<>();
            for (AutoBid bot : auction.getAutoBids()) {
                if (currentWinner == null || !bot.getBidder().getUsername().equals(currentWinner.getUsername())) {
                    if (currentPrice + bot.getIncrement() <= bot.getMaxBid()) {
                        eligibleBots.add(bot);
                    }
                }
            }

            if (!eligibleBots.isEmpty()) {
                eligibleBots.sort(Comparator.comparing(AutoBid::getRegisterTime));
                AutoBid nextBot = eligibleBots.get(0);

                double newPrice = currentPrice + nextBot.getIncrement();
                auction.addBid(new BidTransaction(nextBot.getBidder(), newPrice));
                auction.setWinner(nextBot.getBidder());
                autoBidTriggered = true;

                String autoMsg = "{\"command\":\"UPDATE_PRICE\", \"message\":\"[AUTO-BID] Bot của " + nextBot.getBidder().getUsername() + " nâng giá lên " + newPrice + "\"}";
                notifyObservers(autoMsg);
            }
        }
    }

    public void startAuctionTimer() {
        Thread timerThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();

                    for (Auction auction : activeAuctions.values()) {
                        synchronized (auction) {
                            if (auction.getStatus() == AuctionStatus.RUNNING && now.isAfter(auction.getItem().getEndTime())) {

                                auction.setStatus(AuctionStatus.FINISHED);

                                String winnerInfo = (auction.getWinner() != null)
                                        ? "Người thắng: " + auction.getWinner().getUsername()
                                        : "Không có người thắng.";

                                String msg = "{\"command\":\"AUCTION_FINISHED\", \"message\":\"[HẾT GIỜ] " + auction.getItem().getName() + " đã kết thúc. " + winnerInfo + "\"}";
                                notifyObservers(msg);
                                System.out.println(">>> Đóng phiên: " + auction.getAuctionId());
                                
                                // Lưu kết quả vào database
                                if (auction.getWinner() != null) {
                                    orderDAO.insertOrder(
                                        auction.getItem().getId(),
                                        auction.getSeller().getUsername(),
                                        auction.getWinner().getUsername(),
                                        auction.getItem().getCurrentHighestPrice()
                                    );
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        timerThread.setDaemon(true);
        timerThread.start();
    }

    public String getFirstRunningAuctionId() {
        return activeAuctions.isEmpty() ? null : activeAuctions.keySet().iterator().next();
    }

    public Auction getAuction(String auctionId) {
        return activeAuctions.get(auctionId);
    }
}