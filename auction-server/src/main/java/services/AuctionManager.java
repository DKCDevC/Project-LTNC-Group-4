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
    private Map<String, Long> userLastBidTime = new ConcurrentHashMap<>();
    private Map<String, Boolean> auctionAutoBidProcessing = new ConcurrentHashMap<>();

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

    public boolean removeAutoBid(String auctionId, String username) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) return false;
        
        synchronized (auction) {
            return auction.getAutoBids().removeIf(bot -> bot.getBidder().getUsername().equals(username));
        }
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

            // --- THROTTLING: Giới hạn 1 giây mỗi lần đặt thầu ---
            long nowTime = System.currentTimeMillis();
            String throttleKey = bidder.getUsername() + "_" + auctionId;
            if (userLastBidTime.containsKey(throttleKey)) {
                if (nowTime - userLastBidTime.get(throttleKey) < 1500) { // 1.5 giây
                    return false;
                }
            }
            userLastBidTime.put(throttleKey, nowTime);

            if (bidAmount <= auction.getItem().getCurrentHighestPrice()) {
                return false;
            }

            BidTransaction newBid = new BidTransaction(bidder, bidAmount);
            auction.addBid(newBid);
            auction.setWinner(bidder);

            System.out.println("Thành công: " + bidder.getUsername() + " đã đặt giá " + bidAmount);
            
            String manualMsg = "{\"command\":\"UPDATE_PRICE\", \"auctionId\":\"" + auction.getAuctionId() + "\", \"price\":" + bidAmount + ", \"winnerUsername\":\"" + bidder.getUsername() + "\", \"message\":\"[MANUAL] " + bidder.getUsername() + " đã đặt giá " + bidAmount + "\"}";
            notifyObservers(manualMsg);

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

            String snipeMsg = "{\"command\":\"UPDATE_TIME\", \"newEndTime\":\"" + newEndTime.toString() + "\", \"message\":\"[🔥 HOT] Phiên đấu giá được gia hạn thêm 1 phút do có thầu mới!\"}";
            notifyObservers(snipeMsg);
        }
    }

    public void checkAndTriggerAutoBids(String auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null && auction.getStatus() == AuctionStatus.RUNNING) {
            synchronized (auction) {
                triggerAutoBids(auction);
            }
        }
    }

    public void triggerAutoBids(Auction auction) {
        String auctionId = auction.getAuctionId();
        
        // Tránh chạy nhiều luồng autobid cùng lúc cho 1 sản phẩm
        if (auctionAutoBidProcessing.getOrDefault(auctionId, false)) {
            return;
        }

        new Thread(() -> {
            auctionAutoBidProcessing.put(auctionId, true);
            try {
                boolean autoBidTriggered = true;
                java.util.Random random = new java.util.Random();

                while (autoBidTriggered) {
                    // Delay ngẫu nhiên từ 1.5s - 2.5s để trông "người" hơn
                    int delay = 1500 + random.nextInt(1001);
                    Thread.sleep(delay);

                    synchronized (auction) {
                        if (auction.getStatus() != AuctionStatus.RUNNING) {
                            autoBidTriggered = false;
                            break;
                        }

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

                            String autoMsg = "{\"command\":\"UPDATE_PRICE\", \"auctionId\":\"" + auction.getAuctionId() + "\", \"price\":" + newPrice + ", \"winnerUsername\":\"" + nextBot.getBidder().getUsername() + "\", \"message\":\"[AUTO-BID] Bot của " + nextBot.getBidder().getUsername() + " nâng giá lên " + newPrice + "\"}";
                            notifyObservers(autoMsg);
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                auctionAutoBidProcessing.put(auctionId, false);
            }
        }).start();
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

    public List<Auction> getAllActiveAuctions() {
        return new ArrayList<>(activeAuctions.values());
    }
    
    public java.util.Map<String, Auction> getAllActiveAuctionsMap() {
        return activeAuctions;
    }

    /**
     * Tìm auction bằng itemId (Entity ID của Item).
     * Duyệt qua tất cả auction đang active để tìm item có ID khớp.
     */
    public Auction getAuctionByItemId(String itemId) {
        for (Auction auction : activeAuctions.values()) {
            if (auction.getItem() != null && auction.getItem().getId() != null
                    && auction.getItem().getId().equals(itemId)) {
                return auction;
            }
        }
        return null;
    }
}