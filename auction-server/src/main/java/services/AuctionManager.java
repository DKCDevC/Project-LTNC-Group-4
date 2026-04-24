package services;

import dao.ItemDAO;
import models.Auction;
import models.AuctionStatus;
import models.AutoBid;
import models.BidTransaction;
import models.Bidder;

import com.google.gson.JsonObject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Central auction engine.
 *
 * Concurrency guarantees:
 *  - One ReentrantLock per auction -> fine-grained locking (no global bottleneck).
 *  - Per-user bid cooldown (500 ms) to prevent spam.
 *  - Anti-sniping limited to MAX_EXTENSIONS (5) OR a hard cutoff time.
 *  - Auto-bid uses eBay-style proxy bidding with a PriorityQueue.
 *  - All bids are persisted to DB atomically inside the lock.
 */
public class AuctionManager {

    // ----------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------
    private static final int    MAX_EXTENSIONS    = 5;          // anti-snipe limit
    private static final long   SNIPE_WINDOW_SEC  = 30;        // trigger if bid within last N s
    private static final long   EXTEND_SECS       = 60;        // how many seconds to add
    private static final long   MAX_EXTRA_MINUTES = 10;        // hard cutoff: original end + 10 min
    private static final long   BID_COOLDOWN_MS   = 500;       // min ms between bids per user
    private static final int    MAX_CHART_POINTS  = 50;        // chart history limit sent to clients

    // ----------------------------------------------------------------
    // State
    // ----------------------------------------------------------------
    private static AuctionManager instance;

    /** auctionId ??????????????????????????????? Auction */
    private final Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();

    /** auctionId ??????????????????????????????? ReentrantLock (created lazily) */
    private final Map<String, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    /** auctionId ??????????????????????????????? extension count */
    private final Map<String, Integer> extensionCount = new ConcurrentHashMap<>();

    /** auctionId ??????????????????????????????? original endTime (before any extensions) */
    private final Map<String, LocalDateTime> originalEndTimes = new ConcurrentHashMap<>();

    /** username ??????????????????????????????? last bid timestamp (ms) for cooldown enforcement */
    private final Map<String, Long> lastBidTime = new ConcurrentHashMap<>();

    /** username ??????????????????????????????? Set of item IDs */
    private final Map<String, Set<String>> userWatchlist = new ConcurrentHashMap<>();

    /** itemId ??????????????????????????????? Set of usernames who already received near-expiry alert */
    private final Map<String, Set<String>> sentExpiryAlerts = new ConcurrentHashMap<>();

    private final ItemDAO itemDAO = new ItemDAO();

    // ----------------------------------------------------------------
    // Singleton
    // ----------------------------------------------------------------
    private AuctionManager() {}

    public static synchronized AuctionManager getInstance() {
        if (instance == null) instance = new AuctionManager();
        return instance;
    }

    // ----------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------

    public void addAuction(Auction auction) {
        auction.setStatus(AuctionStatus.RUNNING);
        activeAuctions.put(auction.getAuctionId(), auction);
        auctionLocks.put(auction.getAuctionId(), new ReentrantLock(true));
        extensionCount.put(auction.getAuctionId(), 0);
        originalEndTimes.put(auction.getAuctionId(), auction.getItem().getEndTime());
    }

    public String getFirstRunningAuctionId() {
        for (Map.Entry<String, Auction> e : activeAuctions.entrySet()) {
            if (e.getValue().getStatus() == AuctionStatus.RUNNING) return e.getKey();
        }
        return null;
    }

    /** Return auction by ID, or null. */
    public Auction getAuction(String auctionId) {
        return activeAuctions.get(auctionId);
    }

    // ----------------------------------------------------------------
    // BID ???????????????????????????? main entry point
    // ----------------------------------------------------------------

    /**
     * Place a bid on behalf of {@code bidder}.
     *
     * @param auctionId  target auction
     * @param bidder     verified bidder (session-checked by ClientHandler)
     * @param bidAmount  requested amount
     * @param itemId     DB item ID (for persistence)
     * @return BidResult with success flag and message
     */
    public BidResult placeBid(String auctionId, Bidder bidder, double bidAmount, String itemId) {

        // ?????????????????????????????????????????????????? Spam / rate-limit check (outside lock for performance) ??????????????????????????????????????????????????
        long now = System.currentTimeMillis();
        Long last = lastBidTime.get(bidder.getUsername());
        if (last != null && (now - last) < BID_COOLDOWN_MS) {
            return BidResult.fail("Vui lòng chờ trước khi đặt giá tiếp theo.");
        }

        ReentrantLock lock = auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock(true));
        lock.lock();
        try {
            Auction auction = activeAuctions.get(auctionId);
            if (auction == null) return BidResult.fail("Phiên đấu giá không tồn tại.");

            // ?????????????????????????????????????????????????? Auction state validation ??????????????????????????????????????????????????
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                return BidResult.fail("Phiên đấu giá đã đóng.");
            }
            if (LocalDateTime.now().isAfter(auction.getItem().getEndTime())) {
                auction.setStatus(AuctionStatus.FINISHED);
                return BidResult.fail("Phiên đấu giá đã hết thời gian.");
            }

            // ?????????????????????????????????????????????????? Price validation with server-controlled increment ??????????????????????????????????????????????????
            double current = auction.getItem().getCurrentHighestPrice();
            double minIncr = auction.getItem().getMinIncrement();
            // Server can enforce a global minimum if needed, e.g. 500
            minIncr = Math.max(minIncr, 500.0); 
            
            double minNext = current + minIncr;
            if (bidAmount < minNext) {
                return BidResult.fail("Giá đấu tối thiểu tiếp theo là " + String.format("%,.0f", minNext) + " ₫");
            }

            // ?????????????????????????????????????????????????? Place bid ??????????????????????????????????????????????????
            Bidder prevWinner = auction.getWinner();
            lastBidTime.put(bidder.getUsername(), now);
            BidTransaction tx = new BidTransaction(bidder, bidAmount);
            auction.addBid(tx);
            auction.setWinner(bidder);

            // Persist to DB inside lock for atomicity
            if (itemId != null) itemDAO.saveBid(itemId, bidder.getUsername(), bidAmount);

            // ?????????????????????????????????????????????????? Notify Outbid ??????????????????????????????????????????????????
            if (prevWinner != null && !prevWinner.getUsername().equals(bidder.getUsername())) {
                sendNotify(prevWinner.getUsername(), "OUTBID", "Bạn đã bị vượt mặt ở sản phẩm: " + auction.getItem().getName(), itemId);
            }

            System.out.println(">>> BID OK: " + bidder.getUsername() + " -> " + bidAmount);

            // ?????????????????????????????????????????????????? Anti-sniping ??????????????????????????????????????????????????
            handleAntiSniping(auctionId, auction);

            // ?????????????????????????????????????????????????? Auto-bid cascade ??????????????????????????????????????????????????
            triggerAutoBids(auctionId, auction, itemId);

            return BidResult.success(auction.getItem().getCurrentHighestPrice(),
                    auction.getItem().getEndTime());

        } finally {
            lock.unlock();
        }
    }

    // ----------------------------------------------------------------
    // REGISTER AUTO-BID
    // ----------------------------------------------------------------

    /**
     * Registers an auto-bid proxy for {@code bidder}.
     * Immediately triggers the auto-bid cascade.
     */
    public BidResult registerAutoBid(String auctionId, Bidder bidder,
                                     double maxBid, double increment, String itemId) {
        ReentrantLock lock = auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock(true));
        lock.lock();
        try {
            Auction auction = activeAuctions.get(auctionId);
            if (auction == null) return BidResult.fail("Phiên không tồn tại.");
            if (auction.getStatus() != AuctionStatus.RUNNING)
                return BidResult.fail("Phiên đã đóng.");

            double current = auction.getItem().getCurrentHighestPrice();
            if (maxBid <= current) return BidResult.fail("Giá tối đa phải cao hơn giá hiện tại.");

            // Remove old auto-bid from same user if any
            auction.getAutoBids().removeIf(ab -> ab.getBidder().getUsername().equals(bidder.getUsername()));
            auction.registerAutoBid(new AutoBid(bidder, maxBid, increment));

            triggerAutoBids(auctionId, auction, itemId);

            return BidResult.success(auction.getItem().getCurrentHighestPrice(),
                    auction.getItem().getEndTime());
        } finally {
            lock.unlock();
        }
    }

    // ----------------------------------------------------------------
    // Anti-sniping (called inside lock)
    // ----------------------------------------------------------------

    private void handleAntiSniping(String auctionId, Auction auction) {
        LocalDateTime endTime = auction.getItem().getEndTime();
        LocalDateTime nowDT   = LocalDateTime.now();
        long secsLeft = Duration.between(nowDT, endTime).getSeconds();

        if (secsLeft > SNIPE_WINDOW_SEC) return; // outside snipe window

        int extensions = extensionCount.getOrDefault(auctionId, 0);
        LocalDateTime original = originalEndTimes.getOrDefault(auctionId, endTime);
        LocalDateTime cutoff   = original.plusMinutes(MAX_EXTRA_MINUTES);

        // Hard cutoff OR max extension count reached ??????????????????????????????? no more extensions
        if (extensions >= MAX_EXTENSIONS || !nowDT.isBefore(cutoff)) {
            System.out.println(">>> Anti-snipe BLOCKED (ext=" + extensions + ", cutoff=" + cutoff + ")");
            return;
        }

        LocalDateTime newEnd = endTime.plusSeconds(EXTEND_SECS);
        auction.getItem().setEndTime(newEnd);
        extensionCount.put(auctionId, extensions + 1);

        String msg = "{\"command\":\"UPDATE_TIME\",\"itemId\":\"" + auctionId
                + "\",\"newEndTime\":\"" + newEnd
                + "\",\"message\":\"[HOT] Phiên đấu giá gia hạn thêm 60s (lần " + (extensions + 1) + "/" + MAX_EXTENSIONS + ")!\"}";
        network.ClientHandler.notifyAllObservers(msg);
        System.out.println(">>> Anti-snipe extension " + (extensions + 1) + "/" + MAX_EXTENSIONS);
    }

    // ----------------------------------------------------------------
    // Auto-bid cascade (eBay proxy logic, called inside lock)
    // ----------------------------------------------------------------

    private void triggerAutoBids(String auctionId, Auction auction, String itemId) {
        // Build a max-heap: highest maxBid first; on tie, earliest register time first
        PriorityQueue<AutoBid> pq = new PriorityQueue<>(
            Comparator.comparingDouble(AutoBid::getMaxBid).reversed()
                      .thenComparing(AutoBid::getRegisterTime)
        );
        pq.addAll(auction.getAutoBids());

        boolean changed = true;
        while (changed) {
            changed = false;
            double current  = auction.getItem().getCurrentHighestPrice();
            Bidder winner   = auction.getWinner();

            // Find the top eligible auto-bidder (not the current winner)
            AutoBid best = null;
            for (AutoBid ab : pq) {
                if (winner == null || !ab.getBidder().getUsername().equals(winner.getUsername())) {
                    // Enforce server rules on increment
                    double effIncr = Math.max(ab.getIncrement(), auction.getItem().getMinIncrement());
                    effIncr = Math.max(effIncr, 500.0); // Server minimum floor

                    double needed = current + effIncr;
                    if (ab.getMaxBid() >= needed) {
                        best = ab;
                        break;
                    }
                }
            }

            if (best == null) break;

            // eBay rule: winner bids just enough to beat second-highest+increment
            double newPrice = current + best.getIncrement();
            // Cap at best's maxBid
            if (newPrice > best.getMaxBid()) newPrice = best.getMaxBid();

            BidTransaction autoTx = new BidTransaction(best.getBidder(), newPrice);
            auction.addBid(autoTx);
            auction.setWinner(best.getBidder());
            if (itemId != null) itemDAO.saveBid(itemId, best.getBidder().getUsername(), newPrice);

            String autoMsg = "{\"command\":\"UPDATE_PRICE\",\"itemId\":\"" + auctionId
                    + "\",\"price\":" + newPrice
                    + ",\"winner\":\"" + best.getBidder().getUsername()
                    + "\",\"message\":\"[AUTO-BID] " + best.getBidder().getUsername()
                    + " tự động đặt " + String.format("%,.0f", newPrice) + " ₫\"}";
            network.ClientHandler.notifyAllObservers(autoMsg);

            changed = true;
        }
    }

    // ----------------------------------------------------------------
    // Auction timer ???????????????????????????? runs in background daemon thread
    // ----------------------------------------------------------------

    public void startAuctionTimer() {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    LocalDateTime now = LocalDateTime.now();
                    for (Map.Entry<String, Auction> entry : activeAuctions.entrySet()) {
                        Auction auction = entry.getValue();
                        if (auction.getStatus() == AuctionStatus.RUNNING
                                && now.isAfter(auction.getItem().getEndTime())) {

                            ReentrantLock lock = auctionLocks.computeIfAbsent(
                                    entry.getKey(), k -> new ReentrantLock(true));
                            lock.lock();
                            try {
                                // Re-check inside lock
                                if (auction.getStatus() == AuctionStatus.RUNNING
                                        && now.isAfter(auction.getItem().getEndTime())) {
                                    auction.setStatus(AuctionStatus.FINISHED);
                                    
                                    double finalPrice = auction.getItem().getCurrentHighestPrice();
                                    double reserve = auction.getItem().getReservePrice();
                                    boolean reserveMet = finalPrice >= reserve;
                                    
                                    String winner = (auction.getWinner() != null && reserveMet)
                                            ? auction.getWinner().getUsername() : "Không có";
                                    
                                    String resultMsg = reserveMet ? "đã kết thúc" : "kết thúc (không đạt giá sàn)";
                                    String msg = "{\"command\":\"AUCTION_FINISHED\",\"itemId\":\""
                                            + entry.getKey() + "\",\"winner\":\"" + winner
                                            + "\",\"message\":\"[HẾT GIỜ] "
                                            + auction.getItem().getName()
                                            + " " + resultMsg + ". Người thắng: " + winner + "\"}";
                                    network.ClientHandler.notifyAllObservers(msg);
                                    System.out.println(">>> Phiên đóng: " + entry.getKey()
                                            + " | Winner: " + winner);
                                } else if (auction.getStatus() == AuctionStatus.RUNNING) {
                                    // Near expiry check (e.g. 5 minutes)
                                    long secsLeft = Duration.between(now, auction.getItem().getEndTime()).getSeconds();
                                    if (secsLeft > 0 && secsLeft <= 300) { // 5 mins
                                        notifyWatchersNearExpiry(entry.getKey(), auction.getItem().getName());
                                    }
                                }
                            } finally {
                                lock.unlock();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void notifyWatchersNearExpiry(String itemId, String itemName) {
        Set<String> alerted = sentExpiryAlerts.computeIfAbsent(itemId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
        for (Map.Entry<String, Set<String>> entry : userWatchlist.entrySet()) {
            String username = entry.getKey();
            if (entry.getValue().contains(itemId) && !alerted.contains(username)) {
                sendNotify(username, "EXPIRY", "Sản phẩm bạn đang theo dõi sắp hết thời gian: " + itemName, itemId);
                alerted.add(username);
            }
        }
    }

    private void sendNotify(String username, String type, String msg, String itemId) {
        JsonObject json = new JsonObject();
        json.addProperty("command", "NOTIFY");
        json.addProperty("type", type);
        json.addProperty("message", msg);
        json.addProperty("itemId", itemId);
        json.addProperty("time", LocalDateTime.now().toString());
        network.ClientHandler.sendToUser(username, json.toString());
    }

    public void addToWatchlist(String username, String itemId) {
        userWatchlist.computeIfAbsent(username, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(itemId);
    }

    public void removeFromWatchlist(String username, String itemId) {
        Set<String> list = userWatchlist.get(username);
        if (list != null) list.remove(itemId);
    }

    public Set<String> getWatchlist(String username) {
        return userWatchlist.getOrDefault(username, Collections.emptySet());
    }

    // ----------------------------------------------------------------
    // Inner result class
    // ----------------------------------------------------------------

    public static class BidResult {
        public final boolean success;
        public final String message;
        public final double newPrice;
        public final LocalDateTime newEndTime;

        private BidResult(boolean s, String m, double p, LocalDateTime e) {
            success = s; message = m; newPrice = p; newEndTime = e;
        }

        public static BidResult success(double price, LocalDateTime end) {
            return new BidResult(true, "OK", price, end);
        }
        public static BidResult fail(String reason) {
            return new BidResult(false, reason, 0, null);
        }
    }
}
