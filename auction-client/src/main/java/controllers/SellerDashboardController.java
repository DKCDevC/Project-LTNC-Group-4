package controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import network.SocketManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SellerDashboardController {

    @FXML private Label lblGreeting;
    @FXML private TextField txtSearch;
    @FXML private Label catAll, catElectronics, catArt, catVehicle, catGeneral;

    @FXML private Label lblTotalRevenue, lblActiveAuctions, lblPendingOrders;
    @FXML private BarChart<String, Number> revenueChart, revenueChartDetail;

    @FXML private TableView<ProductUI> tableProducts, tableActiveProducts, tableSoldProducts, tableListings;
    @FXML private TableColumn<ProductUI, String> colId, colName, colPrice, colStatus;
    @FXML private TableColumn<ProductUI, String> colActiveId, colActiveName, colActivePrice, colActiveEndTime, colActiveStatus;
    @FXML private TableColumn<ProductUI, String> colSoldId, colSoldName, colSoldPrice, colSoldEndTime, colSoldStatus;
    @FXML private TableColumn<OrderUI, String> colOrderId, colOrderItem, colOrderBuyer, colOrderPrice, colOrderDate;
    @FXML private TableView<OrderUI> tableOrders;
    @FXML private TableColumn<ProductUI, String> colListId, colListName, colListDesc, colListPrice, colListStatus;

    @FXML private Label lblRevenueTotalDetail, lblSoldCount;
    @FXML private StackPane contentArea;
    @FXML private VBox pageOverview, pageActiveProducts, pageSoldProducts, pageRevenue, pageOrders, pageListings;
    @FXML private ScrollPane scrollActive, scrollSold, scrollRevenue, scrollOrders, scrollListings;
    @FXML private Label tabOverview, tabOrders, tabListings;
    @FXML private Label sideActive, sideSold, sideRevenue;

    private String username;
    private List<ProductUI> allProducts = new ArrayList<>();
    private List<ProductUI> activeProducts = new ArrayList<>();
    private List<ProductUI> soldProducts = new ArrayList<>();
    private List<OrderUI> allOrders = new ArrayList<>();

    private String currentSearchKeyword = "";
    private String currentCategoryFilter = "";

    public void setUserInfo(String username) {
        this.username = username;
        lblGreeting.setText("Xin chào, " + username + "!");
        loadRealDataFromServer();
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        txtSearch.textProperty().addListener((obs, old, newVal) -> {
            currentSearchKeyword = newVal != null ? newVal.trim().toLowerCase() : "";
            applyFilters();
        });
        
        SocketManager sm = SocketManager.getInstance();
        sm.addListener("SET_SELLER_DASHBOARD", this::handleDashboardData);
        sm.addListener("DELETE_ITEM_RESPONSE", this::handleDeleteResponse);
        sm.addListener("UPDATE_ITEM_RESPONSE", this::handleUpdateResponse);
        
        // Listen for global updates (e.g. when someone adds or deletes an item)
        sm.addListener("UPDATE_PRICE", (json) -> {
            Platform.runLater(this::loadRealDataFromServer);
        });
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colActiveId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colActiveName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colActivePrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colActiveEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colActiveStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colSoldId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSoldName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSoldPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colSoldEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colSoldStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colOrderItem.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colOrderBuyer.setCellValueFactory(new PropertyValueFactory<>("buyerName"));
        colOrderPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colOrderDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));

        colListId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colListName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colListDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colListPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colListStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadRealDataFromServer() {
        JsonObject request = new JsonObject();
        request.addProperty("command", "GET_SELLER_DASHBOARD");
        request.addProperty("username", this.username);
        SocketManager.getInstance().send(request);
    }

    private void handleDashboardData(JsonObject jsonResponse) {
        Platform.runLater(() -> {
            allProducts.clear(); activeProducts.clear(); soldProducts.clear(); allOrders.clear();
            revenueChart.getData().clear(); revenueChartDetail.getData().clear();

            if (jsonResponse.has("totalRevenue")) {
                String rev = jsonResponse.get("totalRevenue").getAsString() + " ₫";
                lblTotalRevenue.setText(rev);
                lblRevenueTotalDetail.setText(rev);
            }
            lblActiveAuctions.setText(jsonResponse.get("activeAuctions").getAsString() + " Sản phẩm");
            lblPendingOrders.setText(jsonResponse.get("pendingOrders").getAsString() + " đơn");

            int soldCount = 0;
            if (jsonResponse.has("products")) {
                JsonArray products = jsonResponse.getAsJsonArray("products");
                for (JsonElement element : products) {
                    JsonObject obj = element.getAsJsonObject();
                    ProductUI p = parseProduct(obj);
                    allProducts.add(p);
                    if ("Đang bán".equals(p.getStatus())) activeProducts.add(p);
                    else { soldProducts.add(p); soldCount++; }
                }
            }
            lblSoldCount.setText(String.valueOf(soldCount));

            if (jsonResponse.has("orders")) {
                JsonArray orders = jsonResponse.getAsJsonArray("orders");
                for (JsonElement element : orders) {
                    JsonObject obj = element.getAsJsonObject();
                    allOrders.add(new OrderUI(obj.get("orderId").getAsString(), obj.get("itemName").getAsString(),
                        obj.get("buyerName").getAsString(), obj.get("price").getAsString() + " ₫", obj.get("orderDate").getAsString()));
                }
            }

            if (jsonResponse.has("chartData")) {
                XYChart.Series<String, Number> series = new XYChart.Series<>(); series.setName("Doanh thu");
                XYChart.Series<String, Number> series2 = new XYChart.Series<>(); series2.setName("Doanh thu");
                JsonArray chartData = jsonResponse.getAsJsonArray("chartData");
                for (JsonElement element : chartData) {
                    JsonObject dayData = element.getAsJsonObject();
                    series.getData().add(new XYChart.Data<>(dayData.get("day").getAsString(), dayData.get("revenue").getAsInt()));
                    series2.getData().add(new XYChart.Data<>(dayData.get("day").getAsString(), dayData.get("revenue").getAsInt()));
                }
                revenueChart.getData().add(series);
                revenueChartDetail.getData().add(series2);
            }
            applyFilters();
        });
    }

    private ProductUI parseProduct(JsonObject obj) {
        ProductUI p = new ProductUI(obj.get("id").getAsString(), obj.get("name").getAsString(),
            obj.get("price").getAsString() + " ₫", obj.get("status").getAsString(),
            obj.has("endTime") ? obj.get("endTime").getAsString() : "",
            obj.has("description") ? obj.get("description").getAsString() : "",
            obj.has("type") ? obj.get("type").getAsString() : "GENERAL");
        if (obj.has("saleType") && !obj.get("saleType").isJsonNull()) p.setSaleType(obj.get("saleType").getAsString());
        if (obj.has("imageUrls") && !obj.get("imageUrls").isJsonNull()) p.setImageUrls(obj.get("imageUrls").getAsString());
        return p;
    }

    private void handleDeleteResponse(JsonObject resp) {
        Platform.runLater(() -> {
            if ("SUCCESS".equals(resp.get("status").getAsString())) {
                showAlert("Xóa thành công!");
                loadRealDataFromServer();
            } else showAlert("Xóa thất bại!");
        });
    }

    private void handleUpdateResponse(JsonObject resp) {
        Platform.runLater(() -> {
            if ("SUCCESS".equals(resp.get("status").getAsString())) {
                showAlert("Sửa thành công!");
                loadRealDataFromServer();
            } else showAlert("Sửa thất bại!");
        });
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setContentText(msg);
        a.showAndWait();
    }

    @FXML
    public void handleDeleteProduct(ActionEvent event) {
        ProductUI selected = getSelectedProduct();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Xóa sản phẩm '" + selected.getName() + "'?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            JsonObject req = new JsonObject();
            req.addProperty("command", "DELETE_ITEM");
            JsonObject data = new JsonObject();
            data.addProperty("productId", selected.getId());
            data.addProperty("username", this.username);
            req.add("data", data);
            SocketManager.getInstance().send(req);
        }
    }

    @FXML
    public void handleEditProduct(ActionEvent event) {
        ProductUI selected = getSelectedProduct();
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("Sửa tên");
        Optional<String> name = dialog.showAndWait();
        if (name.isPresent()) {
            JsonObject req = new JsonObject();
            req.addProperty("command", "UPDATE_ITEM");
            JsonObject data = new JsonObject();
            data.addProperty("productId", selected.getId());
            data.addProperty("username", this.username);
            data.addProperty("name", name.get().trim());
            data.addProperty("desc", selected.getDescription());
            data.addProperty("price", Double.parseDouble(selected.getPrice().replace(" ₫", "").replace(",", "")));
            req.add("data", data);
            SocketManager.getInstance().send(req);
        }
    }

    private ProductUI getSelectedProduct() {
        ProductUI s = tableActiveProducts.getSelectionModel().getSelectedItem();
        if (s == null) s = tableProducts.getSelectionModel().getSelectedItem();
        if (s == null) s = tableListings.getSelectionModel().getSelectedItem();
        if (s == null) showAlert("Vui lòng chọn sản phẩm!");
        return s;
    }

    @FXML 
    public void handleLogout(ActionEvent event) {
        try {
            SocketManager sm = SocketManager.getInstance();
            sm.clearListeners();
            sm.close();

            Stage stage = (Stage) lblGreeting.getScene().getWindow();
            if (stage != null) {
                Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
                stage.setScene(new Scene(root));
                stage.setMaximized(true);
                stage.setTitle("Hệ thống đấu giá eBid - Đăng nhập");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleAddProduct(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AddProduct.fxml"));
            Parent root = loader.load();
            ((AddProductController)loader.getController()).setSellerName(this.username);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadRealDataFromServer();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- Tab & Sidebar Navigation (simplified) ---
    private void showPage(javafx.scene.Node page) {
        // Hide all potential page containers
        javafx.scene.Node[] containers = {
            pageOverview, scrollActive, scrollSold, scrollRevenue, scrollOrders, scrollListings
        };
        for (javafx.scene.Node n : containers) {
            if (n != null) {
                n.setVisible(false);
                n.setManaged(false);
            }
        }
        page.setVisible(true);
        page.setManaged(true);
    }

    @FXML public void handleTabOverview(MouseEvent e) { showPage(pageOverview); setActiveTab(tabOverview); }
    @FXML public void handleTabOrders(MouseEvent e) { showPage(scrollOrders); setActiveTab(tabOrders); }
    @FXML public void handleTabListings(MouseEvent e) { showPage(scrollListings); setActiveTab(tabListings); }
    @FXML public void handleSideActive(MouseEvent e) { showPage(scrollActive); }
    @FXML public void handleSideSold(MouseEvent e) { showPage(scrollSold); }
    @FXML public void handleSideRevenue(MouseEvent e) { showPage(scrollRevenue); }

    private void setActiveTab(Label tab) {
        Label[] tabs = {tabOverview, tabOrders, tabListings};
        for (Label t : tabs) t.setStyle("-fx-cursor: hand; -fx-padding: 10 0 5 0;");
        tab.setStyle("-fx-border-color: transparent transparent #0654ba transparent; -fx-border-width: 3; -fx-padding: 10 0 5 0;");
    }

    @FXML
    public void handleSearch() {
        applyFilters();
    }

    private void applyFilters() {
        tableProducts.setItems(FXCollections.observableArrayList(filterProducts(allProducts)));
        tableActiveProducts.setItems(FXCollections.observableArrayList(filterProducts(activeProducts)));
        tableSoldProducts.setItems(FXCollections.observableArrayList(filterProducts(soldProducts)));
        tableListings.setItems(FXCollections.observableArrayList(filterProducts(allProducts)));
        tableOrders.setItems(FXCollections.observableArrayList(allOrders));
    }

    private List<ProductUI> filterProducts(List<ProductUI> source) {
        List<ProductUI> r = new ArrayList<>();
        for (ProductUI p : source) {
            if (!currentCategoryFilter.isEmpty() && !currentCategoryFilter.equalsIgnoreCase(p.getType())) continue;
            if (!currentSearchKeyword.isEmpty() && !p.getName().toLowerCase().contains(currentSearchKeyword)) continue;
            r.add(p);
        }
        return r;
    }

    @FXML public void handleCategoryAll(MouseEvent event) { currentCategoryFilter = ""; applyFilters(); }
    @FXML public void handleCategoryElectronics(MouseEvent event) { currentCategoryFilter = "ELECTRONICS"; applyFilters(); }
    @FXML public void handleCategoryArt(MouseEvent event) { currentCategoryFilter = "ART"; applyFilters(); }
    @FXML public void handleCategoryVehicle(MouseEvent event) { currentCategoryFilter = "VEHICLE"; applyFilters(); }
    @FXML public void handleCategoryGeneral(MouseEvent event) { currentCategoryFilter = "GENERAL"; applyFilters(); }

    public static class ProductUI {
        private String id, name, price, status, endTime, description, type, saleType = "AUCTION", imageUrls = "";
        public ProductUI(String id, String name, String price, String status, String endTime, String description, String type) 
        { this.id=id; this.name=name; this.price=price; this.status=status; this.endTime=endTime; this.description=description; this.type=type; }
        public String getId() { return id; } public String getName() { return name; } public String getPrice() { return price; }
        public String getStatus() { return status; } public String getEndTime() { return endTime; } public String getDescription() { return description; }
        public String getType() { return type; } public String getSaleType() { return saleType; } public void setSaleType(String s) { saleType=s; }
        public String getImageUrls() { return imageUrls; } public void setImageUrls(String i) { imageUrls=i; }
    }

    public static class OrderUI {
        private String orderId, itemName, buyerName, price, orderDate;
        public OrderUI(String id, String item, String buyer, String price, String date) 
        { this.orderId=id; this.itemName=item; this.buyerName=buyer; this.price=price; this.orderDate=date; }
        public String getOrderId() { return orderId; } public String getItemName() { return itemName; }
        public String getBuyerName() { return buyerName; } public String getPrice() { return price; } public String getOrderDate() { return orderDate; }
    }
}
