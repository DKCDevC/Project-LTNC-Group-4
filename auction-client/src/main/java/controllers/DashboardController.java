package controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import utils.GsonConfig;
import network.SocketManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DashboardController {

    // --- Header & Sidebar ---
    @FXML
    private Label lblGreeting;
    @FXML
    private TextField txtSearch;
    @FXML
    private Label sideBrowse;
    @FXML
    private Label sideCart;

    // --- Layout ---
    @FXML
    private StackPane contentArea;
    @FXML
    private VBox pageBrowse;
    @FXML
    private VBox pageCart;
    @FXML
    private VBox pageCheckout;
    @FXML
    private ScrollPane scrollCart;
    @FXML
    private ScrollPane scrollCheckout;

    // --- Page: Browse ---
    @FXML
    private Label lblCategoryTitle;
    @FXML
    private FlowPane gridItems;
    @FXML
    private ComboBox<String> cboFilterCondition;
    @FXML
    private ComboBox<String> cboFilterPrice;
    @FXML
    private ComboBox<String> cboFilterRating;
    @FXML
    private ComboBox<String> cboSort;

    // --- Page: Cart ---
    @FXML
    private Label lblCartTotal;
    @FXML
    private VBox cartListContainer;
    @FXML
    private Label lblCartItemsSubtotal;

    // --- Mini Cart & Overlay ---
    @FXML
    private Label btnMiniCart;
    @FXML
    private Label lblCartBadge;
    @FXML
    private VBox miniCartPreview;
    @FXML
    private VBox miniCartList;
    @FXML
    private Label lblMiniCartTotal;
    @FXML
    private StackPane cartOverlay;
    @FXML
    private Label lblOverlayName;
    @FXML
    private Label lblOverlayPrice;

    // --- Page: Product Detail ---
    @FXML
    private VBox pageProductDetail;
    @FXML
    private Label lblDetailName;
    @FXML
    private Label lblDetailPrice;
    @FXML
    private Label lblBuyNowPrice;
    @FXML
    private Label lblDetailCondition;
    @FXML
    private ImageView imgDetail;
    @FXML
    private VBox boxAuctionActions;
    @FXML
    private VBox boxBuyNowActions;
    @FXML
    private TextField txtBidAmount;
    @FXML
    private TextField txtAutoBidMax;
    @FXML
    private TextField txtAutoBidIncrement;
    @FXML
    private Label lblAutoBidStatus;
    @FXML
    private LineChart<String, Number> bidChart;
    @FXML
    private VBox boxBidChart;

    private ItemUI selectedProductForDetail;
    private XYChart.Series<String, Number> chartSeries = new XYChart.Series<>();

    // --- Page: Checkout ---
    @FXML
    private ToggleGroup paymentGroup;
    @FXML
    private RadioButton radCOD;
    @FXML
    private RadioButton radCard;
    @FXML
    private VBox boxCardInfo;
    @FXML
    private TextField txtAddress;
    @FXML
    private TextField txtPhone;
    @FXML
    private Label lblCheckoutTotal;

    // --- Notifications & Watchlist ---
    @FXML
    private StackPane notifyContainer;
    @FXML
    private StackPane cartContainer;
    @FXML
    private VBox notifyPreview;
    @FXML
    private VBox notifyList;
    @FXML
    private Label lblNotifyBadge;
    @FXML
    private Label btnWatchlist;
    @FXML
    private VBox pageWatchlist;
    @FXML
    private ScrollPane scrollWatchlist;
    @FXML
    private FlowPane gridWatchlist;

    private List<JsonObject> notifications = new ArrayList<>();
    private List<ItemUI> watchlistItems = new ArrayList<>();
    @FXML
    private Label lblCheckoutFinal;
    @FXML
    private ScrollPane scrollDetail;

    // --- Category Labels ---
    @FXML
    private Label catAll;
    @FXML
    private Label catElectronics;
    @FXML
    private Label catArt;
    @FXML
    private Label catVehicle;
    @FXML
    private Label catGeneral;

    private String currentUsername;
    private String userRole;

    private List<ItemUI> allItems = new ArrayList<>();
    private ObservableList<ItemUI> filteredItems = FXCollections.observableArrayList();
    private ObservableList<CartItem> cartItems = FXCollections.observableArrayList();

    private String currentCategoryFilter = "";

    // Timer for product countdowns
    private Timeline countdownTimeline;
    private final ConcurrentHashMap<String, Label> timerLabels = new ConcurrentHashMap<>();

    public void setUserInfo(String username, String role) {
        this.currentUsername = username;
        this.userRole = role;
        lblGreeting.setText("Xin chào, " + username + "!");
    }

    @FXML
    public void initialize() {
        // Setup filter comboboxes
        cboFilterCondition.setItems(FXCollections.observableArrayList("Tất cả", "Mới (New)", "Đã sử dụng (Used)"));
        cboFilterCondition.setValue("Tất cả");
        cboFilterCondition.setOnAction(e -> applySearchFilter());

        cboFilterPrice.setItems(FXCollections.observableArrayList("Tất cả", "Dưới 1,000,000 ₫",
                "1,000,000 ₫ - 5,000,000 ₫", "Trên 5,000,000 ₫"));
        cboFilterPrice.setValue("Tất cả");
        cboFilterPrice.setOnAction(e -> applySearchFilter());

        cboFilterRating.setItems(FXCollections.observableArrayList("Tất cả", "4 Sao trở lên", "5 Sao"));
        cboFilterRating.setValue("Tất cả");
        cboFilterRating.setOnAction(e -> applySearchFilter());

        cboSort.setItems(FXCollections.observableArrayList("Mặc định", "Mới nhất", "Giá: Thấp đến Cao", "Giá: Cao đến Thấp"));
        cboSort.setValue("Mặc định");
        cboSort.setOnAction(e -> applySearchFilter());

        // Mini Cart Hover Logic
        cartContainer.setOnMouseEntered(e -> {
            miniCartPreview.setVisible(true);
            miniCartPreview.setManaged(true);
            renderMiniCart();
        });

        cartContainer.setOnMouseExited(e -> {
            miniCartPreview.setVisible(false);
            miniCartPreview.setManaged(false);
        });

        // Notifications Hover Logic
        notifyContainer.setOnMouseEntered(e -> {
            notifyPreview.setVisible(true);
            notifyPreview.setManaged(true);
            lblNotifyBadge.setVisible(false);
            lblNotifyBadge.setText("0");
        });

        notifyContainer.setOnMouseExited(e -> {
            notifyPreview.setVisible(false);
            notifyPreview.setManaged(false);
        });

        // Toggle card info
        paymentGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            boolean newVal = (newV == radCard);
            boxCardInfo.setVisible(newVal);
            boxCardInfo.setManaged(newVal);
        });

        // Search listener
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> applySearchFilter());

        // Chart setup
        bidChart.getData().add(chartSeries);

        registerServerListeners();
        startCountdownTimer();

        // Initial data fetch
        SocketManager.getInstance().send(new Command("GET_ITEMS"));
        SocketManager.getInstance().send(new Command("GET_WATCHLIST"));
    }

    private void registerServerListeners() {
        SocketManager sm = SocketManager.getInstance();
        sm.addListener("SET_ITEMS", this::handleSetItems);
        sm.addListener("ITEM_DETAIL", this::handleItemDetail);
        sm.addListener("UPDATE_PRICE", this::handleUpdatePrice);
        sm.addListener("UPDATE_TIME", this::handleUpdateTime);
        sm.addListener("AUCTION_FINISHED", this::handleAuctionFinished);
        sm.addListener("KICK", this::handleKick);
        sm.addListener("NOTIFY", this::handleNotify);
        sm.addListener("SET_WATCHLIST", this::handleSetWatchlist);
    }

    // Helper for simple commands
    private static class Command {
        String command;

        Command(String c) {
            this.command = c;
        }
    }

    private void startCountdownTimer() {
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateAllTimers()));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateAllTimers() {
        LocalDateTime now = LocalDateTime.now();
        timerLabels.forEach((id, label) -> {
            // Find item by ID
            Optional<ItemUI> itemOpt = allItems.stream().filter(it -> it.getId().equals(id)).findFirst();
            if (itemOpt.isPresent()) {
                ItemUI item = itemOpt.get();
                try {
                    LocalDateTime end = LocalDateTime.parse(item.getEndTime());
                    java.time.Duration d = java.time.Duration.between(now, end);
                    if (d.isNegative()) {
                        label.setText("Đã kết thúc");
                        label.setStyle("-fx-text-fill: #999;");
                    } else {
                        long h = d.toHours();
                        long m = d.toMinutesPart();
                        long s = d.toSecondsPart();
                        label.setText(String.format("Hết hạn sau: %02d:%02d:%02d", h, m, s));
                    }
                } catch (Exception ex) {
                    label.setText("N/A");
                }
            }
        });
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
    public void handleSideCart(Event event) {
        showPage(scrollCart);
        setActiveSidebar(sideCart);
        renderFullCart();
    }

    private void setActiveSidebar(Label label) {
        sideBrowse.setTextFill(javafx.scene.paint.Color.web("#707070"));
        sideBrowse.setStyle("-fx-cursor: hand;");
        sideCart.setTextFill(javafx.scene.paint.Color.web("#707070"));
        sideCart.setStyle("-fx-cursor: hand;");

        label.setTextFill(javafx.scene.paint.Color.web("#0654ba"));
        label.setStyle("-fx-cursor: hand; -fx-font-weight: bold;");
    }

    // ==========================================
    // CATEGORY FILTERS
    // ==========================================

    private void resetCategoryStyles() {
        Label[] cats = { catAll, catElectronics, catArt, catVehicle, catGeneral };
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
        handleSideBrowse(null);
    }

    @FXML
    public void handleCategoryAll(MouseEvent event) {
        setActiveCategory(catAll, "", "Tất cả sản phẩm");
    }

    @FXML
    public void handleCategoryElectronics(MouseEvent event) {
        setActiveCategory(catElectronics, "ELECTRONICS", "Đồ điện tử");
    }

    @FXML
    public void handleCategoryArt(MouseEvent event) {
        setActiveCategory(catArt, "ART", "Nghệ thuật");
    }

    @FXML
    public void handleCategoryVehicle(MouseEvent event) {
        setActiveCategory(catVehicle, "VEHICLE", "Xe cộ");
    }

    @FXML
    public void handleCategoryGeneral(MouseEvent event) {
        setActiveCategory(catGeneral, "GENERAL", "Sản phẩm khác");
    }

    // ==========================================
    // NETWORK & DATA
    // ==========================================

    private void handleSetItems(JsonObject json) {
        JsonArray dataArray = json.getAsJsonArray("data");
        List<ItemUI> newItems = new ArrayList<>();
        for (JsonElement element : dataArray) {
            JsonObject obj = element.getAsJsonObject();
            newItems.add(parseItem(obj));
        }
        Platform.runLater(() -> {
            allItems = newItems;
            applySearchFilter();
        });
    }

    private void handleItemDetail(JsonObject json) {
        Platform.runLater(() -> {
            try {
                // Clear old data first
                lblDetailName.setText("");
                lblDetailPrice.setText("");
                if (lblBuyNowPrice != null)
                    lblBuyNowPrice.setText("");

                if (json.has("name") && !json.get("name").isJsonNull()) {
                    lblDetailName.setText(json.get("name").getAsString());
                }

                double currentPrice = json.has("currentPrice") ? json.get("currentPrice").getAsDouble() : 0;
                lblDetailPrice.setText(String.format("%,.0f ₫", currentPrice));
                if (lblBuyNowPrice != null) {
                    lblBuyNowPrice.setText(String.format("%,.0f ₫", currentPrice));
                }

                String saleType = json.has("saleType") && !json.get("saleType").isJsonNull()
                        ? json.get("saleType").getAsString()
                        : "AUCTION";

                boolean isAuction = "AUCTION".equals(saleType);
                System.out.println(">>> Detail view: saleType=" + saleType + ", isAuction=" + isAuction);

                boxAuctionActions.setVisible(isAuction);
                boxAuctionActions.setManaged(isAuction);
                boxBuyNowActions.setVisible(!isAuction);
                boxBuyNowActions.setManaged(!isAuction);
                
                if (boxBidChart != null) {
                    boxBidChart.setVisible(isAuction);
                    boxBidChart.setManaged(isAuction);
                }

                // Load images
                if (json.has("imageUrls") && !json.get("imageUrls").isJsonNull()) {
                    String urls = json.get("imageUrls").getAsString();
                    if (!urls.isEmpty()) {
                        String first = urls.split(",")[0].trim();
                        try {
                            imgDetail.setImage(new Image(first, true));
                        } catch (Exception e) {
                            imgDetail.setImage(null);
                        }
                    } else {
                        imgDetail.setImage(null);
                    }
                }
            } catch (Exception ex) {
                System.err.println("Error parsing item detail: " + ex.getMessage());
                ex.printStackTrace();
            }

            // Update chart
            chartSeries.getData().clear();
            JsonArray history = json.getAsJsonArray("bidHistory");
            if (history != null) {
                for (JsonElement e : history) {
                    JsonObject pt = e.getAsJsonObject();
                    double amt = Double.parseDouble(pt.get("amount").getAsString());
                    String time = pt.get("time").getAsString();
                    if (time.length() >= 19) {
                        chartSeries.getData().add(new XYChart.Data<>(time.substring(11, 19), amt));
                    }
                }
            }
            if (chartSeries.getData().size() > 50) {
                chartSeries.getData().remove(0, chartSeries.getData().size() - 50);
            }
        });
    }

    private void handleUpdatePrice(JsonObject json) {
        String itemId = json.has("itemId") ? json.get("itemId").getAsString() : "";

        // If itemId is empty, it's a signal to reload the entire list (e.g. new item
        // added)
        if (itemId.isEmpty()) {
            Platform.runLater(() -> SocketManager.getInstance().send(new Command("GET_ITEMS")));
            return;
        }

        double price = json.has("price") ? json.get("price").getAsDouble() : 0;

        Platform.runLater(() -> {
            // Update in allItems list
            for (ItemUI item : allItems) {
                if (item.getId().equals(itemId)) {
                    item.setRawPrice(price);
                    item.setPriceStr(String.format("%,.0f ₫", price));
                    break;
                }
            }
            applySearchFilter();

            // If it's the selected item, update detail view and chart
            if (selectedProductForDetail != null && selectedProductForDetail.getId().equals(itemId)) {
                lblDetailPrice.setText(String.format("%,.0f ₫", price));
                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                chartSeries.getData().add(new XYChart.Data<>(time, price));
                if (chartSeries.getData().size() > 50)
                    chartSeries.getData().remove(0);
            }

            if (json.has("message")) {
                // Optional notification
            }
        });
    }

    private void handleUpdateTime(JsonObject json) {
        String itemId = json.get("itemId").getAsString();
        String newEnd = json.get("newEndTime").getAsString();
        String msg = json.has("message") ? json.get("message").getAsString() : "";

        Platform.runLater(() -> {
            for (ItemUI item : allItems) {
                if (item.getId().equals(itemId)) {
                    item.setEndTime(newEnd);
                    break;
                }
            }
            if (!msg.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Gia hạn", msg);
            }
        });
    }

    private void handleAuctionFinished(JsonObject json) {
        String msg = json.get("message").getAsString();
        Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "Kết thúc", msg));
        SocketManager.getInstance().send(new Command("GET_ITEMS"));
    }

    private void handleKick(JsonObject json) {
        String msg = json.get("message").getAsString();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Bị Kick");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
            handleLogout(null);
        });
    }

    private ItemUI parseItem(JsonObject obj) {
        String id = obj.has("id") ? obj.get("id").getAsString() : "unknown";
        String name = obj.has("name") ? obj.get("name").getAsString() : "Sản phẩm không tên";
        String desc = obj.has("description") ? obj.get("description").getAsString() : "";
        double startPrice = obj.has("startingPrice") ? obj.get("startingPrice").getAsDouble() : 0;
        double currentPrice = obj.has("currentHighestPrice") ? obj.get("currentHighestPrice").getAsDouble()
                : startPrice;
        String type = obj.has("type") ? obj.get("type").getAsString() : "GENERAL";
        String endTime = obj.has("endTime") ? obj.get("endTime").getAsString() : LocalDateTime.now().toString();
        String startTime = obj.has("startTime") ? obj.get("startTime").getAsString() : LocalDateTime.now().toString();
        String saleType = obj.has("saleType") ? obj.get("saleType").getAsString() : "AUCTION";
        String imageUrls = obj.has("imageUrls") ? obj.get("imageUrls").getAsString() : "";
        String sellerName = "Unknown";
        if (obj.has("seller") && !obj.get("seller").isJsonNull()) {
            sellerName = obj.getAsJsonObject("seller").get("username").getAsString();
        }

        ItemUI item = new ItemUI(id, name, desc, currentPrice, "RUNNING", startPrice, endTime, type, sellerName);
        item.setStartTime(startTime);
        item.setSaleType(saleType);
        item.setImageUrls(imageUrls);
        return item;
    }

    @FXML
    public void handleSearch() {
        applySearchFilter();
    }

    private void applySearchFilter() {
        String keyword = txtSearch.getText() != null ? txtSearch.getText().toLowerCase().trim() : "";
        String filterCond = cboFilterCondition.getValue();
        String filterPrice = cboFilterPrice.getValue();
        String sortOption = cboSort.getValue();

        List<ItemUI> result = new ArrayList<>();
        for (ItemUI item : allItems) {
            if (!currentCategoryFilter.isEmpty() && !currentCategoryFilter.equalsIgnoreCase(item.getType()))
                continue;
            if (!keyword.isEmpty() && !item.getName().toLowerCase().contains(keyword))
                continue;

            if (filterPrice != null && !filterPrice.equals("Tất cả")) {
                double price = item.getRawPrice();
                if (filterPrice.equals("Dưới 1,000,000 ₫") && price >= 1000000)
                    continue;
                if (filterPrice.equals("1,000,000 ₫ - 5,000,000 ₫") && (price < 1000000 || price > 5000000))
                    continue;
                if (filterPrice.equals("Trên 5,000,000 ₫") && price <= 5000000)
                    continue;
            }
            result.add(item);
        }

        // Apply Sorting
        if (sortOption != null) {
            if (sortOption.equals("Mới nhất")) {
                result.sort((a, b) -> b.getStartTime().compareTo(a.getStartTime()));
            } else if (sortOption.equals("Giá: Thấp đến Cao")) {
                result.sort((a, b) -> Double.compare(a.getRawPrice(), b.getRawPrice()));
            } else if (sortOption.equals("Giá: Cao đến Thấp")) {
                result.sort((a, b) -> Double.compare(b.getRawPrice(), a.getRawPrice()));
            }
        }

        filteredItems.setAll(result);
        renderProductCards();
    }

    private void renderProductCards() {
        gridItems.getChildren().clear();
        timerLabels.clear();
        for (ItemUI item : filteredItems) {
            gridItems.getChildren().add(createProductCard(item));
        }
        updateAllTimers();
    }

    private VBox createProductCard(ItemUI item) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-padding: 0 0 15 0; -fx-background-radius: 8; -fx-cursor: hand;");
        card.setPrefWidth(260);

        card.setOnMouseEntered(e -> {
            card.setScaleX(1.02);
            card.setScaleY(1.02);
        });
        card.setOnMouseExited(e -> {
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });
        card.setOnMouseClicked(e -> showProductDetail(item));

        // Async Image Load
        ImageView img = new ImageView();
        img.setFitWidth(260);
        img.setFitHeight(200);
        img.setPreserveRatio(true);
        if (!item.getImageUrls().isEmpty()) {
            String first = item.getImageUrls().split(",")[0].trim();
            try {
                System.out.println(">>> Loading card image: " + first);
                img.setImage(new Image(first, true));
            } catch (Exception e) {
                System.out.println(">>> Failed to load image: " + first + " - " + e.getMessage());
            }
        }

        VBox infoBox = new VBox(5);
        infoBox.setStyle("-fx-padding: 10 15;");

        Label lblName = new Label(item.getName());
        lblName.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 15));
        lblName.setWrapText(true);
        lblName.setMaxHeight(40);

        Label lblPrice = new Label(item.getPriceStr());
        lblPrice.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 18));
        lblPrice.setTextFill(javafx.scene.paint.Color.web("#e53238"));

        Label lblTimer = new Label("Hết hạn sau: --:--:--");
        lblTimer.setStyle("-fx-font-size: 12px; -fx-text-fill: #707070;");
        timerLabels.put(item.getId(), lblTimer);

        Label lblSaleType = new Label("AUCTION".equals(item.getSaleType()) ? "Đấu giá" : "Mua ngay");
        lblSaleType.setStyle(
                "-fx-font-size: 11px; -fx-background-color: #eee; -fx-padding: 2 5; -fx-background-radius: 3;");

        infoBox.getChildren().addAll(lblName, lblPrice, lblTimer, lblSaleType);
        card.getChildren().addAll(img, infoBox);
        return card;
    }

    private void showProductDetail(ItemUI item) {
        selectedProductForDetail = item;
        showPage(scrollDetail);

        JsonObject req = new JsonObject();
        req.addProperty("command", "GET_ITEM_DETAIL");
        req.addProperty("itemId", item.getId());
        SocketManager.getInstance().send(req);
    }

    @FXML
    public void handlePlaceBid() {
        if (selectedProductForDetail == null)
            return;
        String amtStr = txtBidAmount.getText().trim();
        if (amtStr.isEmpty())
            return;

        try {
            double amount = Double.parseDouble(amtStr);
            JsonObject req = new JsonObject();
            req.addProperty("command", "BID");
            JsonObject data = new JsonObject();
            data.addProperty("itemId", selectedProductForDetail.getId());
            data.addProperty("amount", amount);
            req.add("data", data);
            SocketManager.getInstance().send(req);
            txtBidAmount.clear();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập số hợp lệ.");
        }
    }

    @FXML
    public void handleRegisterAutoBid() {
        if (selectedProductForDetail == null)
            return;
        String maxStr = txtAutoBidMax.getText().trim();
        String incStr = txtAutoBidIncrement.getText().trim();
        if (maxStr.isEmpty() || incStr.isEmpty())
            return;

        try {
            double max = Double.parseDouble(maxStr);
            double inc = Double.parseDouble(incStr);
            JsonObject req = new JsonObject();
            req.addProperty("command", "REGISTER_AUTOBID");
            JsonObject data = new JsonObject();
            data.addProperty("itemId", selectedProductForDetail.getId());
            data.addProperty("maxBid", max);
            data.addProperty("increment", inc);
            req.add("data", data);
            SocketManager.getInstance().send(req);
            lblAutoBidStatus.setText(
                    "Đã đăng ký: Max " + String.format("%,.0f", max) + " / Bước " + String.format("%,.0f", inc));
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập số hợp lệ.");
        }
    }

    @FXML
    public void handleBuyNow() {
        if (selectedProductForDetail == null)
            return;
        String firstImg = selectedProductForDetail.getImageUrls().split(",")[0].trim();
        cartItems.add(new CartItem(selectedProductForDetail.getName(), selectedProductForDetail.getRawPrice(), firstImg));
        updateCartTotal();
        handleSideCart(null);
    }

    @FXML
    public void handleAddToCartFromDetail() {
        if (selectedProductForDetail != null) {
            String firstImg = selectedProductForDetail.getImageUrls().split(",")[0].trim();
            cartItems.add(new CartItem(selectedProductForDetail.getName(), selectedProductForDetail.getRawPrice(), firstImg));
            updateCartTotal();
            lblOverlayName.setText(selectedProductForDetail.getName());
            lblOverlayPrice.setText(selectedProductForDetail.getPriceStr());
            cartOverlay.setVisible(true);
        }
    }

    @FXML
    public void handleCloseCartOverlay() {
        cartOverlay.setVisible(false);
    }

    private void renderMiniCart() {
        miniCartList.getChildren().clear();
        double total = 0;
        for (CartItem item : cartItems) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            ImageView img = new ImageView();
            img.setFitWidth(40);
            img.setFitHeight(40);
            img.setPreserveRatio(true);
            if (!item.getImageUrl().isEmpty()) {
                img.setImage(new Image(item.getImageUrl(), true));
            }

            Label name = new Label(item.getName());
            name.setPrefWidth(120);
            Label price = new Label(item.getPriceStr());
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(img, name, spacer, price);
            miniCartList.getChildren().add(row);
            total += item.getRawPrice();
        }
        lblMiniCartTotal.setText(String.format("%,.0f ₫", total));
    }

    private void renderFullCart() {
        cartListContainer.getChildren().clear();
        double total = 0;
        for (CartItem item : cartItems) {
            HBox card = new HBox(20);
            card.setStyle(
                    "-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12; -fx-border-color: #E0E0E0; -fx-border-radius: 12;");
            card.setAlignment(Pos.CENTER_LEFT);

            ImageView img = new ImageView();
            img.setFitWidth(100);
            img.setFitHeight(100);
            img.setPreserveRatio(true);
            if (!item.getImageUrl().isEmpty()) {
                img.setImage(new Image(item.getImageUrl(), true));
            }

            VBox info = new VBox(8);
            Label name = new Label(item.getName());
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #191919;");
            name.setWrapText(true);
            name.setPrefWidth(400);

            Button btnRemove = new Button("Xóa");
            btnRemove.setStyle(
                    "-fx-text-fill: #0654ba; -fx-background-color: transparent; -fx-underline: true; -fx-cursor: hand;");
            btnRemove.setOnAction(e -> {
                cartItems.remove(item);
                renderFullCart();
                updateCartTotal();
            });

            info.getChildren().addAll(name, btnRemove);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label price = new Label(item.getPriceStr());
            price.setStyle("-fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: #191919;");

            card.getChildren().addAll(img, info, spacer, price);
            cartListContainer.getChildren().add(card);
            total += item.getRawPrice();
        }
        lblCartTotal.setText(String.format("%,.0f ₫", total));
        lblCartItemsSubtotal.setText(String.format("%,.0f ₫", total));
    }

    private void updateCartTotal() {
        lblCartBadge.setText(String.valueOf(cartItems.size()));
        lblCartBadge.setVisible(cartItems.size() > 0);
    }

    @FXML
    public void handleProceedToCheckout() {
        if (cartItems.isEmpty())
            return;
        double total = cartItems.stream().mapToDouble(CartItem::getRawPrice).sum();
        lblCheckoutTotal.setText(String.format("%,.0f ₫", total));
        lblCheckoutFinal.setText(String.format("%,.0f ₫", total));
        showPage(scrollCheckout);
    }

    @FXML
    public void handleBackToCart() {
        showPage(scrollCart);
    }

    @FXML
    public void handlePlaceOrder() {
        if (txtAddress.getText().trim().isEmpty() || txtPhone.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập đủ thông tin giao hàng.");
            return;
        }
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đơn hàng đã được đặt!");
        cartItems.clear();
        updateCartTotal();
        showPage(pageBrowse);
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            if (countdownTimeline != null)
                countdownTimeline.stop();

            SocketManager sm = SocketManager.getInstance();
            sm.clearListeners();
            sm.close();

            if (lblGreeting.getScene() == null)
                return;
            Stage stage = (Stage) lblGreeting.getScene().getWindow();
            if (stage == null)
                return;

            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            stage.setScene(new Scene(root));
            stage.setTitle("Hệ thống đấu giá eBid - Đăng nhập");
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
    // NOTIFICATIONS & WATCHLIST
    // ==========================================

    @FXML
    public void handleToggleNotify(MouseEvent event) {
        boolean visible = !notifyPreview.isVisible();
        notifyPreview.setVisible(visible);
        notifyPreview.setManaged(visible);
        if (visible) {
            lblNotifyBadge.setVisible(false);
            lblNotifyBadge.setText("0");
        }
    }

    @FXML
    public void handleClearNotifications(ActionEvent event) {
        notifications.clear();
        notifyList.getChildren().clear();
        lblNotifyBadge.setVisible(false);
        lblNotifyBadge.setText("0");
    }

    private void handleNotify(JsonObject json) {
        Platform.runLater(() -> {
            notifications.add(0, json);
            updateNotifyUI();

            // Increment badge if preview is closed
            if (!notifyPreview.isVisible()) {
                int val = 0;
                try {
                    val = Integer.parseInt(lblNotifyBadge.getText());
                } catch (Exception ignored) {
                }
                lblNotifyBadge.setText(String.valueOf(val + 1));
                lblNotifyBadge.setVisible(true);
            }
        });
    }

    private void updateNotifyUI() {
        notifyList.getChildren().clear();
        for (JsonObject n : notifications) {
            VBox box = new VBox(5);
            box.setStyle(
                    "-fx-padding: 8; -fx-background-color: #f9f9f9; -fx-background-radius: 5; -fx-border-color: #eee; -fx-border-radius: 5;");

            String type = n.has("type") ? n.get("type").getAsString() : "INFO";
            Label title = new Label(
                    type.equals("OUTBID") ? "Đã Bị vượt mặt!" : (type.equals("EXPIRY") ? "Sắp hết giờ!" : "Thông báo"));
            title.setStyle(
                    "-fx-font-weight: bold; -fx-text-fill: " + (type.equals("OUTBID") ? "#dd1e31" : "#f5af02") + ";");

            Label msg = new Label(n.has("message") ? n.get("message").getAsString() : "");
            msg.setWrapText(true);

            String timeStr = "Vừa xong";
            if (n.has("time")) {
                try {
                    timeStr = n.get("time").getAsString().split("T")[1].substring(0, 5);
                } catch (Exception ignored) {
                }
            }
            Label time = new Label(timeStr);
            time.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");

            box.getChildren().addAll(title, msg, time);
            box.setCursor(javafx.scene.Cursor.HAND);
            box.setOnMouseClicked(e -> {
                if (n.has("itemId")) {
                    requestItemDetail(n.get("itemId").getAsString());
                }
                notifyPreview.setVisible(false);
            });

            notifyList.getChildren().add(box);
        }
    }

    @FXML
    public void handleShowWatchlist(MouseEvent event) {
        showPage(scrollWatchlist);
        JsonObject req = new JsonObject();
        req.addProperty("command", "GET_WATCHLIST");
        SocketManager.getInstance().send(req);
    }

    private void handleSetWatchlist(JsonObject json) {
        Platform.runLater(() -> {
            JsonArray arr = json.getAsJsonArray("data");
            watchlistItems.clear();
            gridWatchlist.getChildren().clear();
            for (int i = 0; i < arr.size(); i++) {
                ItemUI item = parseItem(arr.get(i).getAsJsonObject());
                watchlistItems.add(item);
                gridWatchlist.getChildren().add(createProductCard(item));
            }
        });
    }

    private void requestItemDetail(String itemId) {
        JsonObject req = new JsonObject();
        req.addProperty("command", "GET_ITEM_DETAIL");
        req.addProperty("itemId", itemId);
        SocketManager.getInstance().send(req);
    }

    @FXML
    public void handleToggleWatch(ActionEvent event) {
        if (selectedProductForDetail == null)
            return;
        String itemId = selectedProductForDetail.getId();

        JsonObject req = new JsonObject();
        req.addProperty("command", "WATCHLIST_ADD");
        req.addProperty("itemId", itemId);
        SocketManager.getInstance().send(req);

        showAlert(Alert.AlertType.INFORMATION, "Watchlist", "Đã thêm vào danh sách theo dõi!");
    }

    private void showPage(javafx.scene.Node page) {
        pageBrowse.setVisible(false);
        pageBrowse.setManaged(false);
        if (scrollCart != null) {
            scrollCart.setVisible(false);
            scrollCart.setManaged(false);
        }
        if (scrollCheckout != null) {
            scrollCheckout.setVisible(false);
            scrollCheckout.setManaged(false);
        }
        if (scrollWatchlist != null) {
            scrollWatchlist.setVisible(false);
            scrollWatchlist.setManaged(false);
        }
        if (scrollDetail != null) {
            scrollDetail.setVisible(false);
            scrollDetail.setManaged(false);
        }

        page.setVisible(true);
        page.setManaged(true);
    }

    // ==========================================
    // INNER CLASSES
    // ==========================================

    public static class ItemUI {
        private final String id;
        private String name;
        private String description;
        private double rawPrice;
        private String priceStr;
        private String status;
        private double startingPrice;
        private String endTime;
        private String startTime;
        private String type;
        private String sellerName;
        private String saleType = "AUCTION";
        private String imageUrls = "";

        public ItemUI(String id, String name, String description, double rawPrice, String status, double startingPrice,
                String endTime, String type, String sellerName) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.rawPrice = rawPrice;
            this.priceStr = String.format("%,.0f ₫", rawPrice);
            this.status = status;
            this.startingPrice = startingPrice;
            this.endTime = endTime;
            this.type = type;
            this.sellerName = sellerName;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getPriceStr() {
            return priceStr;
        }

        public void setPriceStr(String p) {
            this.priceStr = p;
        }

        public String getStatus() {
            return status;
        }

        public double getRawPrice() {
            return rawPrice;
        }

        public void setRawPrice(double p) {
            this.rawPrice = p;
        }

        public double getStartingPrice() {
            return startingPrice;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String e) {
            this.endTime = e;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String s) {
            this.startTime = s;
        }

        public String getType() {
            return type;
        }

        public String getSellerName() {
            return sellerName;
        }

        public String getSaleType() {
            return saleType;
        }

        public void setSaleType(String s) {
            this.saleType = s;
        }

        public String getImageUrls() {
            return imageUrls;
        }

        public void setImageUrls(String i) {
            this.imageUrls = i;
        }
    }

    public static class CartItem {
        private final String name;
        private final double rawPrice;
        private final String imageUrl;

        public CartItem(String name, double rawPrice, String imageUrl) {
            this.name = name;
            this.rawPrice = rawPrice;
            this.imageUrl = imageUrl;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public String getName() {
            return name;
        }

        public String getPriceStr() {
            return String.format("%,.0f ₫", rawPrice);
        }

        public double getRawPrice() {
            return rawPrice;
        }
    }
}
