package controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import models.Item;
import javafx.beans.property.SimpleStringProperty;
import javafx.animation.KeyValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.event.Event;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * Lớp DashboardController điều khiển toàn bộ giao diện Mua hàng / Đấu giá chính của khách hàng (Dashboard.fxml).
 * Kế thừa các nguyên lý kiến trúc lập trình JavaFX và Client-Server chuyên nghiệp.
 * 
 * Các nguyên lý kỹ thuật cốt lõi được áp dụng:
 * 1. MVC Presentation Controller: Định nghĩa ánh xạ FXML `@FXML` tương ứng với bố cục View, 
 *    nhận tương tác và cập nhật trạng thái các thẻ sản phẩm đồ họa.
 * 2. JavaFX Single-Threaded Rule (Quy tắc đơn luồng JavaFX): Mọi tác vụ vẽ đồ họa bắt buộc phải được gọi 
 *    trên luồng chính JavaFX Application Thread. Nếu luồng Client Socket nhận tin mới từ Server, 
 *    phải đóng gói tác vụ qua `Platform.runLater` để đẩy về luồng chính cập nhật an toàn.
 * 3. Infinite Scroll / Lazy Loading (Cuộn vô tận & nạp trễ): Lắng nghe thuộc tính `vvalueProperty` 
 *    của ScrollPane để tải thêm thẻ sản phẩm khi cuộn quá 90% chiều cao trang, tối ưu dung lượng bộ nhớ.
 * 4. Responsive Grid Flow: Lắng nghe sự thay đổi độ rộng `widthProperty` của FlowPane để tự động tính toán 
 *    số cột card sản phẩm hiển thị (4, 3, 2, 1) giúp giao diện tương thích tốt với mọi độ phân giải.
 * 5. Anti-Sniping & Auto-Bid Integration: Ràng buộc nghiệp vụ thầu tự động tối thiểu 10k VND và cập nhật 
 *    đồng hồ đếm ngược Countdown, đồ thị biến động giá LineChart thời gian thực.
 */
public class DashboardController {

    // --- Khung Tiêu đề & Sidebar ---
    @FXML private Label lblGreeting;
    @FXML private TextField txtSearch;
    @FXML private Label sideBrowse;

    // --- Khung Layout chính (StackPane chuyển trang) ---
    @FXML private StackPane contentArea;
    @FXML private VBox pageBrowse;
    @FXML private ScrollPane scrollPaneBrowse;
    @FXML private Button btnBackToTop;
    @FXML private VBox pageHistory;
    @FXML private VBox pageWonItems;

    @FXML private Label sideHistory;
    @FXML private Label sideWonItems;

    // --- Bảng Lịch sử tham gia đấu thầu ---
    @FXML private TableView<ItemUI> tableHistory;
    @FXML private TableColumn<ItemUI, String> colHistName;
    @FXML private TableColumn<ItemUI, String> colHistPrice;
    @FXML private TableColumn<ItemUI, Integer> colHistBids;
    @FXML private TableColumn<ItemUI, String> colHistStatus;

    // --- Bảng danh sách sản phẩm thắng cuộc (Chờ thanh toán) ---
    @FXML private TableView<ItemUI> tableWon;
    @FXML private TableColumn<ItemUI, String> colWonName;
    @FXML private TableColumn<ItemUI, String> colWonPrice;
    @FXML private TableColumn<ItemUI, String> colWonSeller;
    @FXML private TableColumn<ItemUI, String> colWonDate;
    @FXML private TableColumn<ItemUI, String> colWonStatus;

    // --- Khối lưới sản phẩm Trang mua sắm ---
    @FXML private Label lblCategoryTitle;
    @FXML private FlowPane gridItems;
    @FXML private ComboBox<String> cboFilterCondition;
    @FXML private ComboBox<String> cboFilterPrice;
    @FXML private ComboBox<String> cboFilterRating;

    // --- Màn hình Chi tiết Sản phẩm & Đấu thầu thời gian thực ---
    @FXML private ScrollPane pageProductDetail;
    @FXML private VBox pageMiniCart;
    @FXML private VBox vboxMiniCartItems;
    @FXML private Label lblMiniCartTotal;
    @FXML private Button btnQuickPay;
    @FXML private Button btnPayAllWon;
    @FXML private Label lblDetailName;
    @FXML private Label lblDetailPrice;
    @FXML private Label lblDetailBidsCount;
    @FXML private Label lblDetailStatus;
    @FXML private TextField txtBidAmount;
    @FXML private TextField txtAutoBidMax;
    @FXML private Button btnPlaceBid;
    @FXML private HBox hboxQuickBids;
    
    // Biểu đồ theo dõi giá biến động theo thời gian thực
    @FXML private LineChart<String, Number> priceChart;
    private XYChart.Series<String, Number> priceSeries;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // Đồng hồ đếm ngược và luồng Timeline tương ứng
    @FXML private Label lblCountdown;
    private Timeline countdownTimeline;
    private Timeline chartUpdateTimeline;
    private ItemUI selectedProductForDetail;

    // Khối điều khiển Auto Bid trên giao diện
    @FXML private VBox vboxAutoBidSetup;
    @FXML private VBox vboxAutoBidActive;
    @FXML private Label lblActiveAutoBidMax;
    @FXML private Label lblBidStatus;
    @FXML private TextField txtAutoBidIncrement;
    
    // --- Các đối tượng giao diện màn hình Checkout (chuẩn eBay) ---
    @FXML private ScrollPane pageCheckout;
    @FXML private VBox vboxCheckoutItems;
    @FXML private Label lblCheckoutSubtotal;
    @FXML private Label lblCheckoutTax;
    @FXML private Label lblCheckoutTotal;
    @FXML private Label lblShipName;
    @FXML private Label lblShipPhone;
    @FXML private Label lblShipAddress;
    @FXML private RadioButton radCreditCard;
    @FXML private RadioButton radCOD;
    
    // Lưu trữ danh sách Auto Bid đang hoạt động của người dùng (auctionId -> Mức giá tối đa)
    private java.util.Map<String, Double> activeAutoBids = new java.util.HashMap<>();
    
    // Quy định nghiệp vụ: Bước giá thầu tự động tối thiểu là 10,000đ
    private static final double MIN_INCREMENT = 10000.0;

    // --- Nhãn bộ lọc danh mục sản phẩm ở thanh Sidebar ---
    @FXML private Label catAll;
    @FXML private Label catElectronics;
    @FXML private Label catArt;
    @FXML private Label catVehicle;
    @FXML private Label catGeneral;

    // Các biến luồng kết nối Socket máy chủ
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private String currentUsername;

    // Danh sách gốc và danh sách đã lọc cục bộ phục vụ phân trang
    private List<ItemUI> allItems = new ArrayList<>();
    private ObservableList<ItemUI> filteredItems = FXCollections.observableArrayList();
    private String currentCategoryFilter = "";

    // --- Cấu hình Phân trang & Infinite Scroll ---
    private int itemsPerPage = 8; // Nạp 8 sản phẩm mỗi đợt cuộn trang
    private int currentlyLoadedCount = 0;
    private boolean isLoadingMore = false;

    /**
     * Nhận thông tin người dùng từ màn hình Login thành công để cá nhân hóa lời chào.
     */
    public void setUserInfo(String username, String role) {
        this.currentUsername = username;
        lblGreeting.setText("Xin chào, " + username + "!");
    }

    /**
     * Khởi tạo cài đặt: cấu hình biểu đồ biến động giá, combobox bộ lọc, liên kết các bảng,
     * bộ lắng nghe nhập chữ tìm kiếm thời gian thực và kích hoạt lắng nghe luồng sự kiện Socket chạy nền.
     */
    @FXML
    public void initialize() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Mức giá trúng thầu hiện tại");
        if (priceChart != null) {
            priceChart.getData().add(priceSeries);
        }

        // Đổ danh sách dữ liệu cho bộ lọc FXML ComboBox
        cboFilterCondition.setItems(FXCollections.observableArrayList("Tất cả", "Mới (New)", "Đã sử dụng (Used)"));
        cboFilterCondition.setValue("Tất cả");
        cboFilterCondition.setOnAction(e -> applySearchFilter());

        cboFilterPrice.setItems(FXCollections.observableArrayList("Tất cả", "Dưới 1,000,000 ₫", "1,000,000 ₫ - 5,000,000 ₫", "Trên 5,000,000 ₫"));
        cboFilterPrice.setValue("Tất cả");
        cboFilterPrice.setOnAction(e -> applySearchFilter());

        cboFilterRating.setValue("Tất cả");
        cboFilterRating.setOnAction(e -> applySearchFilter());

        // Định dạng cột cho Bảng Lịch sử đặt thầu
        colHistName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colHistPrice.setCellValueFactory(new PropertyValueFactory<>("priceStr"));
        colHistBids.setCellValueFactory(new PropertyValueFactory<>("bidsCount"));
        colHistStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Định dạng cột cho Bảng Sản phẩm thắng cuộc chờ chốt hóa đơn
        colWonName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colWonPrice.setCellValueFactory(new PropertyValueFactory<>("priceStr"));
        colWonSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        colWonDate.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colWonStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Gắn bộ lắng nghe từ khóa tìm kiếm (Real-time keyword listener)
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applySearchFilter();
        });

        // Thiết lập Infinite Scroll trên ScrollPane chính
        if (scrollPaneBrowse != null) {
            scrollPaneBrowse.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                // 1. Hiển thị nút "Quay lại đầu trang" nếu cuộn quá 40%
                if (newVal.doubleValue() > 0.4) {
                    btnBackToTop.setVisible(true);
                } else {
                    btnBackToTop.setVisible(false);
                }
                
                // 2. Kích hoạt tải thêm sản phẩm khi cuộn quá 90% (Infinite Scroll)
                if (newVal.doubleValue() > 0.9 && !isLoadingMore && currentlyLoadedCount < filteredItems.size()) {
                    loadMoreProducts();
                }
            });
        }

        // Thiết lập lưới Responsive Grid: Tự động tính toán lại bề rộng khi co giãn cửa sổ
        if (gridItems != null) {
            gridItems.widthProperty().addListener((obs, oldVal, newVal) -> {
                updateGridResponsive(newVal.doubleValue());
            });
        }

        // Khởi động luồng Client Socket kết nối trực tiếp với Server để nhận các sự kiện thời gian thực
        startBackgroundListener();
    }

    // =========================================================================
    // --- CHUYỂN TRANG & SIDEBAR NAVIGATION ---
    // =========================================================================

    private void clearAllSidebarSelections() {
        Label[] allLabels = {
            sideBrowse, sideHistory, sideWonItems,
            catAll, catElectronics, catArt, catVehicle, catGeneral
        };
        for (Label label : allLabels) {
            if (label != null) {
                label.setStyle("-fx-cursor: hand; -fx-background-color: transparent; -fx-text-fill: #444444;");
            }
        }
    }

    @FXML
    public void handleSideBrowse(Event event) {
        showPage(pageBrowse);
        if (event != null) {
            clearAllSidebarSelections();
            setActiveSidebar(sideBrowse);
            currentCategoryFilter = "";
            lblCategoryTitle.setText("Tất cả sản phẩm");
            applySearchFilter();
        }
    }

    @FXML
    public void handleBackToBrowse(ActionEvent event) {
        handleSideBrowse(event);
    }

    private void setActiveSidebar(Label label) {
        clearAllSidebarSelections();
        label.setStyle("-fx-cursor: hand; -fx-font-weight: bold; -fx-background-color: #3665f315; -fx-text-fill: #3665f3;");
    }

    @FXML public void handleSideHistory(MouseEvent event) {
        showPage(pageHistory);
        setActiveSidebar(sideHistory);
        loadHistoryData();
    }

    @FXML public void handleSideWonItems(MouseEvent event) {
        showPage(pageWonItems);
        setActiveSidebar(sideWonItems);
        loadWonData();
    }

    /**
     * Lọc danh sách sản phẩm hiển thị trên bảng Lịch sử (User đã từng đặt thầu hoặc cài Auto Bid).
     */
    private void loadHistoryData() {
        List<ItemUI> hist = new ArrayList<>();
        for (ItemUI item : allItems) {
            if (currentUsername.equals(item.getWinnerUsername()) || activeAutoBids.containsKey(item.getAuctionId())) {
                hist.add(item);
            }
        }
        tableHistory.setItems(FXCollections.observableArrayList(hist));
    }

    /**
     * Lọc các sản phẩm đấu giá đã kết thúc mà User là người thắng cuộc cuối cùng để chuẩn bị thanh toán.
     */
    private void loadWonData() {
        List<ItemUI> won = new ArrayList<>();
        for (ItemUI item : allItems) {
            if (("Chờ thanh toán".equals(item.getStatus()) || "Hoàn thành".equals(item.getStatus())) 
                    && currentUsername.equals(item.getWinnerUsername())) {
                won.add(item);
            }
        }
        tableWon.setItems(FXCollections.observableArrayList(won));
        
        boolean hasPending = won.stream().anyMatch(item -> "Chờ thanh toán".equals(item.getStatus()));
        if (btnPayAllWon != null) {
            btnPayAllWon.setDisable(!hasPending);
            btnPayAllWon.setOpacity(!hasPending ? 0.5 : 1.0);
        }
    }

    /**
     * Ẩn toàn bộ các trang con và chỉ hiển thị trang VBox được chỉ định để tiết kiệm tài nguyên vẽ.
     */
    private void showPage(Region page) {
        pageBrowse.setVisible(false);
        pageBrowse.setManaged(false);
        pageProductDetail.setVisible(false);
        pageProductDetail.setManaged(false);
        if (pageHistory != null) { pageHistory.setVisible(false); pageHistory.setManaged(false); }
        if (pageWonItems != null) { pageWonItems.setVisible(false); pageWonItems.setManaged(false); }
        if (pageCheckout != null) { pageCheckout.setVisible(false); pageCheckout.setManaged(false); }

        page.setVisible(true);
        page.setManaged(true);
    }

    private void setActiveCategory(Label label, String category, String title) {
        showPage(pageBrowse);
        setActiveSidebar(label);
        currentCategoryFilter = category;
        if (lblCategoryTitle != null) {
            lblCategoryTitle.setText(title);
        }
        applySearchFilter();
    }

    // =======================================================================
    @FXML public void handleCategoryAll(MouseEvent event) { setActiveCategory(catAll, "", "Tất cả sản phẩm"); }
    @FXML public void handleCategoryElectronics(MouseEvent event) { setActiveCategory(catElectronics, "ELECTRONICS", "Đồ điện tử"); }
    @FXML public void handleCategoryArt(MouseEvent event) { setActiveCategory(catArt, "ART", "Nghệ thuật"); }
    @FXML public void handleCategoryVehicle(MouseEvent event) { setActiveCategory(catVehicle, "VEHICLE", "Xe cộ"); }
    @FXML public void handleCategoryGeneral(MouseEvent event) { setActiveCategory(catGeneral, "GENERAL", "Sản phẩm khác"); }

    // =========================================================================
    // --- KẾT NỐI MẠNG & NHẬN THÔNG TIN THỜI GIAN THỰC ---
    // =========================================================================

    /**
     * Thiết lập Socket Client chạy trên Thread độc lập để lắng nghe bản tin cập nhật giá,
     * lượt đặt thầu mới và sự thay đổi thời gian từ Server đấu giá.
     */
    private void startBackgroundListener() {
        new Thread(() -> {
            try {
                socket = new Socket("127.0.0.1", 9999);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println("{\"command\":\"GET_ITEMS\"}");

                String response;
                while ((response = in.readLine()) != null) {
                    try {
                        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                        String cmd = json.has("command") ? json.get("command").getAsString() : "";

                        if ("SET_ITEMS".equals(cmd)) {
                            JsonArray dataArray = json.getAsJsonArray("data");
                            List<ItemUI> newItems = new ArrayList<>();
                            for (JsonElement element : dataArray) {
                                JsonObject obj = element.getAsJsonObject();
                                String name = obj.has("name") ? obj.get("name").getAsString() : "Sản phẩm";
                                String desc = obj.has("description") ? obj.get("description").getAsString() : "";
                                double startPrice = obj.has("startingPrice") ? obj.get("startingPrice").getAsDouble() : 0.0;
                                double currentPrice = obj.has("currentHighestPrice") ? obj.get("currentHighestPrice").getAsDouble() : startPrice;
                                if (currentPrice <= 0) currentPrice = startPrice;
                                
                                String type = obj.has("type") ? obj.get("type").getAsString() : "GENERAL";
                                // Kiểm tra gán danh mục mặc định dựa trên các trường đặc trưng
                                if ("GENERAL".equals(type)) {
                                    if (obj.has("manufacturer") || obj.has("model")) type = "ELECTRONICS";
                                    else if (obj.has("artist") || obj.has("medium")) type = "ART";
                                    else if (obj.has("make") || obj.has("mileage")) type = "VEHICLE";
                                }
                                
                                // Phân tích thời gian kết thúc đấu giá tương ứng
                                String endTime = "N/A";
                                if (obj.has("endTime")) {
                                    try {
                                        JsonObject endObj = obj.getAsJsonObject("endTime");
                                        if (endObj.has("date") && endObj.has("time")) {
                                            JsonObject d = endObj.getAsJsonObject("date");
                                            JsonObject t = endObj.getAsJsonObject("time");
                                            endTime = String.format("%04d-%02d-%02dT%02d:%02d:%02d", 
                                                d.get("year").getAsInt(), d.get("month").getAsInt(), d.get("day").getAsInt(),
                                                t.get("hour").getAsInt(), t.get("minute").getAsInt(), t.get("second").getAsInt());
                                        }
                                    } catch (Exception e) {
                                        endTime = obj.get("endTime").getAsString();
                                    }
                                }
                                
                                String sellerName = "Unknown";
                                if (obj.has("seller")) {
                                    try {
                                        JsonObject sellerObj = obj.getAsJsonObject("seller");
                                        sellerName = sellerObj.has("username") ? sellerObj.get("username").getAsString() : "Unknown";
                                    } catch (Exception e) {}
                                }

                                String serverStatus = obj.has("auctionStatus") ? obj.get("auctionStatus").getAsString() : "RUNNING";
                                String status = "Đang đấu giá";
                                if ("FINISHED".equals(serverStatus)) status = "Hoàn thành";
                                else if ("ENDED_NO_WINNER".equals(serverStatus)) status = "Đã kết thúc";
                                else if ("ENDED_WITH_WINNER".equals(serverStatus)) status = "Chờ thanh toán";
                                else if ("OPEN".equals(serverStatus)) status = "Chờ mở";
                                
                                String winnerUsername = obj.has("winnerUsername") ? obj.get("winnerUsername").getAsString() : "";
                                
                                int bidsCount = obj.has("bidsCount") ? obj.get("bidsCount").getAsInt() : 0;
                                String imageUrls = obj.has("imageUrls") ? obj.get("imageUrls").getAsString() : "";
 
                                ItemUI itemUI = new ItemUI(
                                    name, desc, currentPrice, status, startPrice, endTime, type, sellerName, bidsCount, imageUrls
                                );
                                itemUI.setWinnerUsername(winnerUsername);
                                
                                if (obj.has("auctionId")) {
                                    itemUI.setAuctionId(obj.get("auctionId").getAsString());
                                }
                                
                                newItems.add(itemUI);
                            }

                            Platform.runLater(() -> {
                                allItems = newItems;
                                applySearchFilter();
                            });
                        }
                        // Nhận sự kiện thời gian thực khi có người đặt thầu mới (Nâng giá thành công)
                        else if ("UPDATE_PRICE".equals(cmd)) {
                            String targetId = json.has("auctionId") ? json.get("auctionId").getAsString() : "";
                            double newP = json.has("price") ? json.get("price").getAsDouble() : -1;
                            String winner = json.has("winnerUsername") ? json.get("winnerUsername").getAsString() : "";
                            
                            if (newP != -1) {
                                final double finalPrice = newP;
                                String currentTime = LocalTime.now().format(timeFormatter);
                                
                                Platform.runLater(() -> {
                                    // Nếu người dùng đang xem trang chi tiết sản phẩm này, lập tức nâng giá trên UI
                                    if (selectedProductForDetail != null && targetId.equals(selectedProductForDetail.getAuctionId())) {
                                        selectedProductForDetail.setCurrentPrice(finalPrice);
                                        selectedProductForDetail.setWinnerUsername(winner);
                                        selectedProductForDetail.setBidsCount(selectedProductForDetail.getBidsCount() + 1);
                                        lblDetailPrice.setText(selectedProductForDetail.getPriceStr());
                                        if (lblDetailBidsCount != null) {
                                            lblDetailBidsCount.setText(selectedProductForDetail.getBidsCount() + " lượt");
                                        }
                                        updateBidStatusUI();
                                        
                                        // Thêm một mốc điểm mới vào biểu đồ LineChart
                                        if (priceSeries != null) {
                                            priceSeries.getData().add(new XYChart.Data<>(currentTime, finalPrice));
                                            if (priceSeries.getData().size() > 15) {
                                                priceSeries.getData().remove(0); // Giữ tối đa 15 mốc điểm gần nhất tránh rối đồ thị
                                            }
                                        }
                                    }
                                });
                            }
                            // Xin lại danh sách mới nhất để đồng bộ giao diện lưới duyệt
                            out.println("{\"command\":\"GET_ITEMS\"}");
                        }
                        // Nhận cập nhật gia hạn thời gian (Anti-Sniping gia hạn thầu ở phút cuối)
                        else if ("UPDATE_TIME".equals(cmd)) {
                            String newEndTime = json.has("newEndTime") ? json.get("newEndTime").getAsString() : "";
                            if (!newEndTime.isEmpty() && selectedProductForDetail != null) {
                                Platform.runLater(() -> {
                                    selectedProductForDetail.setEndTime(newEndTime);
                                });
                            }
                        }
                        // Nhận sự kiện thời gian thực khi có sản phẩm mới được thêm hoặc xóa trên hệ thống
                        else if ("UPDATE_ITEMS".equals(cmd)) {
                            out.println("{\"command\":\"GET_ITEMS\"}");
                        }
                    } catch (Exception ex) {
                        if (!socket.isClosed()) {
                            System.out.println("Lỗi xử lý JSON: " + ex.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                if (socket != null && !socket.isClosed()) {
                    System.out.println("Lỗi kết nối Socket tại Dashboard.");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @FXML
    public void handleSearch(ActionEvent event) {
        applySearchFilter();
    }

    /**
     * Áp dụng bộ lọc tổ hợp giữa ô nhập chữ tìm kiếm, bộ lọc mức giá ComboBox và danh mục.
     */
    private void applySearchFilter() {
        String keyword = txtSearch.getText() != null ? txtSearch.getText().toLowerCase().trim() : "";
        String filterPrice = cboFilterPrice.getValue();
        
        List<ItemUI> result = new ArrayList<>();
        
        for (ItemUI item : allItems) {
            // Chỉ hiển thị sản phẩm đang chạy thầu hoặc chờ mở trên giao diện tìm kiếm / mua sắm chính
            if (!"Đang đấu giá".equals(item.getStatus()) && !"Chờ mở".equals(item.getStatus())) {
                continue;
            }
            // Lọc theo Danh mục
            if (!currentCategoryFilter.isEmpty() && !currentCategoryFilter.equalsIgnoreCase(item.getType())) {
                continue;
            }
            // Lọc theo từ khóa (Tên / Mô tả sản phẩm)
            if (!keyword.isEmpty()) {
                boolean match = (item.getName() != null && item.getName().toLowerCase().contains(keyword)) ||
                                (item.getDescription() != null && item.getDescription().toLowerCase().contains(keyword));
                if (!match) continue;
            }
            
            // Lọc theo Khoảng Giá (Price range)
            if (filterPrice != null && !filterPrice.equals("Tất cả")) {
                double price = item.getRawPrice();
                if (filterPrice.equals("Dưới 1,000,000 ₫") && price >= 1000000) continue;
                if (filterPrice.equals("1,000,000 ₫ - 5,000,000 ₫") && (price < 1000000 || price > 5000000)) continue;
                if (filterPrice.equals("Trên 5,000,000 ₫") && price <= 5000000) continue;
            }
            
            result.add(item);
        }
        
        filteredItems.setAll(result);
        currentlyLoadedCount = 0; // Đặt lại bộ đếm phân trang
        gridItems.getChildren().clear(); // Dọn dẹp giao diện cũ
        loadMoreProducts(); // Bắt đầu nạp đợt sản phẩm đầu tiên
    }

    /**
     * Kỹ thuật Phân trang trễ (Lazy-Loading): Chỉ vẽ các thẻ sản phẩm thuộc lô trang hiện hành 
     * lên giao diện khi người dùng thực hiện cuộn sâu xuống dưới.
     */
    private void loadMoreProducts() {
        if (isLoadingMore) return;
        isLoadingMore = true;
        
        int nextBatch = Math.min(currentlyLoadedCount + itemsPerPage, filteredItems.size());
        List<ItemUI> toAdd = filteredItems.subList(currentlyLoadedCount, nextBatch);
        
        Platform.runLater(() -> {
            for (ItemUI item : toAdd) {
                gridItems.getChildren().add(createProductCard(item));
            }
            currentlyLoadedCount = nextBatch;
            isLoadingMore = false;
            updateGridResponsive(gridItems.getWidth()); // Căn chỉnh kích thước responsive
        });
    }

    /**
     * Dựng bố cục co giãn linh động (Responsive Grid Layout):
     * - Bề rộng > 1100px: Chia làm 4 cột.
     * - Bề rộng > 850px: Chia làm 3 cột.
     * - Bề rộng > 550px: Chia làm 2 cột.
     * - Nhỏ hơn: Hiển thị 1 cột duy nhất tràn màn hình.
     */
    private void updateGridResponsive(double width) {
        if (gridItems == null || width <= 0) return;
        
        int cols = (width > 1100) ? 4 : (width > 850) ? 3 : (width > 550) ? 2 : 1;
        double cardW = (width - (gridItems.getHgap() * (cols + 1))) / cols;
        
        for (javafx.scene.Node node : gridItems.getChildren()) {
            if (node instanceof VBox) {
                VBox card = (VBox) node;
                card.setPrefWidth(cardW);
                card.setMinWidth(cardW);
                card.setMaxWidth(cardW);
                
                // Đồng bộ chiều cao ảnh tỷ lệ 4:3 đẹp mắt
                StackPane img = (StackPane) card.lookup("#imgContainer");
                if (img != null) {
                    img.setPrefHeight(cardW * 0.75);
                }
            }
        }
    }

    /**
     * Thực hiện hoạt ảnh cuộn mượt mà (Smooth scrolling transition) lên đỉnh trang bằng Timeline.
     */
    @FXML
    public void handleBackToTop() {
        if (scrollPaneBrowse != null) {
            Timeline timeline = new Timeline();
            KeyValue kv = new KeyValue(scrollPaneBrowse.vvalueProperty(), 0);
            KeyFrame kf = new KeyFrame(Duration.millis(500), kv);
            timeline.getKeyFrames().add(kf);
            timeline.play();
        }
    }

    // =========================================================================
    // --- DỰNG CARD SẢN PHẨM HOVER DYNAMIC SYSTEM ---
    // =========================================================================

    private void renderProductCards() {
        // Trách nhiệm vẽ thẻ đã được phân cấp sang phân trang trễ loadMoreProducts
    }

    /**
     * Khởi tạo giao diện thẻ sản phẩm đấu giá cao cấp với các hiệu ứng phóng to micro-animation (Scale 1.03) 
     * và đổ bóng sâu (Dropshadow) chân thực khi rê chuột.
     */
    private VBox createProductCard(ItemUI item) {
        VBox card = new VBox(8);
        String baseStyle = "-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-radius: 15; -fx-padding: 0 0 15 0; -fx-background-radius: 15; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5);";
        card.setStyle(baseStyle);
        card.setPrefWidth(260);
        
        // Vi hoạt ảnh Hover phóng to nhẹ
        card.setOnMouseEntered(e -> { 
            card.setScaleX(1.03); 
            card.setScaleY(1.03); 
            card.setStyle(baseStyle + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 20, 0, 0, 10);");
        });
        card.setOnMouseExited(e -> { 
            card.setScaleX(1.0); 
            card.setScaleY(1.0); 
            card.setStyle(baseStyle);
        });
        card.setOnMouseClicked(e -> showProductDetail(item));

        updateCardContent(card, item);
        return card;
    }

    /**
     * Điền và cập nhật dữ liệu (Tên, ảnh, giá, trạng thái, thời gian còn lại) cho thẻ sản phẩm chỉ định.
     */
    private void updateCardContent(VBox card, ItemUI item) {
        // Dựng khung lần đầu nếu thẻ đang rỗng
        if (card.getChildren().isEmpty()) {
            StackPane imgContainer = new StackPane();
            imgContainer.setPrefSize(260, 200);
            imgContainer.setId("imgContainer");
            
            VBox infoBox = new VBox(5);
            infoBox.setStyle("-fx-padding: 10 15;");

            Label lblName = new Label(); lblName.setId("lblName");
            lblName.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
            lblName.setTextFill(javafx.scene.paint.Color.web("#0654ba"));
            lblName.setWrapText(true); lblName.setMaxHeight(40);
            
            Label lblPrice = new Label(); lblPrice.setId("lblPrice");
            lblPrice.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 20));

            Label lblBids = new Label(); lblBids.setId("lblBids");
            lblBids.setTextFill(javafx.scene.paint.Color.GRAY);
            
            Label lblStatus = new Label(); lblStatus.setId("lblStatus");
            
            Label lblTime = new Label(); lblTime.setId("lblTime");
            lblTime.setTextFill(javafx.scene.paint.Color.web("#e62117"));

            infoBox.getChildren().addAll(lblName, lblPrice, lblBids, lblStatus, lblTime);
            card.getChildren().addAll(imgContainer, infoBox);
        }

        // Lấy tham chiếu các nhãn
        StackPane imgContainer = (StackPane) card.lookup("#imgContainer");
        VBox infoBox = (VBox) card.getChildren().get(1);
        Label lblName = (Label) infoBox.lookup("#lblName");
        Label lblPrice = (Label) infoBox.lookup("#lblPrice");
        Label lblBids = (Label) infoBox.lookup("#lblBids");
        Label lblStatus = (Label) infoBox.lookup("#lblStatus");
        Label lblTime = (Label) infoBox.lookup("#lblTime");

        // Ghi văn bản tương ứng
        if (lblName != null) lblName.setText(item.getName());
        if (lblPrice != null) lblPrice.setText(item.getPriceStr());
        if (lblBids != null) lblBids.setText(item.getBidsCount() + " lượt đặt giá");
        
        if (lblStatus != null) {
            lblStatus.setText("Trạng thái: " + item.getStatus());
            lblStatus.setTextFill(item.getStatus().equals("Đã kết thúc") ? javafx.scene.paint.Color.RED : javafx.scene.paint.Color.web("#0654ba"));
        }
        
        if (lblTime != null) lblTime.setText(calculateTimeRemaining(item));

        // Nạp ảnh nền bất đồng bộ (Lazy image loading) để tránh nghẽn luồng vẽ JavaFX Application Thread
        if (imgContainer != null && imgContainer.getChildren().isEmpty()) {
            if (item.getImageUrls() != null && !item.getImageUrls().isEmpty()) {
                String firstImage = item.getImageUrls().split(",")[0];
                try {
                    // Cài đặt backgroundLoading = true để nạp ảnh ngầm
                    javafx.scene.image.Image img = new javafx.scene.image.Image(firstImage, 400, 300, true, true, true);
                    javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView(img);
                    
                    imgView.fitWidthProperty().bind(imgContainer.widthProperty());
                    imgView.fitHeightProperty().bind(imgContainer.heightProperty());
                    imgView.setPreserveRatio(true);
                    
                    // Cắt các góc ảnh bo tròn 30px tinh tế trùng khớp với viền thẻ
                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
                    clip.widthProperty().bind(imgContainer.widthProperty());
                    clip.heightProperty().bind(imgContainer.heightProperty());
                    clip.setArcWidth(30); clip.setArcHeight(30);
                    imgView.setClip(clip);
                    
                    imgContainer.getChildren().add(imgView);
                } catch (Exception e) { imgContainer.getChildren().add(createPlaceholderLabel("Lỗi ảnh")); }
            } else {
                imgContainer.getChildren().add(createPlaceholderLabel("Chưa có ảnh"));
            }
        }
    }

    private Label createPlaceholderLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-background-color: #eaeaea; -fx-text-fill: #999999; -fx-alignment: center; -fx-background-radius: 8 8 0 0;");
        lbl.setPrefSize(260, 200);
        return lbl;
    }

    /**
     * Thuật toán tính toán hiệu chỉnh thời gian thầu còn lại theo giây thực tế.
     */
    private String calculateTimeRemaining(ItemUI item) {
        try {
            java.time.LocalDateTime dt = java.time.LocalDateTime.parse(item.getEndTime());
            java.time.Duration diff = java.time.Duration.between(java.time.LocalDateTime.now(), dt);
            if (diff.isNegative() || diff.isZero()) return "Đã kết thúc";
            long d = diff.toDays();
            long h = diff.toHoursPart();
            long m = diff.toMinutesPart();
            if (d > 0) return "Còn lại: " + d + " ngày " + h + " giờ";
            if (h > 0) return "Còn lại: " + h + " giờ " + m + " phút";
            return "Còn lại: " + m + " phút " + diff.toSecondsPart() + " giây";
        } catch(Exception e) { return "N/A"; }
    }

    /**
     * Đóng gói lệnh BID gửi lên máy chủ Server bằng cấu trúc JSON đồng nhất qua Socket.
     * @param item Sản phẩm đấu thầu
     * @param amountStr Giá trị đặt thầu nhập vào
     */
    private void doPlaceBid(ItemUI item, String amountStr) {
        if (amountStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập mức giá!");
            return;
        }

        try {
            double bidAmount = Double.parseDouble(amountStr);
            if (bidAmount <= item.getRawPrice()) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Giá đấu phải cao hơn giá hiện tại!");
                return;
            }

            if (item.getAuctionId() == null || item.getAuctionId().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Sản phẩm này không có phiên đấu giá!");
                return;
            }

            JsonObject bidRequest = new JsonObject();
            bidRequest.addProperty("command", "BID");

            JsonObject data = new JsonObject();
            data.addProperty("auctionId", item.getAuctionId());
            JsonObject bidder = new JsonObject();
            bidder.addProperty("username", currentUsername);
            data.add("bidder", bidder);
            data.addProperty("amount", bidAmount);

            bidRequest.add("data", data);
            out.println(new Gson().toJson(bidRequest));

            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã gửi yêu cầu đặt giá: " + String.format("%,.0f", bidAmount) + " ₫");

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Vui lòng nhập số hợp lệ!");
        }
    }

    /**
     * Mở màn hình Chi tiết sản phẩm, khởi chạy đồng hồ đếm ngược nguy cấp, khởi tạo 
     * điểm biểu đồ giá ban đầu và làm mới khối thiết lập Auto-Bid.
     * @param item Đối tượng sản phẩm thầu được chọn
     */
    private void showProductDetail(ItemUI item) {
        selectedProductForDetail = item;
        lblDetailName.setText(item.getName());
        lblDetailPrice.setText(item.getPriceStr()); 
        
        if (lblDetailBidsCount != null) {
            lblDetailBidsCount.setText(item.getBidsCount() + " lượt");
        }
        
        if (lblDetailStatus != null) {
            lblDetailStatus.setText(item.getStatus());
            lblDetailStatus.setTextFill(item.getStatus().equals("Đã kết thúc") ? javafx.scene.paint.Color.RED : javafx.scene.paint.Color.web("#0654ba"));
        }
        
        if (priceSeries != null) {
            priceSeries.getData().clear();
            String currentTime = java.time.LocalTime.now().format(timeFormatter);
            priceSeries.getData().add(new XYChart.Data<>(currentTime, item.getRawPrice()));
        }
        
        startCountdown();
        startChartAutoUpdate();
        updateAutoBidUI();
        updateBidStatusUI();
        
        // Vô hiệu hóa điều khiển đặt thầu nếu phiên đấu giá đã kết thúc
        boolean isEnded = "Đã kết thúc".equals(item.getStatus());
        txtBidAmount.setDisable(isEnded);
        if (btnPlaceBid != null) btnPlaceBid.setDisable(isEnded);
        if (hboxQuickBids != null) hboxQuickBids.setDisable(isEnded);
        if (vboxAutoBidSetup != null) vboxAutoBidSetup.setDisable(isEnded);
        if (vboxAutoBidActive != null) vboxAutoBidActive.setDisable(isEnded);
        
        showPage(pageProductDetail);
    }

    /**
     * Cập nhật chỉ báo trạng thái đấu thầu (Bạn đang dẫn đầu / Đã bị vượt mặt) với màu nền đỏ/xanh trực quan.
     */
    private void updateBidStatusUI() {
        if (selectedProductForDetail == null) return;
        
        String winner = selectedProductForDetail.getWinnerUsername();
        if (winner == null || winner.isEmpty()) {
            lblBidStatus.setText("Chưa có lượt đặt giá");
            lblBidStatus.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #777; -fx-font-weight: bold;");
        } else if (winner.equals(currentUsername)) {
            lblBidStatus.setText("BẠN ĐANG DẪN ĐẦU");
            lblBidStatus.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        } else {
            lblBidStatus.setText("BẠN ĐÃ BỊ VƯỢT MẶT");
            lblBidStatus.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-font-weight: bold;");
        }
    }

    /**
     * Tự động bổ sung định kỳ mốc giá hiện tại vào biểu đồ sau mỗi 5 giây 
     * giúp đồ thị dịch chuyển động sinh động.
     */
    private void startChartAutoUpdate() {
        if (chartUpdateTimeline != null) {
            chartUpdateTimeline.stop();
        }
        
        chartUpdateTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (selectedProductForDetail == null || priceSeries == null) return;
            
            String currentTime = LocalTime.now().format(timeFormatter);
            double currentPrice = selectedProductForDetail.getRawPrice();
            
            Platform.runLater(() -> {
                priceSeries.getData().add(new XYChart.Data<>(currentTime, currentPrice));
                if (priceSeries.getData().size() > 20) {
                    priceSeries.getData().remove(0);
                }
            });
        }));
        chartUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        chartUpdateTimeline.play();
    }

    // Các hàm xử lý ba nút bấm đặt giá nhanh (+50k, +100k, +500k)
    @FXML private void handleAdd50k() { addQuickBid(50000); }
    @FXML private void handleAdd100k() { addQuickBid(100000); }
    @FXML private void handleAdd500k() { addQuickBid(500000); }

    private void addQuickBid(double amount) {
        if (selectedProductForDetail == null) return;
        double currentVal = 0;
        try {
            String txt = txtBidAmount.getText().replace(",", "").replace(".", "");
            if (!txt.isEmpty()) currentVal = Double.parseDouble(txt);
        } catch (Exception e) {}
        
        if (currentVal < selectedProductForDetail.getRawPrice()) {
            currentVal = selectedProductForDetail.getRawPrice();
        }
        
        txtBidAmount.setText(String.format("%.0f", currentVal + amount));
    }

    /**
     * Khởi chạy đồng hồ đếm ngược giật giây (1s).
     * Khi thời gian còn dưới 60 giây, đổi chữ sang màu đỏ rực nhấp nháy 
     * để thúc đẩy tâm lý người thầu đưa ra quyết định nhanh.
     */
    private void startCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (selectedProductForDetail == null) return;
            
            try {
                LocalDateTime end = LocalDateTime.parse(selectedProductForDetail.getEndTime());
                LocalDateTime now = LocalDateTime.now();
                
                java.time.Duration diff = java.time.Duration.between(now, end);
                if (diff.isNegative() || diff.isZero()) {
                    lblCountdown.setText("ĐÃ KẾT THÚC");
                    lblCountdown.setStyle("-fx-font-weight: bold; -fx-font-size: 24; -fx-text-fill: #999;");
                    
                    // Cập nhật trạng thái hiển thị thời gian thực trên giao diện chi tiết
                    if (lblDetailStatus != null) {
                        lblDetailStatus.setText("Đã kết thúc");
                        lblDetailStatus.setTextFill(javafx.scene.paint.Color.RED);
                    }
                    selectedProductForDetail.setStatus("Đã kết thúc");
                    
                    // Vô hiệu hóa tất cả bộ điều khiển đặt thầu để tránh người dùng thao tác thừa
                    txtBidAmount.setDisable(true);
                    if (btnPlaceBid != null) btnPlaceBid.setDisable(true);
                    if (hboxQuickBids != null) hboxQuickBids.setDisable(true);
                    if (vboxAutoBidSetup != null) vboxAutoBidSetup.setDisable(true);
                    if (vboxAutoBidActive != null) vboxAutoBidActive.setDisable(true);
                    
                    countdownTimeline.stop();
                } else {
                    long h = diff.toHours();
                    long m = diff.toMinutesPart();
                    long s = diff.toSecondsPart();
                    lblCountdown.setText(String.format("%02d:%02d:%02d", h, m, s));
                    
                    // Dưới 60 giây đổi màu đỏ cảnh báo khẩn cấp
                    if (diff.toSeconds() < 60) {
                        lblCountdown.setStyle("-fx-font-weight: bold; -fx-font-size: 24; -fx-text-fill: #e62117;");
                    } else {
                        lblCountdown.setStyle("-fx-font-weight: bold; -fx-font-size: 24; -fx-text-fill: #0654ba;");
                    }
                }
            } catch (Exception ex) {
                lblCountdown.setText("N/A");
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    @FXML
    public void handlePlaceBid(ActionEvent event) {
        if (selectedProductForDetail != null) {
            String amountStr = txtBidAmount.getText().trim();
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= selectedProductForDetail.getRawPrice()) {
                    showAlert(Alert.AlertType.WARNING, "Giá thầu không hợp lệ", 
                        "Giá đặt phải cao hơn giá hiện tại!");
                    return;
                }
                doPlaceBid(selectedProductForDetail, amountStr);
                txtBidAmount.clear();
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập số tiền hợp lệ!");
            }
        }
    }

    /**
     * Đóng gói lệnh AUTO_BID gửi lên Server khi người dùng cấu hình giá thầu tự động.
     * Áp dụng quy tắc kiểm soát nghiệp vụ: bước giá tối thiểu thầu tự động phải đạt 10,000 đ.
     */
    @FXML
    public void handleSetAutoBid(ActionEvent event) {
        if (selectedProductForDetail == null) return;
        
        String maxStr = txtAutoBidMax.getText().trim();
        String incStr = txtAutoBidIncrement.getText().trim();
        
        if (maxStr.isEmpty() || incStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đủ Mức giá tối đa và Bước giá!");
            return;
        }

        try {
            double maxBid = Double.parseDouble(maxStr);
            double increment = Double.parseDouble(incStr);
            
            if (maxBid <= selectedProductForDetail.getRawPrice()) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Mức giá tối đa phải cao hơn giá hiện tại!");
                return;
            }
            
            // Ép buộc kiểm tra nghiệp vụ tối thiểu 10k
            if (increment < MIN_INCREMENT) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Bước giá tối thiểu phải là " + String.format("%,.0f", MIN_INCREMENT) + " ₫!");
                return;
            }

            // Hộp thoại xác nhận của JavaFX trước khi lưu thầu tự động
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Xác nhận thiết lập Auto Bid");
            confirm.setHeaderText("Bạn có chắc chắn muốn thiết lập Auto Bid?");
            confirm.setContentText("Hệ thống sẽ tự động đặt thầu cho bạn với bước nhảy " + String.format("%,.0f", increment) + " ₫ lên tới mức " + String.format("%,.0f", maxBid) + " ₫.");
            
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                JsonObject bidRequest = new JsonObject();
                bidRequest.addProperty("command", "AUTO_BID");
                JsonObject data = new JsonObject();
                data.addProperty("auctionId", selectedProductForDetail.getAuctionId());
                data.addProperty("username", currentUsername);
                data.addProperty("maxBid", maxBid);
                data.addProperty("increment", increment);
                bidRequest.add("data", data);
                
                if (out != null) {
                    out.println(new Gson().toJson(bidRequest));
                    // Lưu trạng thái và đổi UI sang Active ngay
                    activeAutoBids.put(selectedProductForDetail.getAuctionId(), maxBid);
                    updateAutoBidUI();
                    
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã kích hoạt Auto Bid với bước nhảy " + String.format("%,.0f", increment) + " ₫.");
                    txtAutoBidMax.clear();
                    txtAutoBidIncrement.clear();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến máy chủ.");
                }
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập số hợp lệ!");
        }
    }

    /**
     * Gửi yêu cầu CANCEL_AUTO_BID lên Server để gỡ chế độ thầu tự động cho sản phẩm thầu.
     */
    @FXML
    public void handleCancelAutoBid(ActionEvent event) {
        if (selectedProductForDetail == null) return;
        
        JsonObject cancelRequest = new JsonObject();
        cancelRequest.addProperty("command", "CANCEL_AUTO_BID");
        cancelRequest.addProperty("auctionId", selectedProductForDetail.getAuctionId());
        cancelRequest.addProperty("username", currentUsername);
        
        if (out != null) {
            out.println(new Gson().toJson(cancelRequest));
            activeAutoBids.remove(selectedProductForDetail.getAuctionId());
            updateAutoBidUI();
            showAlert(Alert.AlertType.INFORMATION, "Đã hủy", "Đã dừng Auto Bid cho sản phẩm này.");
        }
    }

    /**
     * Thay đổi ẩn hiện hai khối VBox thiết lập hoặc hiển thị Auto-Bid tùy theo trạng thái lưu trữ.
     */
    private void updateAutoBidUI() {
        if (selectedProductForDetail == null) return;
        
        boolean isActive = activeAutoBids.containsKey(selectedProductForDetail.getAuctionId());
        vboxAutoBidSetup.setVisible(!isActive);
        vboxAutoBidSetup.setManaged(!isActive);
        vboxAutoBidActive.setVisible(isActive);
        vboxAutoBidActive.setManaged(isActive);
        
        if (isActive) {
            double max = activeAutoBids.get(selectedProductForDetail.getAuctionId());
            lblActiveAutoBidMax.setText("Tối đa: " + String.format("%,.0f", max) + " ₫");
        }
    }

    // =========================================================================
    // --- TIỆN ÍCH MINI CART & THANH TOÁN ĐƠN HÀNG THẮNG CUỘC ---
    // =========================================================================

    @FXML
    private void handleShowNotifications() {
        showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Bạn không có thông báo mới nào.");
    }

    @FXML
    private void handleShowMiniCart() {
        if (pageMiniCart != null) {
            pageMiniCart.setVisible(true);
            pageMiniCart.setManaged(true);
            populateMiniCart();
        }
    }

    @FXML
    private void handleHideMiniCart() {
        if (pageMiniCart != null) {
            pageMiniCart.setVisible(false);
            pageMiniCart.setManaged(false);
        }
    }

    /**
     * Duyệt qua toàn bộ sản phẩm đã thắng thầu của user, vẽ ra các dòng HBox hiển thị trong mini-cart
     * và tính toán tổng số tiền tích lũy cần thanh toán.
     */
    private void populateMiniCart() {
        vboxMiniCartItems.getChildren().clear();
        double total = 0;
        int count = 0;

        for (ItemUI item : allItems) {
            if ("Chờ thanh toán".equals(item.getStatus()) && currentUsername.equals(item.getWinnerUsername())) {
                count++;
                total += item.getRawPrice();
                
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 5; -fx-background-color: #fcfcfc; -fx-background-radius: 5;");
                
                Label name = new Label(item.getName());
                name.setPrefWidth(150);
                name.setStyle("-fx-font-size: 12;");
                
                Label price = new Label(item.getPriceStr());
                price.setStyle("-fx-font-weight: bold; -fx-text-fill: #e62117; -fx-font-size: 12;");
                
                row.getChildren().addAll(name, price);
                vboxMiniCartItems.getChildren().add(row);
                
                if (count >= 5) {
                    Label more = new Label("... và " + (allItems.size() - count) + " sản phẩm khác");
                    more.setStyle("-fx-font-size: 11; -fx-text-fill: gray;");
                    vboxMiniCartItems.getChildren().add(more);
                    break;
                }
            }
        }
        
        if (count == 0) {
            vboxMiniCartItems.getChildren().add(new Label("Chưa có sản phẩm nào."));
        }
        
        lblMiniCartTotal.setText(String.format("%,.0f ₫", total));
        if (btnQuickPay != null) {
            btnQuickPay.setDisable(count == 0);
            btnQuickPay.setOpacity(count == 0 ? 0.5 : 1.0);
        }
    }

    @FXML
    private void handleQuickPay() {
        showCheckoutPage();
    }

    /**
     * Nạp dữ liệu và chuyển sang màn hình thanh toán Checkout chuẩn eBay
     */
    private void showCheckoutPage() {
        if (vboxCheckoutItems == null) return;
        
        vboxCheckoutItems.getChildren().clear();
        double subtotal = 0;
        int count = 0;

        for (ItemUI item : allItems) {
            if ("Chờ thanh toán".equals(item.getStatus()) && currentUsername.equals(item.getWinnerUsername())) {
                subtotal += item.getRawPrice();
                count++;
                
                HBox row = new HBox(15);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 10; -fx-background-color: #f9f9f9; -fx-background-radius: 8; -fx-border-color: #eee; -fx-border-width: 1; -fx-border-radius: 8;");
                
                Label name = new Label(item.getName());
                name.setPrefWidth(220);
                name.setWrapText(true);
                name.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #191919;");
                
                Region space = new Region();
                HBox.setHgrow(space, Priority.ALWAYS);
                
                Label price = new Label(item.getPriceStr());
                price.setStyle("-fx-font-weight: bold; -fx-text-fill: #3665f3; -fx-font-size: 13;");
                
                row.getChildren().addAll(name, space, price);
                vboxCheckoutItems.getChildren().add(row);
            }
        }

        if (count == 0) {
            vboxCheckoutItems.getChildren().add(new Label("Không có sản phẩm nào cần thanh toán."));
            if (lblCheckoutSubtotal != null) lblCheckoutSubtotal.setText("0 ₫");
            if (lblCheckoutTax != null) lblCheckoutTax.setText("0 ₫");
            if (lblCheckoutTotal != null) lblCheckoutTotal.setText("0 ₫");
            return;
        }

        double tax = subtotal * 0.08;
        double total = subtotal + tax;

        if (lblCheckoutSubtotal != null) lblCheckoutSubtotal.setText(String.format("%,.0f ₫", subtotal));
        if (lblCheckoutTax != null) lblCheckoutTax.setText(String.format("%,.0f ₫", tax));
        if (lblCheckoutTotal != null) lblCheckoutTotal.setText(String.format("%,.0f ₫", total));
        
        // Thiết lập thông tin giao nhận mẫu
        if (lblShipName != null) lblShipName.setText(currentUsername);
        if (lblShipPhone != null) lblShipPhone.setText("0912 345 678");
        if (lblShipAddress != null) lblShipAddress.setText("144 Xuân Thủy, Cầu Giấy, Hà Nội");

        // Chọn mặc định hình thức thẻ
        if (radCreditCard != null) radCreditCard.setSelected(true);

        showPage(pageCheckout);
    }

    @FXML
    private void handleBackToWonItems() {
        showPage(pageWonItems);
    }

    @FXML
    private void handleConfirmAndPay() {
        if (out != null) {
            JsonObject req = new JsonObject();
            req.addProperty("command", "PAY_WINNINGS");
            req.addProperty("username", currentUsername);
            
            String methodStr = "COD (Thanh toán khi nhận hàng)";
            if (radCreditCard != null && radCreditCard.isSelected()) {
                methodStr = "Credit Card (Thẻ tín dụng)";
            }
            req.addProperty("paymentMethod", methodStr);
            out.println(req.toString());
            
            showAlert(Alert.AlertType.INFORMATION, "Thanh toán thành công", 
                "Đơn giao dịch của bạn đã được gửi đi thành công!\nPhương thức thanh toán đã chọn: " + methodStr + ".\neBid sẽ sớm giao hàng đến bạn.");
            
            // Quay về danh sách chính để cập nhật hiển thị sản phẩm
            showPage(pageBrowse);
        }
    }

    /**
     * Thực hiện đóng kết nối Socket Client an toàn và định tuyến quay lại màn hình Login.fxml.
     * @param event Sự kiện nhấp nút đăng xuất
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            if (socket != null) socket.close();
            Stage stage = (Stage) lblGreeting.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.setTitle("Hệ thống Đấu giá eBid");
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hàm hiển thị Dialog nhanh trong luồng xử lý UI JavaFX.
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    // =========================================================================
    // --- LỚP DỰNG MÔ HÌNH DỮ LIỆU BẢNG GIAO DIỆN (DATA MODEL CLASS) ---
    // =========================================================================

    /**
     * Lớp tĩnh ItemUI đóng vai trò làm Data Transfer Object chứa thông số thầu của sản phẩm
     * được gán trực tiếp lên TableView và FlowPane lưới.
     */
    public static class ItemUI {
        private String name;
        private String description;
        private double rawPrice;
        private String priceStr;
        private String status;
        private double startingPrice;
        private String endTime;
        private String type;
        private String sellerName;
        private String auctionId;
        private String winnerUsername = "";
        private int bidsCount;
        private String imageUrls;

        public ItemUI(String name, String description, double rawPrice, String status, double startingPrice, String endTime, String type, String sellerName, int bidsCount, String imageUrls) {
            this.name = name;
            this.description = description;
            this.rawPrice = rawPrice;
            this.priceStr = String.format("%,.0f ₫", rawPrice);
            this.status = status;
            this.startingPrice = startingPrice;
            this.endTime = endTime;
            this.type = type;
            this.sellerName = sellerName;
            this.bidsCount = bidsCount;
            this.imageUrls = imageUrls;
            this.auctionId = "";
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getPriceStr() { return priceStr; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public double getRawPrice() { return rawPrice; }
        public double getStartingPrice() { return startingPrice; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public String getType() { return type; }
        public String getSellerName() { return sellerName; }
        public String getAuctionId() { return auctionId; }
        public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
        
        public int getBidsCount() { return bidsCount; }
        public void setBidsCount(int bidsCount) { this.bidsCount = bidsCount; }
        
        public void setWinnerUsername(String winner) { this.winnerUsername = winner; }
        public String getWinnerUsername() { return winnerUsername; }
        public String getImageUrls() { return imageUrls; }
        
        public void setCurrentPrice(double newPrice) {
            this.rawPrice = newPrice;
            this.priceStr = String.format("%,.0f ₫", newPrice);
        }
    }
}