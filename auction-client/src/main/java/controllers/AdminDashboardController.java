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
    
    @FXML private TableView<models.Auction> auctionTable;
    @FXML private TableColumn<models.Auction, String> colAuctionId;
    @FXML private TableColumn<models.Auction, String> colItemName;
    @FXML private TableColumn<models.Auction, String> colAuctionSeller;
    @FXML private TableColumn<models.Auction, String> colAuctionPrice;
    @FXML private TableColumn<models.Auction, String> colAuctionEndTime;
    @FXML private TableColumn<models.Auction, Void> colAuctionAction;
    
    @FXML private Button btnManageUsers;
    @FXML private Button btnAuctions;
    @FXML private Button btnTransactions;
    @FXML private Button btnReports;
    
    @FXML private VBox viewManageUsers;
    @FXML private VBox viewAuctions;
    @FXML private VBox viewTransactions;
    @FXML private VBox viewReports;

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
        setupAuctionTable();
        
        viewManageUsers.managedProperty().bind(viewManageUsers.visibleProperty());
        viewAuctions.managedProperty().bind(viewAuctions.visibleProperty());
        viewTransactions.managedProperty().bind(viewTransactions.visibleProperty());
        viewReports.managedProperty().bind(viewReports.visibleProperty());

        btnManageUsers.setMaxWidth(Double.MAX_VALUE);
        btnAuctions.setMaxWidth(Double.MAX_VALUE);
        btnTransactions.setMaxWidth(Double.MAX_VALUE);
        btnReports.setMaxWidth(Double.MAX_VALUE);
    }

    private void setupTableColumns() {
        usernameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        emailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        roleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        
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

    private void setupAuctionTable() {
        colAuctionId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAuctionId()));
        colItemName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getName()));
        colAuctionSeller.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getSeller() != null ? cellData.getValue().getSeller().getUsername() : "Unknown"));
        colAuctionPrice.setCellValueFactory(cellData -> new SimpleStringProperty(
            String.format("%,.0f đ", cellData.getValue().getItem().getStartingPrice())));
        colAuctionEndTime.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getItem().getEndTime().toString().replace("T", " ")));

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

    private void loadUserData() {
        new Thread(() -> {
            try (Socket socket = new Socket("127.0.0.1", 9999);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                JsonObject request = new JsonObject();
                request.addProperty("command", "ADMIN");
                request.addProperty("subCommand", "GET_USERS");
                out.println(gson.toJson(request));
                String response = in.readLine();
                if (response != null) {
                    List<User> users = gson.fromJson(response, new TypeToken<List<User>>(){}.getType());
                    Platform.runLater(() -> {
                        masterData = FXCollections.observableArrayList(users);
                        filteredData = new FilteredList<>(masterData, p -> true);
                        applyFilters();
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void loadAuctionData() {
        new Thread(() -> {
            try (Socket socket = new Socket("127.0.0.1", 9999);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
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
                if ("SUCCESS".equals(in.readLine())) Platform.runLater(this::loadUserData);
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

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

    private void setupFilters() {
        roleFilter.setItems(FXCollections.observableArrayList("Tất cả", "ADMIN", "BIDDER", "SELLER"));
        roleFilter.setValue("Tất cả");
        statusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Hoạt động", "Đã khóa"));
        statusFilter.setValue("Tất cả");
        roleFilter.setOnAction(e -> applyFilters());
        statusFilter.setOnAction(e -> applyFilters());
    }

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

    private void updatePagination() {
        int pageCount = (filteredData.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
        pagination.setPageCount(Math.max(1, pageCount));
        pagination.setPageFactory(this::createPage);
    }

    private Node createPage(int pageIndex) {
        int from = pageIndex * ROWS_PER_PAGE;
        int to = Math.min(from + ROWS_PER_PAGE, filteredData.size());
        userTable.setItems(FXCollections.observableArrayList(filteredData.subList(from, to)));
        return new Group();
    }

    @FXML private void handleSearch() { applyFilters(); }
    @FXML private void handleAddUser() { showAlert("Thông báo", "Tính năng này đang bảo trì."); }

    @FXML
    private void handleSidebarNavigation(ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        viewManageUsers.setVisible(clickedBtn == btnManageUsers);
        viewAuctions.setVisible(clickedBtn == btnAuctions);
        viewTransactions.setVisible(clickedBtn == btnTransactions);
        viewReports.setVisible(clickedBtn == btnReports);
        if (clickedBtn == btnManageUsers) loadUserData();
        else if (clickedBtn == btnAuctions) loadAuctionData();
    }

    @FXML
    private void handleLogout() {
        try {
            Stage stage = (Stage) userTable.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (Exception e) { e.printStackTrace(); }
    }

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
