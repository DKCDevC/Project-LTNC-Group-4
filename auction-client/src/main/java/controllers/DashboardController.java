package controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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

public class DashboardController {

    // --- Header & Sidebar ---
    @FXML private Label lblGreeting;
    @FXML private TextField txtSearch;
    @FXML private Label sideBrowse;

    // --- Layout ---
    @FXML private StackPane contentArea;
    @FXML private VBox pageBrowse;
    @FXML private ScrollPane scrollPaneBrowse;
    @FXML private Button btnBackToTop;
    @FXML private VBox pageHistory;
    @FXML private VBox pageWonItems;

    @FXML private Label sideHistory;
    @FXML private Label sideWonItems;

    // --- Table: History ---
    @FXML private TableView<ItemUI> tableHistory;
    @FXML private TableColumn<ItemUI, String> colHistName;
    @FXML private TableColumn<ItemUI, String> colHistPrice;
    @FXML private TableColumn<ItemUI, Integer> colHistBids;
    @FXML private TableColumn<ItemUI, String> colHistStatus;

    // --- Table: Won ---
    @FXML private TableView<ItemUI> tableWon;
    @FXML private TableColumn<ItemUI, String> colWonName;
    @FXML private TableColumn<ItemUI, String> colWonPrice;
    @FXML private TableColumn<ItemUI, String> colWonSeller;
    @FXML private TableColumn<ItemUI, String> colWonDate;

    // --- Page: Browse ---
    @FXML private Label lblCategoryTitle;
    @FXML private FlowPane gridItems;
    @FXML private ComboBox<String> cboFilterCondition;
    @FXML private ComboBox<String> cboFilterPrice;
    @FXML private ComboBox<String> cboFilterRating;



    // --- Page: Product Detail ---
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
    @FXML private LineChart<String, Number> priceChart;
    private XYChart.Series<String, Number> priceSeries;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    @FXML private Label lblCountdown;
    private Timeline countdownTimeline;
    private Timeline chartUpdateTimeline;
    private ItemUI selectedProductForDetail;

    @FXML private VBox vboxAutoBidSetup;
    @FXML private VBox vboxAutoBidActive;
    @FXML private Label lblActiveAutoBidMax;
    @FXML private Label lblBidStatus;
    @FXML private TextField txtAutoBidIncrement;
    
    // Lưu trữ các Auto Bid đang kích hoạt (auctionId -> maxAmount)
    private java.util.Map<String, Double> activeAutoBids = new java.util.HashMap<>();
    private static final double MIN_INCREMENT = 10000.0; // Bước giá tối thiểu 10k



    // --- Category Labels ---
    @FXML private Label catAll;
    @FXML private Label catElectronics;
    @FXML private Label catArt;
    @FXML private Label catVehicle;
    @FXML private Label catGeneral;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private String currentUsername;

    private List<ItemUI> allItems = new ArrayList<>();
    private ObservableList<ItemUI> filteredItems = FXCollections.observableArrayList();
    private String currentCategoryFilter = "";

    // --- Pagination & Infinite Scroll ---
    private int itemsPerPage = 8;
    private int currentlyLoadedCount = 0;
    private boolean isLoadingMore = false;

    public void setUserInfo(String username, String role) {
        this.currentUsername = username;
        lblGreeting.setText("Xin chào, " + username + "!");
    }

    @FXML
    public void initialize() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Mức giá trúng thầu hiện tại");
        if (priceChart != null) {
            priceChart.getData().add(priceSeries);
        }
        // Cấu hình bảng Giỏ hàng
        //colCartName.setCellValueFactory(new PropertyValueFactory<>("name"));
        //colCartPrice.setCellValueFactory(new PropertyValueFactory<>("priceStr"));

        // Setup filter comboboxes
        cboFilterCondition.setItems(FXCollections.observableArrayList("Tất cả", "Mới (New)", "Đã sử dụng (Used)"));
        cboFilterCondition.setValue("Tất cả");
        cboFilterCondition.setOnAction(e -> applySearchFilter());

        cboFilterPrice.setItems(FXCollections.observableArrayList("Tất cả", "Dưới 1,000,000 ₫", "1,000,000 ₫ - 5,000,000 ₫", "Trên 5,000,000 ₫"));
        cboFilterPrice.setValue("Tất cả");
        cboFilterPrice.setOnAction(e -> applySearchFilter());

        cboFilterRating.setValue("Tất cả");
        cboFilterRating.setOnAction(e -> applySearchFilter());

        // Setup History Table
        colHistName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colHistPrice.setCellValueFactory(new PropertyValueFactory<>("priceStr"));
        colHistBids.setCellValueFactory(new PropertyValueFactory<>("bidsCount"));
        colHistStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Setup Won Table
        colWonName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colWonPrice.setCellValueFactory(new PropertyValueFactory<>("priceStr"));
        colWonSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        colWonDate.setCellValueFactory(new PropertyValueFactory<>("endTime"));



        // Xử lý tìm kiếm
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applySearchFilter();
        });

        // Setup Infinite Scroll
        if (scrollPaneBrowse != null) {
            scrollPaneBrowse.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > 0.4) { // Show button after 40% scroll
                    btnBackToTop.setVisible(true);
                } else {
                    btnBackToTop.setVisible(false);
                }
                
                if (newVal.doubleValue() > 0.9 && !isLoadingMore && currentlyLoadedCount < filteredItems.size()) {
                    loadMoreProducts();
                }
            });
        }

        // Setup Responsive Grid
        if (gridItems != null) {
            gridItems.widthProperty().addListener((obs, oldVal, newVal) -> {
                updateGridResponsive(newVal.doubleValue());
            });
        }

        startBackgroundListener();
    }

    // ==========================================
    // MENU NAVIGATION
    // ==========================================

    @FXML
    public void handleSideBrowse(Event event) {
        showPage(pageBrowse);
        setActiveSidebar(sideBrowse);
    }

    @FXML
    public void handleBackToBrowse(ActionEvent event) {
        handleSideBrowse(event);
    }



    private void setActiveSidebar(Label label) {
        sideBrowse.setTextFill(javafx.scene.paint.Color.web("#707070"));
        sideBrowse.setStyle("-fx-cursor: hand;");
        sideHistory.setTextFill(javafx.scene.paint.Color.web("#707070"));
        sideHistory.setStyle("-fx-cursor: hand;");
        sideWonItems.setTextFill(javafx.scene.paint.Color.web("#707070"));
        sideWonItems.setStyle("-fx-cursor: hand;");
        
        label.setTextFill(javafx.scene.paint.Color.web("#0654ba"));
        label.setStyle("-fx-cursor: hand; -fx-font-weight: bold;");
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

    private void loadHistoryData() {
        List<ItemUI> hist = new ArrayList<>();
        for (ItemUI item : allItems) {
            // Hiển thị sản phẩm mà user đang đấu giá hoặc đã thắng
            if (currentUsername.equals(item.getWinnerUsername()) || activeAutoBids.containsKey(item.getAuctionId())) {
                hist.add(item);
            }
        }
        tableHistory.setItems(FXCollections.observableArrayList(hist));
    }

    private void loadWonData() {
        List<ItemUI> won = new ArrayList<>();
        for (ItemUI item : allItems) {
            if ("Đã kết thúc".equals(item.getStatus()) && currentUsername.equals(item.getWinnerUsername())) {
                won.add(item);
            }
        }
        tableWon.setItems(FXCollections.observableArrayList(won));
        if (btnPayAllWon != null) {
            btnPayAllWon.setDisable(won.isEmpty());
            btnPayAllWon.setOpacity(won.isEmpty() ? 0.5 : 1.0);
        }
    }

    private void showPage(Region page) {
        pageBrowse.setVisible(false);
        pageBrowse.setManaged(false);
        pageProductDetail.setVisible(false);
        pageProductDetail.setManaged(false);
        if (pageHistory != null) { pageHistory.setVisible(false); pageHistory.setManaged(false); }
        if (pageWonItems != null) { pageWonItems.setVisible(false); pageWonItems.setManaged(false); }

        page.setVisible(true);
        page.setManaged(true);
    }

    // ==========================================
    // CATEGORY FILTERS
    // ==========================================

    private void resetCategoryStyles() {
        Label[] cats = {catAll, catElectronics, catArt, catVehicle, catGeneral};
        for (Label cat : cats) {
            cat.setTextFill(javafx.scene.paint.Color.web("#707070"));
            cat.setStyle("-fx-cursor: hand;");
        }
    }

    private void setActiveCategory(Label cat, String type, String title) {
        resetCategoryStyles();
        cat.setTextFill(javafx.scene.paint.Color.web("#0654ba"));
        cat.setStyle("-fx-cursor: hand; -fx-font-weight: bold;");
        currentCategoryFilter = type;
        lblCategoryTitle.setText(title);
        applySearchFilter();
        
        // Đảm bảo đang ở trang Browse
        handleSideBrowse(null);
    }

    @FXML public void handleCategoryAll(MouseEvent event) { setActiveCategory(catAll, "", "Tất cả sản phẩm"); }
    @FXML public void handleCategoryElectronics(MouseEvent event) { setActiveCategory(catElectronics, "ELECTRONICS", "Đồ điện tử"); }
    @FXML public void handleCategoryArt(MouseEvent event) { setActiveCategory(catArt, "ART", "Nghệ thuật"); }
    @FXML public void handleCategoryVehicle(MouseEvent event) { setActiveCategory(catVehicle, "VEHICLE", "Xe cộ"); }
    @FXML public void handleCategoryGeneral(MouseEvent event) { setActiveCategory(catGeneral, "GENERAL", "Sản phẩm khác"); }

    // ==========================================
    // NETWORK & DATA
    // ==========================================

    private void startBackgroundListener() {
        new Thread(() -> {
            try {
                socket = new Socket("127.0.0.1", 9999);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Xin list ban đầu
                out.println("{\"command\":\"GET_ITEMS\"}");

                String response;
                while ((response = in.readLine()) != null) {
                    try {
                        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                        String cmd = (json.has("command") && !json.get("command").isJsonNull()) ? json.get("command").getAsString() : "";

                        if ("SET_ITEMS".equals(cmd)) {
                            JsonArray dataArray = json.getAsJsonArray("data");
                            List<ItemUI> newItems = new ArrayList<>();
                            
                            for (com.google.gson.JsonElement element : dataArray) {
                                JsonObject obj = element.getAsJsonObject();
                                String name = obj.has("name") ? obj.get("name").getAsString() : "Sản phẩm";
                                String desc = obj.has("description") ? obj.get("description").getAsString() : "";
                                double startPrice = obj.has("startingPrice") ? obj.get("startingPrice").getAsDouble() : 0.0;
                                double currentPrice = obj.has("currentHighestPrice") ? obj.get("currentHighestPrice").getAsDouble() : startPrice;
                                if (currentPrice <= 0) currentPrice = startPrice;
                                
                                String type = obj.has("type") ? obj.get("type").getAsString() : "GENERAL";
                                // Fallback type checking if server doesn't provide type field
                                if ("GENERAL".equals(type)) {
                                    if (obj.has("manufacturer") || obj.has("model")) type = "ELECTRONICS";
                                    else if (obj.has("artist") || obj.has("medium")) type = "ART";
                                    else if (obj.has("make") || obj.has("mileage")) type = "VEHICLE";
                                }
                                
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
                                if ("FINISHED".equals(serverStatus)) status = "Đã kết thúc";
                                else if ("OPEN".equals(serverStatus)) status = "Chờ mở";
                                
                                int bidsCount = obj.has("bidsCount") ? obj.get("bidsCount").getAsInt() : 0;
                                String imageUrls = obj.has("imageUrls") ? obj.get("imageUrls").getAsString() : "";

                                ItemUI itemUI = new ItemUI(
                                    name, desc, currentPrice, status, startPrice, endTime, type, sellerName, bidsCount, imageUrls
                                );
                                
                                // Lấy auctionId từ server response (nếu có)
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
                        else if ("UPDATE_PRICE".equals(cmd)) {
                            String targetId = json.has("auctionId") ? json.get("auctionId").getAsString() : "";
                            double newP = json.has("price") ? json.get("price").getAsDouble() : -1;
                            String winner = json.has("winnerUsername") ? json.get("winnerUsername").getAsString() : "";
                            
                            if (newP != -1) {
                                final double finalPrice = newP;
                                String currentTime = LocalTime.now().format(timeFormatter);
                                
                                Platform.runLater(() -> {
                                    if (selectedProductForDetail != null && targetId.equals(selectedProductForDetail.getAuctionId())) {
                                        selectedProductForDetail.setCurrentPrice(finalPrice);
                                        selectedProductForDetail.setWinnerUsername(winner);
                                        selectedProductForDetail.setBidsCount(selectedProductForDetail.getBidsCount() + 1);
                                        lblDetailPrice.setText(selectedProductForDetail.getPriceStr());
                                        if (lblDetailBidsCount != null) {
                                            lblDetailBidsCount.setText(selectedProductForDetail.getBidsCount() + " lượt");
                                        }
                                        updateBidStatusUI();
                                        
                                        if (priceSeries != null) {
                                            priceSeries.getData().add(new XYChart.Data<>(currentTime, finalPrice));
                                            if (priceSeries.getData().size() > 15) {
                                                priceSeries.getData().remove(0);
                                            }
                                        }
                                    }
                                });
                            }
                            out.println("{\"command\":\"GET_ITEMS\"}");
                        }
                        else if ("UPDATE_TIME".equals(cmd)) {
                            String newEndTime = json.has("newEndTime") ? json.get("newEndTime").getAsString() : "";
                            if (!newEndTime.isEmpty() && selectedProductForDetail != null) {
                                Platform.runLater(() -> {
                                    selectedProductForDetail.setEndTime(newEndTime);
                                });
                            }
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

    private void applySearchFilter() {
        String keyword = txtSearch.getText() != null ? txtSearch.getText().toLowerCase().trim() : "";
        String filterPrice = cboFilterPrice.getValue();
        
        List<ItemUI> result = new ArrayList<>();
        
        for (ItemUI item : allItems) {
            // Lọc theo category
            if (!currentCategoryFilter.isEmpty() && !currentCategoryFilter.equalsIgnoreCase(item.getType())) {
                continue;
            }
            // Lọc theo từ khóa
            if (!keyword.isEmpty()) {
                boolean match = (item.getName() != null && item.getName().toLowerCase().contains(keyword)) ||
                                (item.getDescription() != null && item.getDescription().toLowerCase().contains(keyword));
                if (!match) continue;
            }
            
            // Lọc theo Giá (Price)
            if (filterPrice != null && !filterPrice.equals("Tất cả")) {
                double price = item.getRawPrice();
                if (filterPrice.equals("Dưới 1,000,000 ₫") && price >= 1000000) continue;
                if (filterPrice.equals("1,000,000 ₫ - 5,000,000 ₫") && (price < 1000000 || price > 5000000)) continue;
                if (filterPrice.equals("Trên 5,000,000 ₫") && price <= 5000000) continue;
            }
            
            result.add(item);
        }
        
        filteredItems.setAll(result);
        currentlyLoadedCount = 0; // Reset for new search
        gridItems.getChildren().clear();
        loadMoreProducts();
    }

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
            updateGridResponsive(gridItems.getWidth());
        });
    }

    private void updateGridResponsive(double width) {
        if (gridItems == null || width <= 0) return;
        
        // Calculate card width based on available space
        // 4 cols if width > 1100, 3 if > 800, 2 if > 500, else 1
        int cols = (width > 1100) ? 4 : (width > 850) ? 3 : (width > 550) ? 2 : 1;
        double cardW = (width - (gridItems.getHgap() * (cols + 1))) / cols;
        
        for (javafx.scene.Node node : gridItems.getChildren()) {
            if (node instanceof VBox) {
                VBox card = (VBox) node;
                card.setPrefWidth(cardW);
                card.setMinWidth(cardW);
                card.setMaxWidth(cardW);
                
                // Adjust image height proportionally
                StackPane img = (StackPane) card.lookup("#imgContainer");
                if (img != null) {
                    img.setPrefHeight(cardW * 0.75);
                }
            }
        }
    }

    @FXML
    public void handleBackToTop() {
        if (scrollPaneBrowse != null) {
            // Smooth scroll to top
            Timeline timeline = new Timeline();
            KeyValue kv = new KeyValue(scrollPaneBrowse.vvalueProperty(), 0);
            KeyFrame kf = new KeyFrame(Duration.millis(500), kv);
            timeline.getKeyFrames().add(kf);
            timeline.play();
        }
    }

    // ==========================================
    // RENDER PRODUCT CARDS
    // ==========================================

    private void renderProductCards() {
        // This is now handled by loadMoreProducts and applySearchFilter
    }

    private VBox createProductCard(ItemUI item) {
        VBox card = new VBox(8);
        // Style mới: Bo góc 15px và Shadow mềm mại
        String baseStyle = "-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-radius: 15; -fx-padding: 0 0 15 0; -fx-background-radius: 15; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5);";
        card.setStyle(baseStyle);
        card.setPrefWidth(260);
        
        card.setOnMouseEntered(e -> { 
            card.setScaleX(1.03); 
            card.setScaleY(1.03); 
            // Đậm bóng đổ khi di chuột
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


    private void updateCardContent(VBox card, ItemUI item) {
        // Nếu thẻ chưa có nội dung (mới tạo), thì khởi tạo các Label
        if (card.getChildren().isEmpty()) {
            // Image Container
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

        // Truy xuất các thành phần để cập nhật dữ liệu
        StackPane imgContainer = (StackPane) card.lookup("#imgContainer");
        VBox infoBox = (VBox) card.getChildren().get(1);
        Label lblName = (Label) infoBox.lookup("#lblName");
        Label lblPrice = (Label) infoBox.lookup("#lblPrice");
        Label lblBids = (Label) infoBox.lookup("#lblBids");
        Label lblStatus = (Label) infoBox.lookup("#lblStatus");
        Label lblTime = (Label) infoBox.lookup("#lblTime");

        // Cập nhật text (Chỉ cập nhật nếu giá trị thay đổi để tối ưu)
        if (lblName != null) lblName.setText(item.getName());
        if (lblPrice != null) lblPrice.setText(item.getPriceStr());
        if (lblBids != null) lblBids.setText(item.getBidsCount() + " lượt đặt giá");
        
        if (lblStatus != null) {
            lblStatus.setText("Trạng thái: " + item.getStatus());
            lblStatus.setTextFill(item.getStatus().equals("Đã kết thúc") ? javafx.scene.paint.Color.RED : javafx.scene.paint.Color.web("#0654ba"));
        }
        
        if (lblTime != null) lblTime.setText(calculateTimeRemaining(item));

        // Cập nhật ảnh (Lazy loading with background loading)
        if (imgContainer != null && imgContainer.getChildren().isEmpty()) {
            if (item.getImageUrls() != null && !item.getImageUrls().isEmpty()) {
                String firstImage = item.getImageUrls().split(",")[0];
                try {
                    // backgroundLoading = true ensures UI doesn't freeze
                    javafx.scene.image.Image img = new javafx.scene.image.Image(firstImage, 400, 300, true, true, true);
                    javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView(img);
                    
                    imgView.fitWidthProperty().bind(imgContainer.widthProperty());
                    imgView.fitHeightProperty().bind(imgContainer.heightProperty());
                    imgView.setPreserveRatio(true);
                    
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
        showPage(pageProductDetail);
    }

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

    @FXML
    private void handleAdd50k() { addQuickBid(50000); }
    @FXML
    private void handleAdd100k() { addQuickBid(100000); }
    @FXML
    private void handleAdd500k() { addQuickBid(500000); }

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
                    countdownTimeline.stop();
                } else {
                    long h = diff.toHours();
                    long m = diff.toMinutesPart();
                    long s = diff.toSecondsPart();
                    lblCountdown.setText(String.format("%02d:%02d:%02d", h, m, s));
                    
                    // Nếu còn dưới 1 phút thì đổi sang màu đỏ nhấp nháy (nhấn mạnh)
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
            
            if (increment < MIN_INCREMENT) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Bước giá tối thiểu phải là " + String.format("%,.0f", MIN_INCREMENT) + " ₫!");
                return;
            }

            // --- UX: XÁC NHẬN ---
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
                    // Cập nhật UI ngay lập tức
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

    @FXML
    public void handleCancelAutoBid(ActionEvent event) {
        if (selectedProductForDetail == null) return;
        
        // Gửi lệnh hủy tới server
        JsonObject cancelRequest = new JsonObject();
        cancelRequest.addProperty("command", "CANCEL_AUTO_BID");
        cancelRequest.addProperty("auctionId", selectedProductForDetail.getAuctionId());
        cancelRequest.addProperty("username", currentUsername);
        
        if (out != null) {
            out.println(new Gson().toJson(cancelRequest));
            // Cập nhật UI
            activeAutoBids.remove(selectedProductForDetail.getAuctionId());
            updateAutoBidUI();
            showAlert(Alert.AlertType.INFORMATION, "Đã hủy", "Đã dừng Auto Bid cho sản phẩm này.");
        }
    }

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

    // UTILS
    // ==========================================

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

    private void populateMiniCart() {
        vboxMiniCartItems.getChildren().clear();
        double total = 0;
        int count = 0;

        for (ItemUI item : allItems) {
            if ("Đã kết thúc".equals(item.getStatus()) && currentUsername.equals(item.getWinnerUsername())) {
                count++;
                total += item.getRawPrice();
                
                // Tạo một dòng sản phẩm nhỏ gọn
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
                
                if (count >= 5) { // Chỉ hiện tối đa 5 món để không quá dài
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
        showAlert(Alert.AlertType.INFORMATION, "Thanh toán", "Đang chuyển hướng đến trang thanh toán an toàn...");
    }

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



    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    // ==========================================
    // DATA CLASSES
    // ==========================================

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