package controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import models.Item;
import network.AuctionChartGUI;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class DashboardController {

    @FXML private TableView<Item> tableItems;
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, String> colDesc;
    @FXML private TableColumn<Item, Double> colPrice;
    @FXML private TableColumn<Item, String> colStatus;

    @FXML private TextField txtBidAmount;
    @FXML private Button btnBid;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Đã bỏ hardcode, các biến này sẽ nhận giá trị từ LoginController
    private String currentUsername;
    private String userRole;

    private ObservableList<Item> masterData = FXCollections.observableArrayList();
    private Gson gson = new Gson();

    // HÀM ĐỂ LOGINCONTROLLER TRUYỀN DỮ LIỆU SANG
    public void setUserInfo(String username, String role) {
        this.currentUsername = username;
        this.userRole = role;
    }

    @FXML
    public void initialize() {
        // 1. Cấu hình các cột trong bảng
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentHighestPrice"));
        colStatus.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty("ĐANG CHẠY"));

        // 2. Bắt sự kiện Click đúp vào 1 dòng để xem chi tiết sản phẩm
        tableItems.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Item rowData = row.getItem();
                    showDetailPopup(rowData);
                }
            });
            return row;
        });

        // 3. Kết nối Server và lắng nghe dữ liệu ngầm
        startBackgroundListener();
    }

    private void startBackgroundListener() {
        new Thread(() -> {
            try {
                socket = new Socket("127.0.0.1", 9999);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Ngay khi kết nối, gửi lệnh xin danh sách sản phẩm từ SQLite
                out.println("{\"command\":\"GET_ITEMS\"}");

                String response;
                while ((response = in.readLine()) != null) {
                    try {
                        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                        String cmd = json.get("command").getAsString();

                        if ("SET_ITEMS".equals(cmd)) {
                            // Đọc list sản phẩm Server trả về và đưa lên bảng
                            List<Item> items = gson.fromJson(json.get("data"), new TypeToken<List<Item>>(){}.getType());
                            Platform.runLater(() -> {
                                masterData.setAll(items);
                                tableItems.setItems(masterData);
                            });
                        }
                        else if ("UPDATE_PRICE".equals(cmd)) {
                            // Có người vừa đặt giá mới -> Tự động xin lại list mới để cập nhật bảng
                            out.println("{\"command\":\"GET_ITEMS\"}");
                        }
                    } catch (Exception ex) {
                        System.out.println("Lỗi xử lý JSON ngầm tại Client: " + ex.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println("Lỗi kết nối Socket tại Dashboard.");
            }
        }).start();
    }

    // Nút Đặt Giá
    @FXML
    public void handlePlaceBid(ActionEvent event) {
        String amountStr = txtBidAmount.getText().trim();

        if (amountStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập mức giá!");
            return;
        }

        try {
            double bidAmount = Double.parseDouble(amountStr);

            JsonObject bidRequest = new JsonObject();
            bidRequest.addProperty("command", "BID");

            JsonObject data = new JsonObject();
            JsonObject bidder = new JsonObject();

            // Lấy đúng Username đang đăng nhập
            bidder.addProperty("username", currentUsername);

            data.add("bidder", bidder);
            data.addProperty("amount", bidAmount);

            bidRequest.add("data", data);

            out.println(gson.toJson(bidRequest));
            txtBidAmount.clear();

            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã gửi yêu cầu đặt giá: " + bidAmount);

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Vui lòng nhập số hợp lệ!");
        }
    }

    // Nút Xem Biểu Đồ
    @FXML
    public void handleShowChart(ActionEvent event) {
        try {
            Stage chartStage = new Stage();
            AuctionChartGUI chartGUI = new AuctionChartGUI();
            chartGUI.start(chartStage);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở biểu đồ.");
        }
    }

    // Hàm hiện chi tiết khi click đúp
    private void showDetailPopup(Item item) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Chi tiết sản phẩm");
            alert.setHeaderText(item.getName());
            String info = "Mô tả: " + item.getDescription() + "\n" +
                    "Giá khởi điểm: " + item.getStartingPrice() + "\n" +
                    "Giá hiện tại: " + item.getCurrentHighestPrice() + "\n" +
                    "Kết thúc lúc: " + item.getEndTime();
            alert.setContentText(info);
            alert.showAndWait();
        });
    }

    // Hàm tiện ích hiện thông báo chung
    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}