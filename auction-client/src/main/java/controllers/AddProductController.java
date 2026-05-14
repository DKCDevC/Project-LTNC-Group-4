package controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class AddProductController {

    @FXML private TextField txtProductName;
    @FXML private TextField txtStartPrice;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cboCategory;
    @FXML private TextField txtDurationValue;
    @FXML private ComboBox<String> cboDurationUnit;
    @FXML private Label lblStatus;
    @FXML private FlowPane photoContainer;

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
        // Khởi tạo danh mục
        cboCategory.setItems(FXCollections.observableArrayList(
                "Khác (Chung)",
                "Đồ điện tử",
                "Nghệ thuật",
                "Xe cộ"
        ));
        cboCategory.setValue("Khác (Chung)");

        // Khởi tạo thời gian
        cboDurationUnit.setItems(FXCollections.observableArrayList(
                "Phút",
                "Giờ",
                "Ngày"
        ));
        cboDurationUnit.setValue("Ngày");
        cboDurationUnit.setValue("Ngày");
        txtDurationValue.setText("7");

        // --- Auto-format Price with commas ---
        txtStartPrice.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;
            
            // Remove everything except numbers
            String digits = newValue.replaceAll("[^\\d]", "");
            if (digits.isEmpty()) {
                txtStartPrice.setText("");
                return;
            }
            
            try {
                long value = Long.parseLong(digits);
                // Format with commas
                String formatted = String.format("%,d", value).replace(',', ','); // Standard comma separator
                if (!newValue.equals(formatted)) {
                    txtStartPrice.setText(formatted);
                    // Keep cursor at the end
                    Platform.runLater(() -> txtStartPrice.positionCaret(formatted.length()));
                }
            } catch (Exception e) {}
        });
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    @FXML
    public void handleUploadPhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Media Files", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.mp4", "*.mov"),
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp"),
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.mov")
        );

        Stage stage = (Stage) txtProductName.getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);

        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                if (imagePaths.size() >= 5) {
                    lblStatus.setText("Tối đa 5 ảnh!");
                    break;
                }
                String path = file.toURI().toString();
                imagePaths.add(path);

                ImageView imageView = new ImageView(new Image(path));
                imageView.setFitWidth(100);
                imageView.setFitHeight(100);
                imageView.setPreserveRatio(true);
                
                photoContainer.getChildren().add(imageView);
            }
        }
    }

    @FXML
    public void handleSaveProduct(ActionEvent event) {
        String name = txtProductName.getText().trim();
        String priceStr = txtStartPrice.getText().trim();
        String desc = txtDescription.getText().trim();

        if (name.isEmpty() || priceStr.isEmpty()) {
            lblStatus.setText("Vui lòng nhập các trường bắt buộc (*)");
            return;
        }

        try {
            // Strip commas before parsing
            String cleanPrice = priceStr.replaceAll("[^\\d]", "");
            double price = Double.parseDouble(cleanPrice);
            lblStatus.setStyle("-fx-text-fill: gray;");
            lblStatus.setText("Đang đẩy lên Server...");

            String type = getCategoryType();

            // Convert danh sách ảnh thành chuỗi cách nhau bởi dấu phẩy
            String extraImages = String.join(",", imagePaths);

            // Lấy thời gian
            int durationValue = 7;
            try {
                durationValue = Integer.parseInt(txtDurationValue.getText().trim());
            } catch (Exception ex) {}
            
            String durationUnit = cboDurationUnit.getValue();
            
            final int finalDurationValue = durationValue;
            final String finalDurationUnit = durationUnit;

            new Thread(() -> {
                try {
                    Socket socket = new Socket("127.0.0.1", 9999);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                    JsonObject data = new JsonObject();
                    data.addProperty("type", type);
                    data.addProperty("name", name);
                    data.addProperty("desc", desc);
                    data.addProperty("price", price);
                    data.addProperty("extra", extraImages);
                    data.addProperty("seller", this.sellerName);
                    data.addProperty("durationValue", finalDurationValue);
                    data.addProperty("durationUnit", finalDurationUnit);

                    JsonObject request = new JsonObject();
                    request.addProperty("command", "ADD_ITEM");
                    request.add("data", data);

                    out.println(new Gson().toJson(request));
                    socket.close();

                    Platform.runLater(() -> {
                        lblStatus.setStyle("-fx-text-fill: green;");
                        lblStatus.setText("Đăng bán thành công!");
                        Stage stage = (Stage) txtProductName.getScene().getWindow();
                        stage.close();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        lblStatus.setStyle("-fx-text-fill: red;");
                        lblStatus.setText("Lỗi kết nối Server!");
                    });
                }
            }).start();

        } catch (NumberFormatException e) {
            lblStatus.setText("Giá khởi điểm phải là số!");
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        Stage stage = (Stage) txtProductName.getScene().getWindow();
        stage.close();
    }
}