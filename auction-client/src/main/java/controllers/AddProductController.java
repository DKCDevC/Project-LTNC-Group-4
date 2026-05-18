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

/**
 * Lớp AddProductController điều khiển logic giao diện Thêm sản phẩm mới (AddProduct.fxml) của Người bán.
 * Hỗ trợ các chức năng:
 * - Nhập thông tin chi tiết sản phẩm và chọn loại mặt hàng.
 * - Định dạng phân tách phần nghìn hiển thị tiền tệ (VND) trực quan theo thời gian thực (Real-time formatting).
 * - Tải lên và hiển thị tối đa 5 hình ảnh/video xem trước (Thumbnail previews).
 * - Truyền dữ liệu bất đồng bộ qua Socket TCP tới Server để lưu trữ.
 */
public class AddProductController {

    // Liên kết các thành phần UI từ FXML
    @FXML private TextField txtProductName;
    @FXML private TextField txtStartPrice;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cboCategory;
    @FXML private TextField txtDurationValue;
    @FXML private ComboBox<String> cboDurationUnit;
    @FXML private Label lblStatus;
    @FXML private FlowPane photoContainer; // Khung chứa danh sách ảnh xem trước

    // Tên tài khoản người đăng bán sản phẩm
    private String sellerName;
    
    // Danh sách lưu trữ đường dẫn ảnh cục bộ được chọn
    private List<String> imagePaths = new ArrayList<>();

    /**
     * Chuyển đổi tên danh mục hiển thị trên giao diện Tiếng Việt thành hằng số chuỗi tương ứng ở Server.
     * @return Mã loại sản phẩm tương thích với DB
     */
    private String getCategoryType() {
        if (cboCategory.getValue() == null) return "GENERAL";
        switch (cboCategory.getValue()) {
            case "Đồ điện tử": return "ELECTRONICS";
            case "Nghệ thuật": return "ART";
            case "Xe cộ": return "VEHICLE";
            default: return "GENERAL";
        }
    }

    /**
     * Phương thức khởi tạo cấu hình mặc định cho các ComboBox và cài đặt bộ lắng nghe định dạng tiền tệ.
     */
    @FXML
    public void initialize() {
        // Nạp danh sách danh mục sản phẩm đấu giá
        cboCategory.setItems(FXCollections.observableArrayList(
                "Khác (Chung)",
                "Đồ điện tử",
                "Nghệ thuật",
                "Xe cộ"
        ));
        cboCategory.setValue("Khác (Chung)");

        // Nạp danh sách đơn vị thời gian phiên đấu giá
        cboDurationUnit.setItems(FXCollections.observableArrayList(
                "Phút",
                "Giờ",
                "Ngày"
        ));
        cboDurationUnit.setValue("Ngày");
        txtDurationValue.setText("7");

        // --- BỘ LẮNG NGHE ĐỊNH DẠNG SỐ TIỀN ĐỘNG (Auto-format Price with commas) ---
        // Giúp người dùng dễ dàng đọc số tiền lớn (ví dụ: 10,000,000 thay vì 10000000) khi gõ phím
        txtStartPrice.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;
            
            // Loại bỏ tất cả ký tự không phải là chữ số
            String digits = newValue.replaceAll("[^\\d]", "");
            if (digits.isEmpty()) {
                txtStartPrice.setText("");
                return;
            }
            
            try {
                long value = Long.parseLong(digits);
                // Định dạng số tiền có dấu phân cách phần nghìn
                String formatted = String.format("%,d", value).replace(',', ','); 
                if (!newValue.equals(formatted)) {
                    txtStartPrice.setText(formatted);
                    // Giữ vị trí con trỏ chuột (Cursor caret) nằm cuối chuỗi sau khi định dạng lại văn bản
                    Platform.runLater(() -> txtStartPrice.positionCaret(formatted.length()));
                }
            } catch (Exception e) {}
        });
    }

    /**
     * Thiết lập tên người bán được truyền từ Seller Dashboard.
     * @param sellerName Tên người bán
     */
    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    /**
     * Xử lý tải ảnh sản phẩm từ máy tính (Multi-file upload).
     * @param event Sự kiện click nút bấm
     */
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
        // Cho phép người dùng chọn nhiều ảnh cùng lúc
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);

        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                // Ràng buộc giới hạn tối đa 5 tệp hình ảnh để bảo toàn giao diện
                if (imagePaths.size() >= 5) {
                    lblStatus.setText("Tối đa 5 ảnh!");
                    break;
                }
                String path = file.toURI().toString();
                imagePaths.add(path);

                // Tạo đối tượng ImageView hiển thị hình thu nhỏ (Thumbnail Preview)
                ImageView imageView = new ImageView(new Image(path));
                imageView.setFitWidth(100);
                imageView.setFitHeight(100);
                imageView.setPreserveRatio(true);
                
                // Đưa ảnh xem trước vào FlowPane container trên giao diện
                photoContainer.getChildren().add(imageView);
            }
        }
    }

    /**
     * Lưu thông tin sản phẩm và đăng bán lên máy chủ eBid.
     * @param event Sự kiện click nút bấm
     */
    @FXML
    public void handleSaveProduct(ActionEvent event) {
        String name = txtProductName.getText().trim();
        String priceStr = txtStartPrice.getText().trim();
        String desc = txtDescription.getText().trim();

        // Kiểm duyệt bắt buộc các thông tin cốt lõi
        if (name.isEmpty() || priceStr.isEmpty()) {
            lblStatus.setText("Vui lòng nhập các trường bắt buộc (*)");
            return;
        }

        try {
            // Loại bỏ toàn bộ dấu phẩy phân tách tiền tệ trước khi phân tích cú pháp kiểu Double gửi lên máy chủ
            String cleanPrice = priceStr.replaceAll("[^\\d]", "");
            double price = Double.parseDouble(cleanPrice);
            
            lblStatus.setStyle("-fx-text-fill: gray;");
            lblStatus.setText("Đang đẩy lên Server...");

            String type = getCategoryType();

            // Chuyển đổi mảng danh sách URL ảnh thành chuỗi phẳng phân cách bởi dấu phẩy để lưu DB dễ dàng
            String extraImages = String.join(",", imagePaths);

            // Thu thập thời lượng đấu giá
            int durationValue = 7;
            try {
                durationValue = Integer.parseInt(txtDurationValue.getText().trim());
            } catch (Exception ex) {}
            
            String durationUnit = cboDurationUnit.getValue();
            
            final int finalDurationValue = durationValue;
            final String finalDurationUnit = durationUnit;

            // Khởi chạy Thread phụ bất đồng bộ gửi yêu cầu ADD_ITEM qua Socket
            new Thread(() -> {
                try {
                    Socket socket = new Socket("127.0.0.1", 9999);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                    // Đóng gói tham số sản phẩm
                    JsonObject data = new JsonObject();
                    data.addProperty("type", type);
                    data.addProperty("name", name);
                    data.addProperty("desc", desc);
                    data.addProperty("price", price);
                    data.addProperty("extra", extraImages);
                    data.addProperty("seller", this.sellerName);
                    data.addProperty("durationValue", finalDurationValue);
                    data.addProperty("durationUnit", finalDurationUnit);

                    // Đóng gói gói tin lệnh trung tâm
                    JsonObject request = new JsonObject();
                    request.addProperty("command", "ADD_ITEM");
                    request.add("data", data);

                    out.println(new Gson().toJson(request));
                    socket.close();

                    // Sử dụng Platform.runLater thông báo thành công và đóng màn hình nhập liệu
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

    /**
     * Hủy bỏ thao tác thêm sản phẩm và đóng cửa sổ Dialog hiện hành.
     * @param event Sự kiện click nút bấm
     */
    @FXML
    public void handleCancel(ActionEvent event) {
        Stage stage = (Stage) txtProductName.getScene().getWindow();
        stage.close();
    }
}