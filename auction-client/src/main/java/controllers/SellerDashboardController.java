package controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.PrintWriter;
import java.net.Socket;

public class SellerDashboardController {
    @FXML private TextField txtName;
    @FXML private TextField txtDesc;
    @FXML private TextField txtPrice;
    @FXML private ComboBox<String> cbType;
    @FXML private TextField txtExtra;
    @FXML private Label lblMessage;

    private String currentUsername;
    private PrintWriter out;

    public void setUserInfo(String username) {
        this.currentUsername = username;
    }

    @FXML
    public void initialize() {
        new Thread(() -> {
            try {
                Socket socket = new Socket("127.0.0.1", 9999);
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (Exception e) {
                Platform.runLater(() -> lblMessage.setText("Lỗi kết nối Server!"));
            }
        }).start();
    }

    @FXML
    public void handleAddItem(ActionEvent event) {
        String name = txtName.getText().trim();
        String desc = txtDesc.getText().trim();
        String price = txtPrice.getText().trim();
        String type = cbType.getValue();
        String extra = txtExtra.getText().trim();

        if (name.isEmpty() || price.isEmpty() || type == null) {
            lblMessage.setTextFill(javafx.scene.paint.Color.RED);
            lblMessage.setText("Vui lòng nhập đủ thông tin bắt buộc!");
            return;
        }

        try {
            JsonObject req = new JsonObject();
            req.addProperty("command", "ADD_ITEM");

            JsonObject data = new JsonObject();
            data.addProperty("name", name);
            data.addProperty("desc", desc);
            data.addProperty("price", Double.parseDouble(price));
            data.addProperty("type", type);
            data.addProperty("extra", extra);
            data.addProperty("seller", currentUsername);

            req.add("data", data);
            out.println(new Gson().toJson(req));

            lblMessage.setTextFill(javafx.scene.paint.Color.web("#34c759"));
            lblMessage.setText("Đã gửi sản phẩm lên hệ thống!");

            // Xóa form chuẩn bị nhập tiếp
            txtName.clear(); txtDesc.clear(); txtPrice.clear(); txtExtra.clear();

        } catch (Exception e) {
            lblMessage.setTextFill(javafx.scene.paint.Color.RED);
            lblMessage.setText("Giá tiền phải là số!");
        }
    }
}