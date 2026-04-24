package controllers;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import network.SocketManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AddProductController {

    @FXML private TextField txtProductName, txtStartPrice, txtReservePrice, txtMinIncrement;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cboCategory, cboDuration;
    @FXML private VBox boxDuration, boxReservePrice, boxMinIncrement;
    @FXML private Label lblStatus;
    @FXML private FlowPane photoContainer;
    @FXML private RadioButton radAuction, radBuyNow;
    @FXML private ToggleGroup saleTypeGroup;

    private String sellerName;
    private List<String> imagePaths = new ArrayList<>();

    private String getCategoryType() {
        if (cboCategory.getValue() == null) return "GENERAL";
        switch (cboCategory.getValue()) {
            case "Đồ điện tử": return "ELECTRONICS";
            case "Nghệ thuật": return "ART";
            case "Xe cộ": return "VEHICLE";
            default: return "GENERAL";
        }
    }

    @FXML
    public void initialize() {
        cboCategory.setItems(FXCollections.observableArrayList("Khác (Chung)", "Đồ điện tử", "Nghệ thuật", "Xe cộ"));
        cboCategory.setValue("Khác (Chung)");
        cboDuration.setItems(FXCollections.observableArrayList("1 Phút", "5 Phút", "30 Phút", "1 Giờ", "1 Ngày", "3 Ngày", "7 Ngày"));
        cboDuration.setValue("1 Giờ");
        txtMinIncrement.setText("1000");
        txtReservePrice.setText("0");

        saleTypeGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            boolean isAuction = (newT == radAuction);
            boxDuration.setVisible(isAuction); boxDuration.setManaged(isAuction);
            boxReservePrice.setVisible(isAuction); boxReservePrice.setManaged(isAuction);
            boxMinIncrement.setVisible(isAuction); boxMinIncrement.setManaged(isAuction);
        });

        SocketManager.getInstance().addListener("ADD_ITEM_RESPONSE", resp -> {
            Platform.runLater(() -> {
                if ("SUCCESS".equals(resp.get("status").getAsString())) {
                    lblStatus.setStyle("-fx-text-fill: green;");
                    lblStatus.setText("Đăng bán thành công!");
                    // Give a small delay before closing
                    new Thread(() -> {
                        try { Thread.sleep(500); } catch (Exception ignored) {}
                        Platform.runLater(() -> ((Stage) txtProductName.getScene().getWindow()).close());
                    }).start();
                } else {
                    lblStatus.setStyle("-fx-text-fill: red;");
                    lblStatus.setText("Lỗi từ server!");
                }
            });
        });
    }

    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    @FXML
    public void handleUploadPhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog((Stage) txtProductName.getScene().getWindow());
        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                if (imagePaths.size() >= 5) break;
                String path = file.toURI().toString();
                imagePaths.add(path);
                ImageView iv = new ImageView(new Image(path));
                iv.setFitWidth(100); iv.setFitHeight(100); iv.setPreserveRatio(true);
                photoContainer.getChildren().add(iv);
            }
        }
    }

    @FXML
    public void handleSaveProduct(ActionEvent event) {
        String name = txtProductName.getText().trim();
        String priceStr = txtStartPrice.getText().trim();
        if (name.isEmpty() || priceStr.isEmpty()) { lblStatus.setText("Nhập tên và giá!"); return; }

        try {
            double price = Double.parseDouble(priceStr);
            lblStatus.setText("Đang đăng bán...");
            
            int durationMinutes = 60;
            if (radAuction.isSelected()) {
                String val = cboDuration.getValue();
                if (val.contains("Phút")) durationMinutes = Integer.parseInt(val.split(" ")[0]);
                else if (val.contains("Giờ")) durationMinutes = Integer.parseInt(val.split(" ")[0]) * 60;
                else if (val.contains("Ngày")) durationMinutes = Integer.parseInt(val.split(" ")[0]) * 1440;
            } else durationMinutes = 43200;

            JsonObject data = new JsonObject();
            data.addProperty("type", getCategoryType());
            data.addProperty("name", name);
            data.addProperty("desc", txtDescription.getText().trim());
            data.addProperty("price", price);
            data.addProperty("reservePrice", txtReservePrice.getText().isEmpty() ? 0 : Double.parseDouble(txtReservePrice.getText()));
            data.addProperty("minIncrement", txtMinIncrement.getText().isEmpty() ? 1000 : Double.parseDouble(txtMinIncrement.getText()));
            data.addProperty("extra", String.join(",", imagePaths));
            data.addProperty("seller", this.sellerName);
            data.addProperty("duration", durationMinutes);
            data.addProperty("saleType", radBuyNow.isSelected() ? "BUY_NOW" : "AUCTION");
            data.addProperty("imageUrls", String.join(",", imagePaths));

            JsonObject request = new JsonObject();
            request.addProperty("command", "ADD_ITEM");
            request.add("data", data);

            SocketManager.getInstance().send(request);
        } catch (Exception e) { lblStatus.setText("Số không hợp lệ!"); }
    }

    @FXML public void handleCancel(ActionEvent event) {
        ((Stage) txtProductName.getScene().getWindow()).close();
    }
}
