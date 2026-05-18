package controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import utils.GsonConfig;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import models.User;
import javafx.event.ActionEvent;

/**
 * Lớp AdminDashboardController điều khiển màn hình tổng quan quản trị của Quản trị viên (admin_dashboard.fxml).
 * Cung cấp hệ thống tính năng hành chính cao cấp:
 * - Quản lý Danh sách Người dùng: Xem thông tin, tìm kiếm, lọc theo Vai trò/Trạng thái và Khóa/Mở khóa tài khoản.
 * - Quản lý Phiên Đấu Giá: Theo dõi toàn bộ phiên đấu giá thời gian thực và cưỡng chế hủy thầu vi phạm quy chế.
 * - Phân trang dữ liệu người dùng (Pagination) thông minh tối ưu hiệu năng.
 * - Điều khiển Sidebar chuyển đổi màn hình động và xử lý Đăng xuất an toàn.
 */
public class AdminDashboardController {

    // Thành phần điều khiển bảng Người dùng (User Table)
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, String> statusCol;
    @FXML private TableColumn<User, Void> actionCol;
    
    // Thành phần tìm kiếm và bộ lọc
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Pagination pagination; // Đối tượng phân trang JavaFX
    
    // Thành phần điều khiển bảng Phiên đấu giá (Auction Table)
    @FXML private TableView<models.Auction> auctionTable;
    @FXML private TableColumn<models.Auction, String> colAuctionId;
    @FXML private TableColumn<models.Auction, String> colItemName;
    @FXML private TableColumn<models.Auction, String> colAuctionSeller;
    @FXML private TableColumn<models.Auction, String> colAuctionPrice;
    @FXML private TableColumn<models.Auction, String> colAuctionEndTime;
    @FXML private TableColumn<models.Auction, Void> colAuctionAction;
    
    // Nút bấm Sidebar chuyển trang
    @FXML private Button btnManageUsers;
    @FXML private Button btnAuctions;
    @FXML private Button btnTransactions;
    @FXML private Button btnReports;
    
    // Khung màn hình chuyển đổi tương ứng
    @FXML private VBox viewManageUsers;
    @FXML private VBox viewAuctions;
    @FXML private VBox viewTransactions;
    @FXML private VBox viewReports;

    // Bộ phân tích JSON GSON cấu hình LocalDateTime tương thích
    private final Gson gson = GsonConfig.createGson();
    
    // Danh sách gốc người dùng
    private ObservableList<User> masterData = FXCollections.observableArrayList();
    
    // Danh sách lọc trung gian phục vụ tìm kiếm động
    private FilteredList<User> filteredData;
    
    // Danh sách gốc các phiên đấu giá
    private ObservableList<models.Auction> auctionMasterData = FXCollections.observableArrayList();
    
    // Số hàng hiển thị tối đa trên một trang
    private static final int ROWS_PER_PAGE = 10;

    /**
     * Phương thức khởi tạo cấu hình các cột, sự kiện bộ lọc, tải dữ liệu người dùng ban đầu 
     * và liên kết thuộc tính layout động.
     */
    @FXML
    public void initialize() {
        setupTableColumns();
        setupFilters();
        loadUserData();
        setupAuctionTable();
        
        // Kỹ thuật JavaFX Layout Bindings: Liên kết thuộc tính managed với visible. 
        // Khi ẩn một VBox (visible = false), nó sẽ tự giải phóng diện tích (managed = false) 
        // giúp giao diện co giãn linh hoạt mà không bị khoảng trắng thừa.
        viewManageUsers.managedProperty().bind(viewManageUsers.visibleProperty());
        viewAuctions.managedProperty().bind(viewAuctions.visibleProperty());
        viewTransactions.managedProperty().bind(viewTransactions.visibleProperty());
        viewReports.managedProperty().bind(viewReports.visibleProperty());

        // Đặt nút bấm Sidebar co giãn tối đa theo bề rộng
        btnManageUsers.setMaxWidth(Double.MAX_VALUE);
        btnAuctions.setMaxWidth(Double.MAX_VALUE);
        btnTransactions.setMaxWidth(Double.MAX_VALUE);
        btnReports.setMaxWidth(Double.MAX_VALUE);
    }

    /**
     * Cấu hình nạp giá trị và hiển thị màu sắc tùy chỉnh cho bảng Người dùng.
     */
    private void setupTableColumns() {
        usernameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        emailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        roleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        
        // Tùy biến hiển thị trạng thái tài khoản màu đỏ/xanh trực quan (Đã khóa / Hoạt động)
        statusCol.setCellFactory(param -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView().getItems().get(getIndex()) == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    boolean isLocked = user.isLocked();
                    setText(isLocked ? "Đã khóa" : "Hoạt động");
                    setTextFill(isLocked ? javafx.scene.paint.Color.RED : javafx.scene.paint.Color.GREEN);
                    setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
                }
            }
        });

        // Tạo nút Khóa/Mở khóa động trực tiếp trên từng hàng của bảng dữ liệu
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button lockBtn = new Button();
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    User user = getTableView().getItems().get(getIndex());
                    boolean isLocked = user.isLocked();
                    lockBtn.setText(isLocked ? "Mở khóa" : "Khóa");
                    lockBtn.setStyle(isLocked ? "-fx-text-fill: blue;" : "-fx-text-fill: red;");
                    lockBtn.setOnAction(e -> handleLockToggle(user));
                    setGraphic(lockBtn);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });
    }

    /**
     * Cấu hình nạp giá trị hiển thị cột và hành vi nút Hủy cho bảng đấu giá.
     */
    private void setupAuctionTable() {
        colAuctionId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAuctionId()));
        colItemName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getName()));
        colAuctionSeller.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getSeller() != null ? cellData.getValue().getSeller().getUsername() : "Unknown"));
        colAuctionPrice.setCellValueFactory(cellData -> new SimpleStringProperty(
            String.format("%,.0f đ", cellData.getValue().getItem().getStartingPrice())));
        colAuctionEndTime.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getItem().getEndTime().toString().replace("T", " ")));

        // Tích hợp nút Cưỡng chế Hủy thầu cho Admin
        colAuctionAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnCancel = new Button("Hủy");
            {
                btnCancel.setStyle("-fx-text-fill: #dc3545; -fx-border-color: #dc3545;");
                btnCancel.setOnAction(event -> handleCancelAuction(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnCancel);
            }
        });
    }

    /**
     * Tải danh sách toàn bộ người dùng từ Server qua kết nối Socket TCP bất đồng bộ.
     */
    private void loadUserData() {
        new Thread(() -> {
            try (Socket socket = new Socket("127.0.0.1", 9999);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                
                // Đóng gói gói tin ADMIN - GET_USERS
                JsonObject request = new JsonObject();
                request.addProperty("command", "ADMIN");
                request.addProperty("subCommand", "GET_USERS");
                out.println(gson.toJson(request));
                
                String response = in.readLine();
                if (response != null) {
                    List<User> users = gson.fromJson(response, new TypeToken<List<User>>(){}.getType());
                    // Cập nhật lại UI an toàn thông qua Platform.runLater
                    Platform.runLater(() -> {
                        masterData = FXCollections.observableArrayList(users);
                        filteredData = new FilteredList<>(masterData, p -> true);
                        applyFilters();
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    /**
     * Tải toàn bộ danh sách phiên đấu giá hiện hành từ Server qua Socket.
     */
    private void loadAuctionData() {
        new Thread(() -> {
            try (Socket socket = new Socket("127.0.0.1", 9999);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                
                // Đóng gói gói tin ADMIN - GET_AUCTIONS
                JsonObject request = new JsonObject();
                request.addProperty("command", "ADMIN");
                request.addProperty("subCommand", "GET_AUCTIONS");
                out.println(gson.toJson(request));
                
                String response = in.readLine();
                if (response != null) {
                    List<models.Auction> auctions = gson.fromJson(response, new TypeToken<List<models.Auction>>(){}.getType());
                    Platform.runLater(() -> {
                        auctionMasterData.setAll(auctions);
                        auctionTable.setItems(auctionMasterData);
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    /**
     * Xử lý gửi lệnh Khóa/Mở khóa tài khoản người dùng chỉ định lên Server.
     * @param user Đối tượng người dùng mục tiêu
     */
    private void handleLockToggle(User user) {
        String subCmd = user.isLocked() ? "UNLOCK_USER" : "LOCK_USER";
        new Thread(() -> {
            try (Socket socket = new Socket("127.0.0.1", 9999);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                
                JsonObject req = new JsonObject();
                req.addProperty("command", "ADMIN");
                req.addProperty("subCommand", subCmd);
                req.addProperty("targetUser", user.getUsername());
                
                out.println(gson.toJson(req));
                if ("SUCCESS".equals(in.readLine())) {
                    // Sau khi khóa/mở khóa thành công, tải lại dữ liệu mới nhất
                    Platform.runLater(this::loadUserData);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    /**
     * Xử lý gửi lệnh cưỡng chế hủy bỏ một phiên đấu giá vi phạm quy định.
     * @param auction Đối tượng phiên đấu giá cần hủy
     */
    private void handleCancelAuction(models.Auction auction) {
        if (auction == null) return;
        new Thread(() -> {
            try (Socket socket = new Socket("127.0.0.1", 9999);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                
                JsonObject request = new JsonObject();
                request.addProperty("command", "ADMIN");
                request.addProperty("subCommand", "CANCEL_AUCTION");
                request.addProperty("targetId", auction.getAuctionId());
                
                out.println(gson.toJson(request));
                if ("SUCCESS".equals(in.readLine())) {
                    Platform.runLater(() -> {
                        auctionMasterData.remove(auction);
                        showAlert("Thành công", "Đã hủy phiên đấu giá.");
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    /**
     * Cài đặt bộ lọc danh mục vai trò và trạng thái khóa tài khoản.
     */
    private void setupFilters() {
        roleFilter.setItems(FXCollections.observableArrayList("Tất cả", "ADMIN", "BIDDER", "SELLER"));
        roleFilter.setValue("Tất cả");
        statusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Hoạt động", "Đã khóa"));
        statusFilter.setValue("Tất cả");
        roleFilter.setOnAction(e -> applyFilters());
        statusFilter.setOnAction(e -> applyFilters());
    }

    /**
     * Áp dụng bộ lọc kết hợp giữa: tìm kiếm từ khóa nhập vào, bộ lọc vai trò ComboBox, 
     * và trạng thái khóa. Thực hiện làm mới phân trang dữ liệu hiển thị.
     */
    private void applyFilters() {
        if (filteredData == null) return;
        String searchText = searchField.getText().toLowerCase();
        String role = roleFilter.getValue();
        String status = statusFilter.getValue();
        
        filteredData.setPredicate(user -> {
            boolean matchesSearch = searchText.isEmpty() || user.getUsername().toLowerCase().contains(searchText);
            boolean matchesRole = role.equals("Tất cả") || user.getRole().equals(role);
            boolean matchesStatus = status.equals("Tất cả") || 
                                     (status.equals("Đã khóa") && user.isLocked()) ||
                                     (status.equals("Hoạt động") && !user.isLocked());
            return matchesSearch && matchesRole && matchesStatus;
        });
        updatePagination();
    }

    /**
     * Cập nhật số lượng trang dựa trên dữ liệu sau khi lọc và ROWS_PER_PAGE.
     */
    private void updatePagination() {
        int pageCount = (filteredData.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
        pagination.setPageCount(Math.max(1, pageCount));
        pagination.setPageFactory(this::createPage);
    }

    /**
     * Hàm Callback phân trang: Cắt lát dữ liệu tương ứng hiển thị lên TableView.
     * @param pageIndex Chỉ mục trang hiện tại
     * @return Node rỗng (vì TableView tự cập nhật dữ liệu)
     */
    private Node createPage(int pageIndex) {
        int from = pageIndex * ROWS_PER_PAGE;
        int to = Math.min(from + ROWS_PER_PAGE, filteredData.size());
        userTable.setItems(FXCollections.observableArrayList(filteredData.subList(from, to)));
        return new Group(); // Trả về container rỗng theo chuẩn JavaFX
    }

    @FXML private void handleSearch() { applyFilters(); }
    @FXML private void handleAddUser() { showAlert("Thông báo", "Tính năng này đang bảo trì."); }

    /**
     * Xử lý điều hướng các mục Sidebar trái trên Menu hành chính.
     * @param event Sự kiện ActionEvent kích hoạt từ nút bấm Sidebar
     */
    @FXML
    private void handleSidebarNavigation(ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        
        // Thay đổi tính ẩn/hiển thị của màn hình tương ứng
        viewManageUsers.setVisible(clickedBtn == btnManageUsers);
        viewAuctions.setVisible(clickedBtn == btnAuctions);
        viewTransactions.setVisible(clickedBtn == btnTransactions);
        viewReports.setVisible(clickedBtn == btnReports);
        
        // Tải lại dữ liệu trực tuyến của trang tương ứng
        if (clickedBtn == btnManageUsers) loadUserData();
        else if (clickedBtn == btnAuctions) loadAuctionData();
    }

    /**
     * Đăng xuất khỏi tài khoản Admin và chuyển hướng quay lại màn hình Login.fxml.
     */
    @FXML
    private void handleLogout() {
        try {
            Stage stage = (Stage) userTable.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Tiện ích hiển thị Dialog thông báo nhanh trên giao diện.
     * @param title Tiêu đề Dialog
     * @param content Nội dung thông điệp
     */
    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
