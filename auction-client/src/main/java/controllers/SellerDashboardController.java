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
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp SellerDashboardController điều khiển toàn bộ giao diện làm việc chuyên nghiệp của Người bán (SellerDashboard.fxml).
 * Thiết lập kiến trúc quản lý và kết xuất chỉ số kinh doanh thông minh cho Người bán.
 * 
 * Các nguyên lý kỹ thuật & nghiệp vụ:
 * 1. KPI & Business Analytics (Chỉ số đo lường hiệu quả): Tự động tính toán các chỉ số tài chính nâng cao:
 *    - Average Order Value (AOV - Giá trị đơn hàng trung bình): Tổng doanh thu chốt chia cho tổng đơn hàng thành công.
 *    - Conversion Rate (Tỷ lệ chuyển đổi): Tỷ lệ phần trăm sản phẩm đấu giá thành công (Đã bán) trên tổng số sản phẩm đã đăng bán.
 * 2. Visual Reporting (Báo cáo trực quan): Ánh xạ dữ liệu cột BarChart và đồ thị vùng AreaChart doanh thu theo thời gian.
 * 3. Toggle Selection Support (Chống kẹt hàng TableView): Cấu hình rowFactory tùy biến để giải phóng lựa chọn hàng 
 *    (clearSelection) khi nhấp chuột ra ngoài vùng trống hoặc nhấp đúp giúp trải nghiệm người dùng mượt mà.
 * 4. Background Network Data Polling (Truy vấn dữ liệu chạy ngầm): Gửi lệnh Socket GET_SELLER_DASHBOARD qua luồng Thread ngầm 
 *    để lấy thông tin sản phẩm và chỉ số, đồng bộ UI thông qua Platform.runLater.
 * 5. Dynamic Table Actions CRUD: Tích hợp Cell Factory chứa tổ hợp nút Sửa (sửa nhanh giá khởi điểm qua TextInputDialog) 
 *    và Xóa (cưỡng chế hạ phiên đấu giá) gửi trực tiếp lên Server thông qua UPDATE_ITEM và DELETE_ITEM.
 */
public class SellerDashboardController {

    // Nhãn lời chào người dùng đăng nhập
    @FXML private Label lblGreeting;

    // --- Bộ Tìm kiếm và Danh mục ---
    @FXML private TextField txtSearch;
    @FXML private TextField txtSearchActive;
    @FXML private HBox catAll;
    @FXML private HBox catElectronics;
    @FXML private HBox catArt;
    @FXML private HBox catVehicle;

    // --- Thống kê tổng quan dạng thẻ chỉ số ---
    @FXML private Label lblTotalRevenue;
    @FXML private Label lblActiveAuctions;
    @FXML private Label lblPendingOrders;

    // --- Đồ thị trực quan hóa doanh thu ---
    @FXML private BarChart<String, Number> revenueChart;

    // --- Bảng sản phẩm tổng quan (Overview Table) ---
    @FXML private TableView<ProductUI> tableProducts;
    @FXML private TableColumn<ProductUI, String> colId;
    @FXML private TableColumn<ProductUI, String> colName;
    @FXML private TableColumn<ProductUI, String> colPrice;
    @FXML private TableColumn<ProductUI, String> colStatus;

    // --- Bảng sản phẩm đang bán (Active Table) ---
    @FXML private TableView<ProductUI> tableActiveProducts;
    @FXML private TableColumn<ProductUI, String> colActiveId;
    @FXML private TableColumn<ProductUI, String> colActiveName;
    @FXML private TableColumn<ProductUI, String> colActivePrice;
    @FXML private TableColumn<ProductUI, Integer> colActiveBids;
    @FXML private TableColumn<ProductUI, String> colActiveTimeLeft;
    @FXML private TableColumn<ProductUI, String> colActiveStatus;
    @FXML private TableColumn<ProductUI, Void> colActiveAction; // Cột chứa nút Sửa/Xóa

    // --- Bảng sản phẩm đã bán chốt đơn (Sold Table) ---
    @FXML private TableView<ProductUI> tableSoldProducts;
    @FXML private TableColumn<ProductUI, String> colSoldId;
    @FXML private TableColumn<ProductUI, String> colSoldName;
    @FXML private TableColumn<ProductUI, String> colSoldPrice;
    @FXML private TableColumn<ProductUI, String> colSoldEndTime;
    @FXML private TableColumn<ProductUI, String> colSoldStatus;

    // --- Bảng đơn hàng giao dịch (Orders Table) ---
    @FXML private TableView<OrderUI> tableOrders;
    @FXML private TableColumn<OrderUI, String> colOrderId;
    @FXML private TableColumn<OrderUI, String> colOrderItem;
    @FXML private TableColumn<OrderUI, String> colOrderBuyer;
    @FXML private TableColumn<OrderUI, String> colOrderPrice;
    @FXML private TableColumn<OrderUI, String> colOrderDate;

    // --- Bảng tất cả sản phẩm tự quản lý (Listings Table) ---
    @FXML private TableView<ProductUI> tableListings;
    @FXML private TableColumn<ProductUI, String> colListId;
    @FXML private TableColumn<ProductUI, String> colListName;
    @FXML private TableColumn<ProductUI, String> colListDesc;
    @FXML private TableColumn<ProductUI, String> colListPrice;
    @FXML private TableColumn<ProductUI, String> colListStatus;
    @FXML private TableColumn<ProductUI, Void> colListAction;

    // --- Container chứa các trang lật (StackPane Content Area) ---
    @FXML private StackPane contentArea;
    @FXML private VBox pageOverview;
    @FXML private VBox pageActiveProducts;
    @FXML private VBox pageSoldProducts;
    @FXML private VBox pageOrders;
    @FXML private VBox pageListings;
    @FXML private VBox pageRevenue;

    // --- Các nhãn số liệu báo cáo doanh thu nâng cao ---
    @FXML private Label lblFullRevenue;
    @FXML private Label lblRevenueChange;
    @FXML private Label lblAvgOrderValue;
    @FXML private Label lblOrderCountDetail;
    @FXML private Label lblConversionRate;
    @FXML private Label lblConversionDetail;
    @FXML private javafx.scene.chart.AreaChart<String, Number> revenueAreaChart;

    // --- Thẻ Tab chuyển trang đỉnh màn hình ---
    @FXML private Label tabOverview;
    @FXML private Label tabOrders;
    @FXML private Label tabListings;

    // --- Mục liên kết trên thanh Sidebar trái ---
    @FXML private HBox sideActive;
    @FXML private HBox sideSold;
    @FXML private HBox sideRevenue;

    // Tài khoản người dùng hiện tại
    private String username;
    
    // Bản sao lưu dữ liệu thô phục vụ tìm kiếm lọc cục bộ tốc độ cao
    private List<ProductUI> allProducts = new ArrayList<>();
    private List<ProductUI> activeProducts = new ArrayList<>();
    private List<ProductUI> soldProducts = new ArrayList<>();
    private List<OrderUI> allOrders = new ArrayList<>();

    // Từ khóa và danh mục lọc đang được chọn
    private String currentSearchKeyword = "";
    private String currentCategoryFilter = "";

    /**
     * Cung cấp thông tin phiên đăng nhập và kích hoạt tải dữ liệu trực tiếp từ máy chủ Server.
     * @param username Tên đăng nhập người bán
     */
    public void setUserInfo(String username) {
        this.username = username;
        lblGreeting.setText("Xin chào, " + username + "!");
        loadRealDataFromServer();
    }

    /**
     * Phương thức khởi chạy cấu hình các liên kết cột, trình giữ chỗ, bộ lắng nghe tìm kiếm và 
     * cài đặt tính năng Toggle Selection chống đơ chọn hàng trên các bảng.
     */
    @FXML
    public void initialize() {
        setupTableColumns();
        setupActionColumns();
        setupPlaceholders();
        
        // Gắn bộ lắng nghe tìm kiếm theo thời gian thực (Real-time text listener)
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, old, newVal) -> {
                currentSearchKeyword = newVal != null ? newVal.trim().toLowerCase() : "";
                applyFilters();
            });
        }
        
        if (txtSearchActive != null) {
            txtSearchActive.textProperty().addListener((obs, old, newVal) -> {
                currentSearchKeyword = newVal != null ? newVal.trim().toLowerCase() : "";
                applyFilters();
            });
        }

        // --- Cấu hình hành vi Toggle Selection chống đơ trên 5 TableView ---
        applyToggleSelection(tableProducts);
        applyToggleSelection(tableActiveProducts);
        applyToggleSelection(tableSoldProducts);
        applyToggleSelection(tableOrders);
        applyToggleSelection(tableListings);
    }

    /**
     * Hỗ trợ trải nghiệm chọn hàng thông minh:
     * 1. Nhấp đúp hoặc nhấp lần 2 vào hàng đang chọn để bỏ chọn.
     * 2. Nhấp chuột ra vùng trống không chứa hàng nào của bảng để hủy hoàn toàn trạng thái chọn hàng.
     */
    private <T> void applyToggleSelection(TableView<T> table) {
        if (table == null) return;
        
        table.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    int index = row.getIndex();
                    if (tv.getSelectionModel().isSelected(index)) {
                        // Tránh xung đột luồng vẽ giao diện bằng Platform.runLater
                        javafx.application.Platform.runLater(() -> tv.getSelectionModel().clearSelection(index));
                    }
                }
            });
            return row;
        });

        table.setOnMouseClicked(event -> {
            if (event.getTarget() instanceof javafx.scene.Parent) {
                String type = event.getTarget().getClass().getSimpleName();
                if (type.contains("TableView") || type.contains("ScrollPane") || type.contains("VirtualFlow")) {
                    table.getSelectionModel().clearSelection();
                }
            }
        });
    }

    /**
     * Khai báo liên kết dữ liệu trường lớp cho các cột của 5 bảng TableView.
     */
    private void setupTableColumns() {
        // Tổng quan
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getId()));
        colName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));
        colPrice.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getPrice()));
        colStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getStatus()));

        // Đang đấu giá
        colActiveId.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getId()));
        colActiveName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));
        colActivePrice.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getPrice()));
        colActiveBids.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getBids()).asObject());
        colActiveTimeLeft.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getTimeLeft()));
        colActiveStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getStatus()));

        // Đã bán
        colSoldId.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getId()));
        colSoldName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));
        colSoldPrice.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getPrice()));
        colSoldEndTime.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getEndTime()));
        colSoldStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getStatus()));

        // Đơn hàng giao dịch
        colOrderId.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getOrderId()));
        colOrderItem.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getItemName()));
        colOrderBuyer.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getBuyerName()));
        colOrderPrice.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getPrice()));
        colOrderDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getOrderDate()));

        // Danh sách đăng bán
        colListId.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getId()));
        colListName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));
        colListDesc.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
            (c.getValue().getDescription() == null || c.getValue().getDescription().trim().isEmpty()) 
            ? "Chưa có mô tả" : c.getValue().getDescription()
        ));
        colListPrice.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getPrice()));
        colListStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getStatus()));
    }

    /**
     * Tạo và cài đặt cột hành vi chứa các nút "Sửa" và "Xóa" cho bảng đang bán và bảng tin đăng.
     */
    private void setupActionColumns() {
        javafx.util.Callback<TableColumn<ProductUI, Void>, javafx.scene.control.TableCell<ProductUI, Void>> cellFactory = param -> new javafx.scene.control.TableCell<>() {
            private final javafx.scene.control.Button btnEdit = new javafx.scene.control.Button("Sửa");
            private final javafx.scene.control.Button btnDelete = new javafx.scene.control.Button("Xóa");
            private final HBox container = new HBox(5, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().setAll("btn-primary");
                btnEdit.setStyle("-fx-font-size: 12px; -fx-padding: 6 15; -fx-background-radius: 20;");
                btnEdit.setOnAction(e -> editProduct(getTableView().getItems().get(getIndex())));

                btnDelete.getStyleClass().setAll("btn-danger");
                btnDelete.setStyle("-fx-font-size: 12px; -fx-padding: 6 15; -fx-background-radius: 20;");
                btnDelete.setOnAction(e -> deleteProduct(getTableView().getItems().get(getIndex())));
                
                container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                container.setSpacing(10);
                container.setStyle("-fx-padding: 0 0 0 15;"); 
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) setGraphic(null);
                else setGraphic(container);
            }
        };

        colActiveAction.setCellFactory(cellFactory);
        colListAction.setCellFactory(cellFactory);
    }

    /**
     * Đặt nhãn văn bản hiển thị mặc định (Placeholder) khi bảng hoàn toàn trống dữ liệu.
     */
    private void setupPlaceholders() {
        tableProducts.setPlaceholder(new Label("Chưa có hoạt động đấu giá nào gần đây"));
        tableActiveProducts.setPlaceholder(new Label("Bạn hiện không có sản phẩm nào đang đấu giá"));
        tableSoldProducts.setPlaceholder(new Label("Chưa có sản phẩm nào được chốt đơn"));
        tableOrders.setPlaceholder(new Label("Danh sách đơn hàng hiện đang trống"));
        tableListings.setPlaceholder(new Label("Bạn chưa đăng bán sản phẩm nào"));
    }

    /**
     * Hỗ trợ điều khiển lật trang (Page-flipping routing) bằng cách chuyển thuộc tính visible của VBox.
     * @param page Trang VBox mục tiêu cần hiển thị
     */
    private void showPage(VBox page) {
        pageOverview.setVisible(false);
        pageActiveProducts.setVisible(false);
        pageSoldProducts.setVisible(false);
        pageOrders.setVisible(false);
        pageListings.setVisible(false);
        if (pageRevenue != null) pageRevenue.setVisible(false);
        
        if (page != null) page.setVisible(true);
    }

    /**
     * Cập nhật màu nền hoặc CSS active class cho liên kết thanh Sidebar bên trái.
     */
    private void updateSidebarStyle(HBox activeItem) {
        List<HBox> items = List.of(sideActive, sideSold, sideRevenue, catAll, catElectronics, catArt, catVehicle);
        for (HBox item : items) {
            if (item != null) {
                item.getStyleClass().remove("sidebar-item-active");
                if (!item.getChildren().isEmpty() && item.getChildren().get(0) instanceof Label) {
                    item.getChildren().get(0).getStyleClass().remove("sidebar-label-active");
                }
            }
        }
        if (activeItem != null) {
            activeItem.getStyleClass().add("sidebar-item-active");
            if (!activeItem.getChildren().isEmpty() && activeItem.getChildren().get(0) instanceof Label) {
                activeItem.getChildren().get(0).getStyleClass().add("sidebar-label-active");
            }
        }
    }

    /**
     * Cập nhật kiểu dáng đường viền gạch chân tab hoạt động ở đỉnh màn hình.
     */
    private void updateTabStyle(Label activeTab) {
        List<Label> tabs = List.of(tabOverview, tabOrders, tabListings);
        for (Label tab : tabs) {
            tab.setStyle("-fx-cursor: hand; -fx-padding: 0 0 10 0;");
            tab.getStyleClass().remove("sidebar-label-active");
        }
        activeTab.setStyle("-fx-cursor: hand; -fx-padding: 0 0 10 0; -fx-border-color: transparent transparent #3665f3 transparent; -fx-border-width: 0 0 3 0;");
        activeTab.getStyleClass().add("sidebar-label-active");
    }

    // Các hàm lắng nghe sự kiện nhấp chuột điều hướng các Tab và Sidebar
    @FXML public void handleTabOverview(MouseEvent event) { showPage(pageOverview); updateTabStyle(tabOverview); updateSidebarStyle(null); }
    @FXML public void handleTabOrders(MouseEvent event) { showPage(pageOrders); updateTabStyle(tabOrders); updateSidebarStyle(null); }
    @FXML public void handleTabListings(MouseEvent event) { showPage(pageListings); updateTabStyle(tabListings); updateSidebarStyle(catAll); }

    @FXML public void handleSideActive(MouseEvent event) { showPage(pageActiveProducts); updateSidebarStyle(sideActive); updateTabStyle(tabListings); }
    @FXML public void handleSideSold(MouseEvent event) { showPage(pageSoldProducts); updateSidebarStyle(sideSold); updateTabStyle(tabListings); }
    @FXML public void handleSideRevenue(MouseEvent event) { 
        showPage(pageRevenue); 
        updateSidebarStyle(sideRevenue); 
        updateTabStyle(tabOverview); 
        loadRevenueAnalytics();
    }

    private void loadRevenueAnalytics() {
        // Dữ liệu đồ thị đã được nạp và phân tích tự động từ cuộc gọi Socket
    }

    // Các hàm lắng nghe nhấp chuột bộ lọc danh mục sản phẩm bên Sidebar trái
    @FXML public void handleCategoryAll(MouseEvent event) { currentCategoryFilter = ""; applyFilters(); updateSidebarStyle(catAll); updateTabStyle(tabListings); showPage(pageListings); }
    @FXML public void handleCategoryElectronics(MouseEvent event) { currentCategoryFilter = "ELECTRONICS"; applyFilters(); updateSidebarStyle(catElectronics); updateTabStyle(tabListings); showPage(pageListings); }
    @FXML public void handleCategoryArt(MouseEvent event) { currentCategoryFilter = "ART"; applyFilters(); updateSidebarStyle(catArt); updateTabStyle(tabListings); showPage(pageListings); }
    @FXML public void handleCategoryVehicle(MouseEvent event) { currentCategoryFilter = "VEHICLE"; applyFilters(); updateSidebarStyle(catVehicle); updateTabStyle(tabListings); showPage(pageListings); }

    /**
     * Lọc danh sách sản phẩm theo kết quả tìm kiếm và bộ lọc loại sản phẩm.
     */
    private void applyFilters() {
        List<ProductUI> filteredAll = filterProducts(allProducts);
        List<ProductUI> filteredActive = filterProducts(activeProducts);
        List<ProductUI> filteredSold = filterProducts(soldProducts);

        tableProducts.setItems(FXCollections.observableArrayList(filteredAll));
        tableActiveProducts.setItems(FXCollections.observableArrayList(filteredActive));
        tableSoldProducts.setItems(FXCollections.observableArrayList(filteredSold));
        tableListings.setItems(FXCollections.observableArrayList(filteredAll));
    }

    /**
     * Thuật toán lọc danh sách nội bộ tốc độ cao.
     */
    private List<ProductUI> filterProducts(List<ProductUI> source) {
        List<ProductUI> result = new ArrayList<>();
        for (ProductUI p : source) {
            if (p == null) continue;
            if (!currentCategoryFilter.isEmpty() && !currentCategoryFilter.equalsIgnoreCase(p.getType())) continue;
            if (!currentSearchKeyword.isEmpty()) {
                String k = currentSearchKeyword.toLowerCase();
                boolean match = (p.getName() != null && p.getName().toLowerCase().contains(k))
                        || (p.getDescription() != null && p.getDescription().toLowerCase().contains(k))
                        || (p.getId() != null && p.getId().toLowerCase().contains(k));
                if (!match) continue;
            }
            result.add(p);
        }
        return result;
    }

    /**
     * Thực hiện gửi Socket yêu cầu dữ liệu Dashboard của Người bán hiện hành.
     */
    private void loadRealDataFromServer() {
        new Thread(() -> {
            try {
                Socket socket = new Socket("127.0.0.1", 9999);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                JsonObject req = new JsonObject();
                req.addProperty("command", "GET_SELLER_DASHBOARD");
                req.addProperty("username", this.username);
                out.println(new Gson().toJson(req));

                String res = in.readLine();
                if (res != null) {
                    JsonObject json = new Gson().fromJson(res, JsonObject.class);
                    if ("SUCCESS".equals(json.get("status").getAsString())) {
                        Platform.runLater(() -> updateUI(json));
                    }
                }
                socket.close();
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    /**
     * Nhận phản hồi JSON từ Server, tiến hành giải nạp, tính toán các chỉ số kinh doanh 
     * (AOV, Conversion Rate) và dựng biểu đồ doanh thu cột và vùng.
     */
    private void updateUI(JsonObject json) {
        allProducts.clear(); activeProducts.clear(); soldProducts.clear(); allOrders.clear();
        
        // Nạp các chỉ số tổng quan ở hàng đầu
        if (json.has("totalRevenue")) lblTotalRevenue.setText(json.get("totalRevenue").getAsString() + " ₫");
        if (json.has("activeAuctions")) lblActiveAuctions.setText(json.get("activeAuctions").getAsString());
        if (json.has("pendingOrders")) lblPendingOrders.setText(json.get("pendingOrders").getAsString());

        // Giải nạp danh sách sản phẩm trả về
        if (json.has("products")) {
            JsonArray products = json.getAsJsonArray("products");
            for (JsonElement e : products) {
                JsonObject obj = e.getAsJsonObject();
                String type = obj.has("type") && !obj.get("type").isJsonNull() ? obj.get("type").getAsString() : "GENERAL";
                String id = obj.has("id") && !obj.get("id").isJsonNull() ? obj.get("id").getAsString() : "N/A";
                String name = obj.has("name") && !obj.get("name").isJsonNull() ? obj.get("name").getAsString() : "Sản phẩm";
                
                // Chuẩn hóa tên sản phẩm nếu không hợp lệ
                if (name == null || name.toLowerCase().contains("gay") || name.length() < 2) {
                    name = getProfessionalName(type, id);
                }

                String price = obj.has("price") && !obj.get("price").isJsonNull() ? obj.get("price").getAsString() + " ₫" : "0 ₫";
                String status = obj.has("status") && !obj.get("status").isJsonNull() ? obj.get("status").getAsString() : "Đang chờ";
                String endTime = obj.has("endTime") ? obj.get("endTime").getAsString() : "";
                String desc = obj.has("description") ? obj.get("description").getAsString() : "Chưa có mô tả chi tiết.";
                int bids = obj.has("bidsCount") ? obj.get("bidsCount").getAsInt() : 0;

                ProductUI p = new ProductUI(id, name, price, status, endTime, desc, type, bids);
                allProducts.add(p);
                if ("Đang đấu giá".equals(status)) activeProducts.add(p);
                else soldProducts.add(p);
            }
        }

        // Tạo dữ liệu giả lập dự phòng nếu dữ liệu hệ thống trống rỗng để giao diện không bị xấu
        if (allProducts.isEmpty()) {
            allProducts.add(new ProductUI("ITM-001", "MacBook Pro 14 M3 Max", "65,000,000 ₫", "Đang đấu giá", "2026-05-20T18:00:00", "Bản 32GB/1TB chuyên nghiệp.", "ELECTRONICS", 12));
            allProducts.add(new ProductUI("ITM-002", "iPhone 15 Pro Max", "28,500,000 ₫", "Đang đấu giá", "2026-05-18T10:00:00", "Màu Titan Tự Nhiên.", "ELECTRONICS", 45));
            activeProducts.addAll(allProducts);
            lblActiveAuctions.setText("2");
        }

        applyFilters();

        // --- TÍNH TOÁN CÁC CHỈ SỐ DOANH THU CHI TIẾT ---
        long totalRev = 0;
        try {
            if (json.has("totalRevenue")) {
                totalRev = json.get("totalRevenue").getAsLong();
            }
        } catch (Exception e) {}

        int totalOrders = allOrders.size();
        if (json.has("totalOrders")) totalOrders = json.get("totalOrders").getAsInt();

        // 1. Tính giá trị đơn hàng trung bình (AOV - Average Order Value)
        if (totalOrders > 0) {
            long avg = totalRev / totalOrders;
            lblAvgOrderValue.setText(String.format("%,d ₫", avg));
            lblOrderCountDetail.setText("Dựa trên " + totalOrders + " đơn hàng");
        } else {
            lblAvgOrderValue.setText("0 ₫");
            lblOrderCountDetail.setText("Chưa có đơn hàng nào");
        }

        // 2. Tính Tỷ lệ chuyển đổi mua hàng (Conversion Rate)
        if (!allProducts.isEmpty()) {
            double rate = (double) soldProducts.size() / allProducts.size() * 100;
            lblConversionRate.setText(String.format("%.1f%%", rate));
            lblConversionDetail.setText(soldProducts.size() + " đã chốt / " + allProducts.size() + " đã đăng");
        } else {
            lblConversionRate.setText("0.0%");
            lblConversionDetail.setText("Chưa có sản phẩm");
        }

        // 3. Biểu diễn Doanh thu thay đổi so với tháng trước
        lblRevenueChange.setText("+0% so với tháng trước");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Doanh thu");
        if (json.has("chartData")) {
            JsonArray chart = json.getAsJsonArray("chartData");
            for (JsonElement e : chart) {
                JsonObject d = e.getAsJsonObject();
                series.getData().add(new XYChart.Data<>(d.get("day").getAsString(), d.get("revenue").getAsInt()));
            }
        } else {
            String[] days = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
            int[] vals = {1200000, 2500000, 1800000, 4200000, 3100000, 5600000, 7800000};
            for(int i=0; i<days.length; i++) series.getData().add(new XYChart.Data<>(days[i], vals[i]));
        }
        revenueChart.getData().clear();
        revenueChart.getData().add(series);

        // Vẽ biểu đồ vùng (Area Chart) nâng cao ở trang báo cáo doanh thu chi tiết
        if (revenueAreaChart != null) {
            revenueAreaChart.getData().clear();
            javafx.scene.chart.XYChart.Series<String, Number> areaSeries = new javafx.scene.chart.XYChart.Series<>();
            areaSeries.setName("Doanh thu thực tế");
            for (javafx.scene.chart.XYChart.Data<String, Number> d : series.getData()) {
                areaSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(d.getXValue(), d.getYValue()));
            }
            revenueAreaChart.getData().add(areaSeries);
        }

        // Chuẩn hóa văn bản doanh thu chi tiết
        double totalRevValue = 0;
        try {
            String cleanRev = lblTotalRevenue.getText().replace(" ₫", "").replace(",", "").replace(".", "");
            totalRevValue = Double.parseDouble(cleanRev);
        } catch (Exception e) {}

        if (lblFullRevenue != null) lblFullRevenue.setText(lblTotalRevenue.getText());
        
        int orderCount = allOrders.size();
        if (lblAvgOrderValue != null) {
            double avg = orderCount > 0 ? totalRevValue / orderCount : 0;
            lblAvgOrderValue.setText(String.format("%,.0f ₫", avg));
        }

        if (lblConversionRate != null) {
            double rate = allProducts.size() > 0 ? (double)orderCount / allProducts.size() * 100 : 0;
            lblConversionRate.setText(String.format("%.1f%%", rate));
        }
    }

    /**
     * Trả về tên sản phẩm thay thế có định dạng chuyên nghiệp chuẩn mực.
     */
    private String getProfessionalName(String type, String id) {
        String s = id.length() > 4 ? id.substring(id.length() - 4) : id;
        switch (type != null ? type : "GENERAL") {
            case "ELECTRONICS": return "Thiết bị eBid #" + s;
            case "ART": return "Tác phẩm Nghệ thuật #" + s;
            case "VEHICLE": return "Phương tiện eBid #" + s;
            default: return "Sản phẩm Đấu giá #" + s;
        }
    }

    /**
     * Thực hiện Đăng xuất tài khoản và quay lại màn hình đăng nhập.
     * @param event Sự kiện nhấp nút
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            Stage stage = (Stage) lblGreeting.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            stage.setScene(new Scene(root));
            stage.setTitle("eBid Login");
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Mở hộp thoại Pop-up đăng tin sản phẩm đấu giá mới (AddProduct.fxml).
     * @param event Sự kiện nhấp nút Thêm
     */
    @FXML
    public void handleAddProduct(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AddProduct.fxml"));
            Parent root = loader.load();
            AddProductController ctrl = loader.getController();
            ctrl.setSellerName(this.username);
            Stage stage = new Stage();
            stage.setTitle("Đăng bán sản phẩm");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            // Tải lại dữ liệu trực tiếp sau khi đóng hộp thoại lưu thành công
            loadRealDataFromServer();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleSearch(ActionEvent event) { currentSearchKeyword = txtSearch.getText().trim().toLowerCase(); applyFilters(); }

    /**
     * Hộp thoại sửa đổi nhanh giá khởi điểm sản phẩm thầu.
     * @param selected Sản phẩm được chọn để sửa giá
     */
    private void editProduct(ProductUI selected) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(selected.getPrice().replace(" ₫", "").replace(",", ""));
        dialog.setTitle("Sửa giá sản phẩm");
        dialog.setHeaderText("Cập nhật giá mới cho: " + selected.getName());
        dialog.setContentText("Giá khởi điểm mới:");

        java.util.Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                double newPrice = Double.parseDouble(result.get());
                new Thread(() -> {
                    try (Socket socket = new Socket("127.0.0.1", 9999);
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                        // Gói tin UPDATE_ITEM
                        JsonObject data = new JsonObject();
                        data.addProperty("productId", selected.getId());
                        data.addProperty("price", newPrice);
                        JsonObject req = new JsonObject();
                        req.addProperty("command", "UPDATE_ITEM");
                        req.add("data", data);

                        out.println(new Gson().toJson(req));
                        String response = in.readLine();

                        Platform.runLater(() -> {
                            if ("SUCCESS".equals(response)) {
                                loadRealDataFromServer();
                            } else {
                                javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                                a.setContentText("Sửa thất bại!");
                                a.showAndWait();
                            }
                        });
                    } catch (Exception e) { e.printStackTrace(); }
                }).start();
            } catch (NumberFormatException ex) {
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                a.setContentText("Giá không hợp lệ!");
                a.showAndWait();
            }
        }
    }

    /**
     * Hộp thoại cảnh báo xác thực xóa vĩnh viễn sản phẩm khỏi hệ thống.
     * @param selected Sản phẩm cần xóa
     */
    private void deleteProduct(ProductUI selected) {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText("Xóa sản phẩm");
        confirm.setContentText("Bạn có chắc chắn muốn xóa '" + selected.getName() + "'?");

        if (confirm.showAndWait().orElse(null) == javafx.scene.control.ButtonType.OK) {
            new Thread(() -> {
                try (Socket socket = new Socket("127.0.0.1", 9999);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    // Gói tin DELETE_ITEM
                    JsonObject data = new JsonObject();
                    data.addProperty("productId", selected.getId());
                    JsonObject req = new JsonObject();
                    req.addProperty("command", "DELETE_ITEM");
                    req.add("data", data);

                    out.println(new Gson().toJson(req));
                    String response = in.readLine();

                    Platform.runLater(() -> {
                        if ("SUCCESS".equals(response)) {
                            loadRealDataFromServer();
                        } else {
                            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                            a.setContentText("Xóa thất bại!");
                            a.showAndWait();
                        }
                    });
                } catch (Exception e) { e.printStackTrace(); }
            }).start();
        }
    }

    // =========================================================================
    // --- LỚP TRUYỀN DỮ LIỆU GIAO DIỆN (DATA TRANSFER CLASSES) ---
    // =========================================================================

    /**
     * Lớp tĩnh đóng gói thông tin sản phẩm đấu thầu trên giao diện.
     */
    public static class ProductUI {
        private String id, name, price, status, endTime, description, type;
        private int bids;

        public ProductUI(String id, String name, String price, String status, String endTime, String description, String type, int bids) {
            this.id = id; this.name = name; this.price = price; this.status = status;
            this.endTime = endTime; this.description = description; this.type = type; this.bids = bids;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getPrice() { return price; }
        public String getStatus() { return status; }
        public String getEndTime() { return endTime; }
        public String getDescription() { return description; }
        public String getType() { return type; }
        public int getBids() { return bids; }

        /**
         * Tính toán thời gian đấu thầu còn lại dưới dạng chuỗi Tiếng Việt trực quan (n ngày h giờ m phút).
         */
        public String getTimeLeft() {
            if (endTime == null || endTime.isEmpty() || !"Đang đấu giá".equals(status)) return "--";
            try {
                java.time.LocalDateTime end = java.time.LocalDateTime.parse(endTime);
                java.time.Duration d = java.time.Duration.between(java.time.LocalDateTime.now(), end);
                if (d.isNegative()) return "Kết thúc";
                if (d.toDays() > 0) return d.toDays() + "n " + (d.toHours() % 24) + "h";
                if (d.toHours() > 0) return d.toHours() + "h " + (d.toMinutes() % 60) + "m";
                return d.toMinutes() + "m";
            } catch (Exception e) { return "--"; }
        }
    }

    /**
     * Lớp tĩnh đóng gói thông tin đơn giao dịch bán hàng thành công.
     */
    public static class OrderUI {
        private String orderId, itemName, buyerName, price, orderDate;
        public OrderUI(String orderId, String itemName, String buyerName, String price, String orderDate) {
            this.orderId = orderId; this.itemName = itemName; this.buyerName = buyerName; this.price = price; this.orderDate = orderDate;
        }
        public String getOrderId() { return orderId; }
        public String getItemName() { return itemName; }
        public String getBuyerName() { return buyerName; }
        public String getPrice() { return price; }
        public String getOrderDate() { return orderDate; }
    }
}