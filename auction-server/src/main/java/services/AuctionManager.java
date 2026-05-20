// 1. Khai báo package: Nằm trong phân hệ dịch vụ nghiệp vụ (Services) của Server.
package services;

// 2. Import các mô hình (Models) dữ liệu liên quan đến cấu trúc đấu giá.
import models.Auction;
import models.AuctionStatus;
import models.BidTransaction;
import models.Bidder;
import models.AutoBid;
// 3. Import các cấu trúc dữ liệu đa luồng và tiện ích Java SE.
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import dao.OrderDAO;

/**
 * Lớp AuctionManager chịu trách nhiệm quản trị cốt lõi toàn bộ các phiên đấu giá trực tuyến.
 * Quản lý tính trạng đấu giá thời gian thực, xử lý đặt thầu an toàn đa luồng (thread-safe),
 * kích hoạt các bot đặt giá tự động, thực thi quy tắc chống bắn tỉa phút chót (anti-sniping),
 * giới hạn tần suất gửi thầu (rate limiting/throttling), và chạy ngầm một bộ giám sát thời gian kết thúc (timer thread).
 * 
 * Mẫu thiết kế & Java Concurrency áp dụng:
 * - Singleton Design Pattern: Đảm bảo chỉ tồn tại duy nhất một đối tượng điều phối đấu giá trên toàn Server.
 * - ConcurrentHashMap: Cấu trúc bản đồ chia nhỏ phân đoạn khóa (Segmented Lock) giúp đa luồng đọc/ghi 
 *   mạnh mẽ mà không bị xung đột khóa (Lock Contention) hay ConcurrentModificationException.
 * - Thread Synchronization: Sử dụng `synchronized` cục bộ trên từng thực thể Auction để khóa luồng thầu song song.
 */
public class AuctionManager {

    // 4. Khai báo biến Singleton static lưu trữ thể hiện duy nhất trong RAM Heap.
    private static AuctionManager instance;
    
    // Bản đồ lưu trữ các phiên đấu giá đang kích hoạt. Sử dụng ConcurrentHashMap để an toàn đa luồng.
    private Map<String, Auction> activeAuctions;
    
    // Dịch vụ gửi thông báo thời gian thực đến toàn bộ các socket client trực tuyến (Observer Pattern).
    private AuctionNotificationService notificationService;
    
    // Đối tượng truy cập cơ sở dữ liệu để ghi hóa đơn khi phiên kết thúc thành công.
    private OrderDAO orderDAO;
    
    // Lưu trữ thời điểm đặt giá cuối cùng của người dùng để chống Spam đặt thầu (Throttling/Rate Limiting).
    private Map<String, Long> userLastBidTime = new ConcurrentHashMap<>();
    
    // Tránh việc kích hoạt lặp đi lặp lại nhiều luồng Thread chạy Đặt thầu tự động cùng lúc trên cùng 1 phiên đấu giá (Cờ khóa luồng).
    private Map<String, Boolean> auctionAutoBidProcessing = new ConcurrentHashMap<>();
 
    /**
     * Hàm khởi tạo riêng tư (Private Constructor) để ngăn cản việc đúc đối tượng từ bên ngoài qua từ khóa `new`.
     */
    private AuctionManager() {
        activeAuctions = new ConcurrentHashMap<>();
        orderDAO = new OrderDAO();
    }

    /**
     * Lấy thể hiện duy nhất của AuctionManager (Thread-safe).
     * Sử dụng từ khóa `synchronized` ở cấp độ phương thức để đảm bảo nếu có nhiều luồng
     * khởi chạy Server đồng thời, chỉ có đúng một thực thể được đúc.
     * 
     * @return Đối tượng AuctionManager duy nhất
     */
    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }
 
    /**
     * Tiêm phụ thuộc (Dependency Injection) cho dịch vụ gửi thông tin cập nhật Socket.
     */
    public void setNotificationService(AuctionNotificationService service) {
        this.notificationService = service;
    }

    /**
     * Hủy bỏ cấu hình tự động đấu giá (Auto-bid) của một người dùng trên một sản phẩm.
     * 
     * @param auctionId Mã phiên đấu giá
     * @param username Tên tài khoản người muốn hủy bot đặt giá
     * @return true nếu tìm thấy và xóa cấu hình thành công, ngược lại false
     */
    public boolean removeAutoBid(String auctionId, String username) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) return false;
        
        // Đồng bộ hóa trên đối tượng auction để tránh xung đột dữ liệu đa luồng (ConcurrentModificationException)
        // khi vừa duyệt danh sách vừa gỡ bỏ phần tử ở luồng Thread khác.
        synchronized (auction) {
            return auction.getAutoBids().removeIf(bot -> bot.getBidder().getUsername().equals(username));
        }
    }

    /**
     * Gửi bản tin thông báo thời gian thực đến tầng Server để chuyển phát đến các Client (Observer Pattern).
     * @param message Nội dung bản tin định dạng JSON
     */
    public void notifyObservers(String message) {
        if (notificationService != null) {
            notificationService.notifyAll(message);
        }
    }

    /**
     * Khởi động một phiên đấu giá mới: chuyển trạng thái sang RUNNING và thêm vào danh sách đang theo dõi.
     * @param auction Đối tượng phiên đấu giá
     */
    public void addAuction(Auction auction) {
        auction.setStatus(AuctionStatus.RUNNING);
        activeAuctions.put(auction.getAuctionId(), auction);
    }

    /**
     * Phương thức cốt lõi xử lý ĐẶT THỦY THỦ CÔNG (Manual Bidding) của người dùng.
     * Đảm bảo tính Thread-safe tuyệt đối nhờ cơ chế đồng bộ hóa `synchronized (auction)`.
     * 
     * Quy trình xử lý (Input -> Process -> Output):
     * 1. Đầu vào (Input): Nhận mã phiên thầu, đối tượng người mua, và số tiền đặt giá.
     * 2. Xử lý (Process): 
     *    - Kiểm duyệt trạng thái hoạt động (phải là RUNNING).
     *    - Áp dụng bộ lọc chống spam đặt giá (Throttling - tối thiểu 1.5 giây giữa hai lượt nhấn của cùng một người).
     *    - So khớp giá tiền (phải lớn hơn giá cao nhất hiện tại).
     *    - Thiết lập người tạm thắng, phát sóng thông báo giá mới thời gian thực đến toàn Client.
     *    - Kích hoạt luật chống bắn tỉa phút chót (Anti-Sniping).
     *    - Kích hoạt cơ chế Bot thầu tự động.
     * 3. Đầu ra (Output): Trả về kết quả thầu thành công (true) hoặc thất bại (false).
     * 
     * @param auctionId Mã phiên đấu giá
     * @param bidder Đối tượng người đấu thầu
     * @param bidAmount Số tiền thầu đề xuất
     * @return true nếu đặt thầu hợp lệ và thành công, ngược lại false
     */
    public boolean placeBid(String auctionId, Bidder bidder, double bidAmount) {
        Auction auction = activeAuctions.get(auctionId);

        if (auction == null) {
            return false;
        }

        // Khóa đồng bộ hóa trên chính phiên đấu giá này (Object Monitor Monitor Lock):
        // Đảm bảo tại một thời điểm, chỉ có duy nhất một luồng (Thread) kết nối được thay đổi thông tin thầu
        // của phiên đấu giá này, giải quyết triệt để lỗi đua tranh dữ liệu (Race Condition).
        synchronized (auction) {
            // 1. Chỉ được đặt thầu khi phiên đang chạy
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                return false;
            }

            // 2. THROTTLING (Giới hạn tần suất đặt thầu - Rate Limiting):
            // Ngăn chặn các tool auto clicker hoặc người dùng bấm nút liên tục spam server làm sập DB.
            // Sử dụng thời gian thực tế dạng Epoch Milliseconds. Giới hạn khoảng cách mỗi lần đặt thầu là 1.5 giây.
            long nowTime = System.currentTimeMillis();
            String throttleKey = bidder.getUsername() + "_" + auctionId;
            if (userLastBidTime.containsKey(throttleKey)) {
                if (nowTime - userLastBidTime.get(throttleKey) < 1500) { // Cần cách nhau tối thiểu 1500ms
                    return false;
                }
            }
            userLastBidTime.put(throttleKey, nowTime); // Ghi nhận mốc thời gian gửi thầu mới nhất

            // 3. Kiểm tra số tiền đặt thầu phải cao hơn giá cao nhất hiện tại (Ràng buộc nghiệp vụ đấu giá tự do):
            // Đã lược bỏ gợi ý bước nhảy cố định theo yêu cầu của người dùng, cho phép đặt bất kỳ giá nào lớn hơn giá hiện hành.
            if (bidAmount <= auction.getItem().getCurrentHighestPrice()) {
                return false;
            }

            // 4. Tạo giao dịch đấu giá và ghi nhận người tạm thắng
            BidTransaction newBid = new BidTransaction(bidder, bidAmount);
            auction.addBid(newBid);
            auction.setWinner(bidder);

            System.out.println("Thành công: " + bidder.getUsername() + " đã đặt giá " + bidAmount);
            
            // 5. Phát sóng thông báo cập nhật giá mới đến tất cả các Client qua mạng (JSON broadcast)
            String manualMsg = "{\"command\":\"UPDATE_PRICE\", \"auctionId\":\"" + auction.getAuctionId() + "\", \"price\":" + bidAmount + ", \"winnerUsername\":\"" + bidder.getUsername() + "\", \"message\":\"[MANUAL] " + bidder.getUsername() + " đã đặt giá " + bidAmount + "\"}";
            notifyObservers(manualMsg);

            // 6. Xử lý cơ chế chống bắn tỉa phút chót (Anti-Sniping)
            handleAntiSniping(auction);
            
            // 7. Kích hoạt luồng kiểm tra đặt thầu tự động của các Bot đã đăng ký
            triggerAutoBids(auction);

            return true;
        }
    }

    /**
     * Cơ chế CHỐNG BẮN TỈA PHÚT CHÓT (Anti-Sniping).
     * Nếu xuất hiện bất kỳ lượt đặt thầu hợp lệ nào trong vòng 30 giây cuối cùng trước khi hết giờ,
     * tự động gia hạn thời gian kết thúc phiên đấu giá thêm 60 giây để đảm bảo tính công bằng,
     * ngăn chặn các phần mềm tự động thầu cướp sản phẩm ở mili-giây cuối mà người dùng khác không kịp phản ứng.
     * 
     * @param auction Phiên đấu giá đang được kiểm duyệt
     */
    private void handleAntiSniping(Auction auction) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime endTime = auction.getItem().getEndTime();

        // Kiểm tra xem khoảng cách đến thời điểm kết thúc có nhỏ hơn hoặc bằng 30 giây hay không
        if (java.time.Duration.between(now, endTime).getSeconds() <= 30) {
            java.time.LocalDateTime newEndTime = endTime.plusSeconds(60); // Cộng thêm 1 phút (60s) vào thời gian kết thúc
            auction.getItem().setEndTime(newEndTime);

            // Phát thông báo thời gian thực đến Client để cập nhật đồng hồ đếm ngược trên giao diện
            String snipeMsg = "{\"command\":\"UPDATE_TIME\", \"newEndTime\":\"" + newEndTime.toString() + "\", \"message\":\"[🔥 HOT] Phiên đấu giá được gia hạn thêm 1 phút do có thầu mới!\"}";
            notifyObservers(snipeMsg);
        }
    }

    /**
     * Kiểm tra và kích hoạt bot đặt thầu tự động theo mã đấu giá (ngoài luồng).
     */
    public void checkAndTriggerAutoBids(String auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null && auction.getStatus() == AuctionStatus.RUNNING) {
            synchronized (auction) {
                triggerAutoBids(auction);
            }
        }
    }

    /**
     * Xử lý ĐẶT THẦU TỰ ĐỘNG (Auto-Bidding Engine) bằng cách khởi chạy luồng bất đồng bộ ngầm (Background Async Thread).
     * Cơ chế chạy vòng lặp cạnh tranh giữa các Bot cho đến khi không còn ai đủ điều kiện nâng giá.
     * Phân xử thứ tự nâng giá ưu tiên dựa trên thời điểm đăng ký bot (FIFO - đăng ký trước được ưu tiên nâng giá trước).
     * 
     * Kỹ thuật & Nghiệp vụ Auto-Bid:
     * - Chạy đa luồng bất đồng bộ: `new Thread(...)` giúp quá trình robot tự động nâng giá liên tục
     *   không gây tắc nghẽn giao tiếp mạng chính (Socket I/O thread).
     * - Cờ khóa luồng (`auctionAutoBidProcessing`): Đảm bảo tại một thời điểm trên cùng một sản phẩm,
     *   chỉ có đúng một tiến trình Bot đấu giá tự động chạy ngầm.
     * - Độ trễ giả lập hành vi người dùng: Ngủ ngẫu nhiên từ 1.5s - 2.5s để tạo cảm giác tự nhiên như có người nâng thầu thật.
     * - FIFO Sắp xếp Bot: Nếu nhiều Bot cùng đủ điều kiện, Bot đăng ký trước sẽ nâng giá trước.
     * - Ràng buộc giá trị bước nhảy tự do: Mỗi bot sở hữu một bước giá nhảy độc lập tùy cấu hình từ người dùng.
     * 
     * @param auction Phiên đấu giá chạy động cơ bot
     */
    public void triggerAutoBids(Auction auction) {
        String auctionId = auction.getAuctionId();
        
        // Cơ chế khóa cờ: Tránh việc tạo quá nhiều luồng Thread cạnh tranh xử lý lồng nhau cùng một lúc trên một sản phẩm
        if (auctionAutoBidProcessing.getOrDefault(auctionId, false)) {
            return;
        }

        // Khởi động luồng chạy ngầm để không gây tắc nghẽn (block) luồng xử lý mạng socket chính
        new Thread(() -> {
            auctionAutoBidProcessing.put(auctionId, true);
            try {
                boolean autoBidTriggered = true;
                java.util.Random random = new java.util.Random();

                // Chạy vòng lặp thầu tự động liên tục cho đến khi cờ dừng
                while (autoBidTriggered) {
                    // Tạo một khoảng trễ ngẫu nhiên từ 1.5 giây đến 2.5 giây.
                    // Khoảng trễ mô phỏng hành vi suy nghĩ của con người, tránh việc bot nâng giá lập tức trong 0ms.
                    int delay = 1500 + random.nextInt(1001);
                    Thread.sleep(delay);

                    // Khóa đồng bộ phiên đấu giá ở mỗi bước đấu thầu tự động để đảm bảo tính an toàn dữ liệu
                    synchronized (auction) {
                        if (auction.getStatus() != AuctionStatus.RUNNING) {
                            autoBidTriggered = false;
                            break;
                        }

                        autoBidTriggered = false;
                        double currentPrice = auction.getItem().getCurrentHighestPrice();
                        Bidder currentWinner = auction.getWinner();

                        // 1. Tìm các bot đủ điều kiện nâng thầu
                        // Bot hợp lệ là bot không phải của người đang giữ giá cao nhất hiện tại,
                        // và số tiền đấu giá tiếp theo (giá hiện tại + bước giá thầu cấu hình của bot) không vượt quá giới hạn tối đa mà chủ bot thiết lập.
                        List<AutoBid> eligibleBots = new ArrayList<>();
                        for (AutoBid bot : auction.getAutoBids()) {
                            if (currentWinner == null || !bot.getBidder().getUsername().equals(currentWinner.getUsername())) {
                                if (currentPrice + bot.getIncrement() <= bot.getMaxBid()) {
                                    eligibleBots.add(bot);
                                }
                            }
                        }

                        // 2. Phân xử nâng giá nếu có bot đủ điều kiện (Áp dụng FIFO)
                        if (!eligibleBots.isEmpty()) {
                            // Sắp xếp các bot theo thứ tự thời điểm đăng ký (FIFO - Đăng ký trước được ưu tiên nâng giá trước)
                            eligibleBots.sort(Comparator.comparing(AutoBid::getRegisterTime));
                            AutoBid nextBot = eligibleBots.get(0);

                            // Tiến hành nâng giá tự động
                            double newPrice = currentPrice + nextBot.getIncrement();
                            auction.addBid(new BidTransaction(nextBot.getBidder(), newPrice));
                            auction.setWinner(nextBot.getBidder());
                            autoBidTriggered = true; // Tiếp tục vòng lặp để các bot khác có cơ hội nâng thầu tiếp

                            // Phát sóng thông báo thầu tự động đến các Client
                            String autoMsg = "{\"command\":\"UPDATE_PRICE\", \"auctionId\":\"" + auction.getAuctionId() + "\", \"price\":" + newPrice + ", \"winnerUsername\":\"" + nextBot.getBidder().getUsername() + "\", \"message\":\"[AUTO-BID] Bot của " + nextBot.getBidder().getUsername() + " nâng giá lên " + newPrice + "\"}";
                            notifyObservers(autoMsg);
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                // Nhả cờ xử lý sau khi hoàn thành chuỗi thầu tự động
                auctionAutoBidProcessing.put(auctionId, false);
            }
        }).start();
    }

    /**
     * Khởi động BỘ ĐỊNH THÌ GIÁM SÁT ĐÓNG PHIÊN (Background Daemon Timer Thread).
     * Luồng chạy ngầm liên tục mỗi 1 giây quét qua toàn bộ danh sách các phiên đấu giá đang mở.
     * Tự động kết thúc phiên, cập nhật trạng thái FINISHED và lưu hóa đơn mua bán vào SQLite 
     * nếu thời gian thực tế vượt quá mốc endTime của sản phẩm.
     * 
     * Kỹ thuật Java Daemon Thread:
     * - `timerThread.setDaemon(true)`: Đánh dấu luồng định thì chạy ngầm này là một Daemon Thread.
     *   Ý nghĩa: Khi luồng chính (Main Thread) của Server bị tắt, JVM sẽ tự động chấm dứt Daemon Thread này ngay lập tức
     *   mà không cần chờ nó kết thúc vòng lặp vô tận, ngăn ngừa lỗi "Treo Server ngầm" chiếm dụng CPU.
     */
    public void startAuctionTimer() {
        Thread timerThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000); // Ngủ 1 giây mỗi chu kỳ quét
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();

                    for (Auction auction : activeAuctions.values()) {
                        synchronized (auction) {
                            // Nếu phiên đang chạy và thời gian hiện tại đã vượt quá thời gian kết thúc thầu
                            if (auction.getStatus() == AuctionStatus.RUNNING && now.isAfter(auction.getItem().getEndTime())) {

                                // Chuyển trạng thái phiên sang KẾT THÚC
                                if (auction.getWinner() != null) {
                                    auction.setStatus(AuctionStatus.ENDED_WITH_WINNER);
                                } else {
                                    auction.setStatus(AuctionStatus.ENDED_NO_WINNER);
                                }

                                String winnerInfo = (auction.getWinner() != null)
                                        ? "Người thắng: " + auction.getWinner().getUsername()
                                        : "Không có người thắng.";

                                // Phát bản tin kết thúc đấu giá
                                String msg = "{\"command\":\"AUCTION_FINISHED\", \"message\":\"[HẾT GIỜ] " + auction.getItem().getName() + " đã kết thúc. " + winnerInfo + "\"}";
                                notifyObservers(msg);
                                 notifyObservers("{\"command\":\"UPDATE_ITEMS\"}");
                                System.out.println(">>> Đóng phiên: " + auction.getAuctionId());
                                
                                // Ghi hóa đơn mua bán thành công xuống Database để người bán thống kê doanh thu (Persistent Order Data)
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
        timerThread.setDaemon(true); // Thiết lập luồng Daemon để tự động đóng khi Server dừng hoạt động
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
     * Tìm kiếm một phiên đấu giá dựa trên mã định danh thực thể của sản phẩm (Item ID).
     * Duyệt qua toàn bộ tập phiên đấu giá active để đối chiếu.
     * @param itemId Mã ID sản phẩm
     * @return Phiên đấu giá tương ứng, hoặc null nếu không tồn tại
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

    /**
     * Thực hiện thanh toán tất cả sản phẩm đã thắng thầu cho người dùng.
     */
    public boolean payWinnings(String username) {
        boolean paidAny = false;
        for (Auction auction : activeAuctions.values()) {
            synchronized (auction) {
                if (auction.getStatus() == AuctionStatus.ENDED_WITH_WINNER &&
                        auction.getWinner() != null &&
                        username.equals(auction.getWinner().getUsername())) {
                    
                    auction.setStatus(AuctionStatus.FINISHED);
                    orderDAO.markOrderAsCompletedByItemId(auction.getItem().getId());
                    paidAny = true;
                }
            }
        }
        
        boolean dbUpdated = orderDAO.markAllOrdersAsFinishedByBidderName(username);
        return paidAny || dbUpdated;
    }
}