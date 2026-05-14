package controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import models.User;
import network.AuctionClient;
import javafx.event.ActionEvent;

public class AdminDashboardController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, String> statusCol;
    @FXML private TableColumn<User, Void> actionCol;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Pagination pagination;
    
    // Auctions
    @FXML private TableView<models.Auction> auctionTable;
    @FXML private TableColumn<models.Auction, String> colAuctionId;
    @FXML private TableColumn<models.Auction, String> colItemName;
    @FXML private TableColumn<models.Auction, String> colAuctionSeller;
    @FXML private TableColumn<models.Auction, String> colAuctionPrice;
    @FXML private TableColumn<models.Auction, String> colAuctionEndTime;
    @FXML private TableColumn<models.Auction, Void> colAuctionAction;
    @FXML private TextField auctionSearchField;
    
    // Sidebar buttons
    @FXML private Button btnManageUsers;
    @FXML private Button btnAuctions;
    @FXML private Button btnTransactions;
    @FXML private Button btnReports;
    
    // Views
    @FXML private VBox viewManageUsers;
    @FXML private VBox viewAuctions;
    @FXML private VBox viewTransactions;
    @FXML private VBox viewReports;

    private final AuctionClient client = AuctionClient.getInstance();
    private final Gson gson = GsonConfig.createGson();
    private ObservableList<User> masterData = FXCollections.observableArrayList();
    private FilteredList<User> filteredData;
    private ObservableList<models.Auction> auctionMasterData = FXCollections.observableArrayList();
    private static final int ROWS_PER_PAGE = 10;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFilters();
        loadUserData();
        
        // Bind managed property to visible property for clean switching
        viewManageUsers.managedProperty().bind(viewManageUsers.visibleProperty());
        viewAuctions.managedProperty().bind(viewAuctions.visibleProperty());
        viewTransactions.managedProperty().bind(viewTransactions.visibleProperty());
        viewReports.managedProperty().bind(viewReports.visibleProperty());

        // Force sidebar buttons to full width
        btnManageUsers.setMaxWidth(Double.MAX_VALUE);
        btnAuctions.setMaxWidth(Double.MAX_VALUE);
        btnTransactions.setMaxWidth(Double.MAX_VALUE);
        btnReports.setMaxWidth(Double.MAX_VALUE);

        // Set initial active state
        btnManageUsers.getStyleClass().add("sidebar-button-active");

        setupAuctionTable();
    }

    private void setupAuctionTable() {
        colAuctionId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAuctionId()));
        colItemName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getName()));
        colAuctionSeller.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getSeller() != null ? cellData.getValue().getSeller().getUsername() : "Unknown"));
        colAuctionPrice.setCellValueFactory(cellData -> new SimpleStringProperty(
            String.format("%,.0f đ", cellData.getValue().getItem().getStartingPrice())));
        colAuctionEndTime.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getItem().getEndTime().toString().replace("T", " ")));

        setupAuctionActionColumn();
    }

    private void setupAuctionActionColumn() {
        colAuctionAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnCancel = new Button("Hủy");
            {
                btnCancel.getStyleClass().add("action-button-lock");
                btnCancel.setStyle("-fx-text-fill: #dc3545; -fx-border-color: #dc3545; -fx-background-color: transparent; -fx-padding: 5 15;");
                btnCancel.setOnAction(event -> {
                    models.Auction auction = getTableView().getItems().get(getIndex());
                    handleCancelAuction(auction);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnCancel);
            }
        });
    }

    private void handleCancelAuction(models.Auction auction) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận hủy");
        alert.setHeaderText("Hủy phiên đấu giá: " + auction.getItem().getName());
        alert.setContentText("Hành động này không thể hoàn tác. Bạn có chắc chắn?");
        
        if (alert.showAndWait().get() == ButtonType.OK) {
            String request = "{\"command\":\"ADMIN\", \"subCommand\":\"CANCEL_AUCTION\", \"targetId\":\"" + auction.getAuctionId() + "\"}";
            client.sendRequest(request, response -> {
                if ("SUCCESS".equals(response)) {
                    Platform.runLater(() -> {
                        auctionMasterData.remove(auction);
                        showAlert("Thành công", "Đã hủy phiên đấu giá.");
                    });
                } else {
                    Platform.runLater(() -> showAlert("Lỗi", "Không thể hủy phiên đấu giá."));
                }
            });
        }
    }

    private void loadAuctionData() {
        String request = "{\"command\":\"ADMIN\", \"subCommand\":\"GET_AUCTIONS\"}";
        client.sendRequest(request, response -> {
            try {
                List<models.Auction> auctions = gson.fromJson(response, new TypeToken<List<models.Auction>>(){}.getType());
                Platform.runLater(() -> {
                    auctionMasterData.clear();
                    auctionMasterData.addAll(auctions);
                    auctionTable.setItems(auctionMasterData);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void setupFilters() {
        roleFilter.setItems(FXCollections.observableArrayList("Tất cả", "ADMIN", "BIDDER", "SELLER"));
        roleFilter.setValue("Tất cả");
        statusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Hoạt động", "Đã khóa"));
        statusFilter.setValue("Tất cả");

        // Real-time filtering
        roleFilter.setOnAction(e -> applyFilters());
        statusFilter.setOnAction(e -> applyFilters());
    }

    private void setupTableColumns() {
        usernameCol.setCellValueFactory(data -> data.getValue() != null ? new SimpleStringProperty(data.getValue().getUsername()) : null);
        emailCol.setCellValueFactory(data -> data.getValue() != null ? new SimpleStringProperty(data.getValue().getEmail()) : null);
        roleCol.setCellValueFactory(data -> data.getValue() != null ? new SimpleStringProperty(data.getValue().getRole()) : null);
        
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
                    setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
                    if (isLocked) {
                        setTextFill(javafx.scene.paint.Color.web("#e53238")); // Red
                    } else {
                        setTextFill(javafx.scene.paint.Color.web("#28a745")); // Green
                    }
                }
            }
        });

        // Center align columns
        roleCol.setStyle("-fx-alignment: CENTER;");
        usernameCol.setStyle("-fx-alignment: CENTER-LEFT;");
        emailCol.setStyle("-fx-alignment: CENTER-LEFT;");
        
        // Setup Action Buttons (Lock/Unlock/Verify)
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button lockBtn = new Button();
            {
                lockBtn.setCursor(Cursor.HAND);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size() || getTableView().getItems().get(getIndex()) == null) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    if (user.getUsername() == null || user.getUsername().isEmpty()) {
                        setGraphic(null);
                        return;
                    }
                    boolean isLocked = user.isLocked();
                    lockBtn.setText(isLocked ? "Mở khóa" : "Khóa");
                    
                    if (isLocked) {
                        lockBtn.setStyle("-fx-background-color: #e8f0fe; -fx-text-fill: #3665f3; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6; -fx-border-color: #3665f3; -fx-border-radius: 6; -fx-border-width: 1;");
                    } else {
                        lockBtn.setStyle("-fx-background-color: #fce8e8; -fx-text-fill: #e53238; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6; -fx-border-color: #e53238; -fx-border-radius: 6; -fx-border-width: 1;");
                    }

                    lockBtn.setOnAction(event -> handleLockToggle(user));
                    setGraphic(lockBtn);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });
    }

    private void applyFilters() {
        if (filteredData == null) return;

        String searchText = searchField.getText().toLowerCase();
        String role = roleFilter.getValue();
        String status = statusFilter.getValue();

        filteredData.setPredicate(user -> {
            // Search filter
            boolean matchesSearch = searchText.isEmpty() || 
                                    user.getUsername().toLowerCase().contains(searchText) ||
                                    user.getEmail().toLowerCase().contains(searchText);
            
            // Role filter
            boolean matchesRole = role.equals("Tất cả") || user.getRole().equals(role);
            
            // Status filter
            boolean matchesStatus = status.equals("Tất cả") || 
                                    (status.equals("Đã khóa") && user.isLocked()) ||
                                    (status.equals("Hoạt động") && !user.isLocked());

            return matchesSearch && matchesRole && matchesStatus;
        });

        updatePagination();
    }

    private void updatePagination() {
        int totalItems = filteredData.size();
        int pageCount = (totalItems + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
        pagination.setPageCount(Math.max(1, pageCount));
        pagination.setPageFactory(this::createPage);
    }

    private Node createPage(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filteredData.size());
        userTable.setItems(FXCollections.observableArrayList(filteredData.subList(fromIndex, toIndex)));
        return new Group(); // Dummy node as we update table directly
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    @FXML
    private void handleAddUser() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Thêm người dùng mới");
        dialog.setHeaderText("Nhập thông tin người dùng mới");

        ButtonType registerButtonType = new ButtonType("Đăng ký", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Tên đăng nhập");
        PasswordField password = new PasswordField();
        password.setPromptText("Mật khẩu");
        TextField email = new TextField();
        email.setPromptText("Email");
        ComboBox<String> role = new ComboBox<>(FXCollections.observableArrayList("BIDDER", "SELLER", "ADMIN"));
        role.setValue("BIDDER");

        grid.add(new Label("Tên đăng nhập:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Mật khẩu:"), 0, 1);
        grid.add(password, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(email, 1, 2);
        grid.add(new Label("Vai trò:"), 0, 3);
        grid.add(role, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registerButtonType) {
                return new User(username.getText(), password.getText(), email.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newUser -> {
            newUser.setRole(role.getValue());
            String request = String.format("{\"command\":\"REGISTER\", \"username\":\"%s\", \"password\":\"%s\", \"email\":\"%s\", \"role\":\"%s\"}",
                    newUser.getUsername(), newUser.getPassword(), newUser.getEmail(), newUser.getRole());
            
            client.sendRequest(request, response -> {
                if (response.contains("SUCCESS")) {
                    Platform.runLater(() -> {
                        loadUserData();
                        showAlert("Thành công", "Đã thêm người dùng mới!");
                    });
                } else {
                    Platform.runLater(() -> showAlert("Lỗi", "Không thể thêm người dùng."));
                }
            });
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void loadUserData() {
        String request = "{\"command\":\"ADMIN\", \"subCommand\":\"GET_USERS\"}";
        client.sendRequest(request, response -> {
            List<User> users = gson.fromJson(response, new TypeToken<List<User>>(){}.getType());
            Platform.runLater(() -> {
                masterData = FXCollections.observableArrayList(users);
                filteredData = new FilteredList<>(masterData, p -> true);
                applyFilters();
            });
        });
    }

    private void handleLockToggle(User user) {
        String action = user.isLocked() ? "mở khóa" : "khóa";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận thao tác");
        confirm.setHeaderText("Bạn có chắc chắn muốn " + action + " người dùng này?");
        confirm.setContentText("Tên đăng nhập: " + user.getUsername());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String subCmd = user.isLocked() ? "UNLOCK_USER" : "LOCK_USER";
                String request = "{\"command\":\"ADMIN\", \"subCommand\":\"" + subCmd + "\", \"targetUser\":\"" + user.getUsername() + "\"}";
                client.sendRequest(request, res -> {
                    if ("SUCCESS".equals(res)) {
                        loadUserData();
                    }
                });
            }
        });
    }

    @FXML
    private void handleSidebarNavigation(ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        
        // Reset all buttons to default style
        btnManageUsers.getStyleClass().removeAll("sidebar-button-active");
        btnAuctions.getStyleClass().removeAll("sidebar-button-active");
        btnTransactions.getStyleClass().removeAll("sidebar-button-active");
        btnReports.getStyleClass().removeAll("sidebar-button-active");

        // Hide all views
        viewManageUsers.setVisible(false);
        viewAuctions.setVisible(false);
        viewTransactions.setVisible(false);
        viewReports.setVisible(false);

        // Show selected view and activate button
        clickedBtn.getStyleClass().add("sidebar-button-active");
        
        if (clickedBtn == btnManageUsers) {
            viewManageUsers.setVisible(true);
            loadUserData();
        } else if (clickedBtn == btnAuctions) {
            viewAuctions.setVisible(true);
            loadAuctionData();
        } else if (clickedBtn == btnTransactions) {
            viewTransactions.setVisible(true);
        } else if (clickedBtn == btnReports) {
            viewReports.setVisible(true);
        }
    }

    @FXML
    private void handleLogout() {
        try {
            Stage stage = (Stage) userTable.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
