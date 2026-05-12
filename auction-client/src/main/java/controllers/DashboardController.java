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
import network.AuctionChartGUI;

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
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DashboardController {

    // --- Header & Sidebar ---
    @FXML private Label lblGreeting;
    @FXML private TextField txtSearch;
    @FXML private Label sideBrowse;

    // --- Layout ---
    @FXML private StackPane contentArea;
    @FXML private VBox pageBrowse;

    // --- Page: Browse ---
    @FXML private Label lblCategoryTitle;
    @FXML private FlowPane gridItems;
    @FXML private ComboBox<String> cboFilterCondition;
    @FXML private ComboBox<String> cboFilterPrice;
    @FXML private ComboBox<String> cboFilterRating;



    // --- Page: Product Detail ---
    @FXML private ScrollPane pageProductDetail;
    @FXML private Label lblDetailName;
    @FXML private Label lblDetailPrice;
    @FXML private TextField txtBidAmount;
    @FXML private TextField txtAutoBidMax;
    @FXML private LineChart<String, Number> priceChart;
    private XYChart.Series<String, Number> priceSeries;
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private ItemUI selectedProductForDetail;



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

        cboFilterRating.setItems(FXCollections.observableArrayList("Tất cả", "4 Sao trở lên", "5 Sao"));
        cboFilterRating.setValue("Tất cả");
        cboFilterRating.setOnAction(e -> applySearchFilter());



        // Xử lý tìm kiếm
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applySearchFilter();
        });

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
        
        label.setTextFill(javafx.scene.paint.Color.web("#0654ba"));
        label.setStyle("-fx-cursor: hand; -fx-font-weight: bold;");
    }

    private void showPage(Region page) {
        pageBrowse.setVisible(false);
        pageBrowse.setManaged(false);
        pageProductDetail.setVisible(false);
        pageProductDetail.setManaged(false);

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
                                            endTime = String.format("%04d-%02d-%02d", d.get("year").getAsInt(), d.get("month").getAsInt(), d.get("day").getAsInt());
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

                                newItems.add(new ItemUI(
                                    name, desc, currentPrice, "Đang đấu giá", startPrice, endTime, type, sellerName
                                ));
                            }

                            Platform.runLater(() -> {
                                allItems = newItems;
                                applySearchFilter();
                            });
                        }
                        else if ("UPDATE_PRICE".equals(cmd)) {
                            String msg = json.has("message") ? json.get("message").getAsString() : "";
                            Pattern pattern = Pattern.compile("giá (\\d+(\\.\\d+)?)");
                            Matcher matcher = pattern.matcher(msg);
                            
                            if (matcher.find()) {
                                double newPrice = Double.parseDouble(matcher.group(1));
                                String currentTime = LocalTime.now().format(timeFormatter);
                                
                                Platform.runLater(() -> {
                                    if (priceSeries != null) {
                                        priceSeries.getData().add(new XYChart.Data<>(currentTime, newPrice));
                                        if (priceSeries.getData().size() > 15) {
                                            priceSeries.getData().remove(0);
                                        }
                                    }
                                });
                            }
                            out.println("{\"command\":\"GET_ITEMS\"}");
                        }
                    } catch (Exception ex) {
                        System.out.println("Lỗi xử lý JSON: " + ex.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println("Lỗi kết nối Socket tại Dashboard.");
                e.printStackTrace();
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
        renderProductCards();
    }

    // ==========================================
    // RENDER PRODUCT CARDS
    // ==========================================

    private void renderProductCards() {
        gridItems.getChildren().clear();
        for (ItemUI item : filteredItems) {
            VBox card = new VBox(8);
            card.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-padding: 0 0 15 0; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2);");
            card.setPrefWidth(260);
            
            // eBay Hover Effect: Scale up 5%
            card.setOnMouseEntered(e -> {
                card.setScaleX(1.05);
                card.setScaleY(1.05);
                card.toFront(); 
            });
            card.setOnMouseExited(e -> {
                card.setScaleX(1.0);
                card.setScaleY(1.0);
            });

            // Click to view details
            card.setOnMouseClicked(e -> {
                showProductDetail(item);
            });

            // Image Placeholder
            Label lblImg = new Label("Image Not Available");
            lblImg.setStyle("-fx-background-color: #eaeaea; -fx-text-fill: #999999; -fx-alignment: center; -fx-font-size: 14; -fx-background-radius: 8 8 0 0;");
            lblImg.setPrefSize(260, 200);

            VBox infoBox = new VBox(5);
            infoBox.setStyle("-fx-padding: 10 15;");

            // Tên sản phẩm
            Label lblName = new Label(item.getName());
            lblName.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
            lblName.setTextFill(javafx.scene.paint.Color.web("#0654ba"));
            lblName.setWrapText(true);
            lblName.setMaxHeight(40);
            
            // Giá
            Label lblPrice = new Label(item.getPriceStr());
            lblPrice.setTextFill(javafx.scene.paint.Color.web("#191919"));
            lblPrice.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 20));

            infoBox.getChildren().addAll(lblName, lblPrice);
            card.getChildren().addAll(lblImg, infoBox);
            
            gridItems.getChildren().add(card);
        }
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

            JsonObject bidRequest = new JsonObject();
            bidRequest.addProperty("command", "BID");

            JsonObject data = new JsonObject();
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
        
        if (priceSeries != null) {
            priceSeries.getData().clear();
            String currentTime = java.time.LocalTime.now().format(timeFormatter);
            priceSeries.getData().add(new XYChart.Data<>(currentTime, item.getRawPrice()));
        }
        showPage(pageProductDetail);
    }

    @FXML
    public void handlePlaceBid(ActionEvent event) {
        if (selectedProductForDetail != null) {
            doPlaceBid(selectedProductForDetail, txtBidAmount.getText().trim());
            txtBidAmount.clear();
        }
    }

    @FXML
    public void handleSetAutoBid(ActionEvent event) {
        if (selectedProductForDetail != null) {
            String amountStr = txtAutoBidMax.getText().trim();
            if (amountStr.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập mức giá tối đa!");
                return;
            }
            try {
                double maxBid = Double.parseDouble(amountStr);
                if (maxBid <= selectedProductForDetail.getRawPrice()) {
                    showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Max Bid phải cao hơn giá hiện tại!");
                    return;
                }
                
                JsonObject bidRequest = new JsonObject();
                bidRequest.addProperty("command", "AUTO_BID");

                JsonObject data = new JsonObject();
                JsonObject bidder = new JsonObject();
                bidder.addProperty("username", currentUsername);
                data.add("bidder", bidder);
                data.addProperty("maxBid", maxBid);
                data.addProperty("increment", 50000); // Mặc định 50k

                bidRequest.add("data", data);
                if (out != null) {
                    out.println(new Gson().toJson(bidRequest));
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thiết lập Auto Bid với mức tối đa: " + String.format("%,.0f", maxBid) + " ₫");
                    txtAutoBidMax.clear();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến máy chủ.");
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Vui lòng nhập số hợp lệ!");
            }
        }
    }

    // ==========================================
    // UTILS
    // ==========================================

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

        public ItemUI(String name, String description, double rawPrice, String status, double startingPrice, String endTime, String type, String sellerName) {
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

        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getPriceStr() { return priceStr; }
        public String getStatus() { return status; }
        public double getRawPrice() { return rawPrice; }
        public double getStartingPrice() { return startingPrice; }
        public String getEndTime() { return endTime; }
        public String getType() { return type; }
        public String getSellerName() { return sellerName; }
    }


}