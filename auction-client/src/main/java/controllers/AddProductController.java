// 1. Khai báo package: Thuộc phân hệ Controllers quản lý luồng dữ liệu cho giao diện Client.
package controllers;

// 2. Import thư viện Google GSON và cấu phần UI JavaFX
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
 * 
 * Vai trò kiến trúc:
 * - Presentation Layer Controller (MVC Pattern): Nhận tương tác trực tiếp của người dùng từ View (FXML), 
 *   kiểm duyệt dữ liệu thô tại chỗ và đóng gói gửi tới máy chủ xử lý.
 * - Real-time input formatter: Định dạng tiền tệ theo phần nghìn (VND) trực quan ngay khi gõ phím.
 * - Multi-media previews: Cho phép duyệt chọn tối đa 5 file ảnh/video và tự động sinh thumbnail động xem trước.
 * - Thread-safe asynchronous networking: Chạy luồng phụ socket gửi gói tin ADD_ITEM để giữ luồng giao diện 
 *   chính (UI Application Thread) luôn mượt mà, không bị treo đơ (freeze).
 */
public class AddProductController {

    // Liên kết các thành phần UI từ FXML bằng chú thích @FXML (FXML Injection Bindings)
    @FXML private TextField txtProductName;
    @FXML private TextField txtStartPrice;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cboCategory;
    @FXML private TextField txtDurationValue;
    @FXML private ComboBox<String> cboDurationUnit;
    @FXML private Label lblStatus;
    @FXML private FlowPane photoContainer; // Khung chứa danh sách ảnh xem trước được sắp xếp dạng dòng tràn (Flow Layout)

    // Tên tài khoản người đăng bán sản phẩm
    private String sellerName;
    
    // Danh sách lưu trữ đường dẫn ảnh cục bộ được chọn
    private List<String> imagePaths = new ArrayList<>();

    /**
     * Chuyển đổi tên danh mục hiển thị tiếng Việt trên giao diện sang hằng số chuỗi tương ứng ở Server SQLite.
     * @return Mã loại sản phẩm tương thích cấu trúc Server
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
     * Được tự động gọi bởi JavaFX sau khi tệp FXML đã được nạp thành công trên RAM.
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
        // Giúp người dùng dễ dàng đọc số tiền lớn (ví dụ: 10,000,000 thay vì 10000000) khi gõ phím.
        // Cơ chế: Lắng nghe sự thay đổi của thuộc tính textProperty() của ô nhập giá khởi điểm.
        txtStartPrice.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;
            
            // Loại bỏ tất cả ký tự không phải là chữ số để thu thập số gốc
            String digits = newValue.replaceAll("[^\\d]", "");
            if (digits.isEmpty()) {
                txtStartPrice.setText("");
                return;
            }
            
            try {
                long value = Long.parseLong(digits);
                // Định dạng số tiền có dấu phân cách phần nghìn theo tiêu chuẩn
                String formatted = String.format("%,d", value).replace(',', ','); 
                if (!newValue.equals(formatted)) {
                    txtStartPrice.setText(formatted);
                    // Giữ vị trí con trỏ chuột (Cursor caret) nằm cuối chuỗi sau khi định dạng lại văn bản:
                    // Sử dụng Platform.runLater đưa tác vụ vị trí con trỏ vào hàng đợi xử lý tiếp theo của UI Thread.
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
     * 
     * Kỹ thuật JavaFX Stage và FileChooser:
     * - `FileChooser`: Mở hộp thoại chọn tệp tin mặc định của hệ điều hành.
     * - `ExtensionFilter`: Lọc định dạng để người dùng chỉ chọn được tệp tin ảnh hoặc video hợp lệ.
     * - `showOpenMultipleDialog`: Cho phép chọn nhiều tệp tin cùng một lúc, trả về danh sách `List<File>`.
     * - Thumbnail Generation (Sinh ảnh thu nhỏ): Tạo động đối tượng `ImageView` từ luồng ảnh cục bộ 
     *   và bơm nó vào khung chứa `photoContainer` để hiển thị lập tức kết quả trực quan cho người dùng.
     * 
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
                // Ràng buộc giới hạn tối đa 5 tệp hình ảnh để bảo toàn tính cân đối của bố cục giao diện
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
                imageView.setPreserveRatio(true); // Giữ nguyên tỉ lệ ảnh gốc, tránh méo mó
                
                // Đưa ảnh xem trước vào FlowPane container trên giao diện để kết xuất đồ họa
                photoContainer.getChildren().add(imageView);
            }
        }
    }

    /**
     * Lưu thông tin sản phẩm và đăng bán lên máy chủ eBid thông qua Socket TCP bất đồng bộ.
     * 
     * Kỹ thuật Threading và An toàn giao diện (UI Thread Safety):
     * - **Nguyên lý Đơn luồng JavaFX**: Mọi hoạt động giao tiếp mạng Socket (Blocking IO) tuyệt đối không được 
     *   chạy trực tiếp trên JavaFX Application Thread. Nếu không, toàn bộ màn hình sẽ bị "đóng băng" (freeze) 
     *   cho đến khi kết nối mạng hoàn tất.
     * - **Giải pháp Asynchronous Worker Thread**: Tạo một lớp luồng phụ `new Thread(() -> { ... }).start()` 
     *   để tự do thực hiện kết nối socket gửi tệp JSON thâu đêm suốt sáng ở chế độ chạy ngầm (Background).
     * - **Platform.runLater**: Khi luồng phụ hoàn tất giao tiếp socket và muốn cập nhật giao diện 
     *   (như đóng Stage, thay đổi nhãn lblStatus thành màu xanh), nó không được phép can thiệp trực tiếp mà phải 
     *   đóng gói lệnh vào phương thức `Platform.runLater` để đẩy về luồng UI Thread xử lý an toàn, tránh lỗi xung đột luồng đồ họa.
     * 
     * @param event Sự kiện click nút bấm
     */
    @FXML
    public void handleSaveProduct(ActionEvent event) {
        String name = txtProductName.getText().trim();
        String priceStr = txtStartPrice.getText().trim();
        String desc = txtDescription.getText().trim();

        // Kiểm duyệt bắt buộc điền các thông tin cốt lõi ban đầu
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

            // Thu thập thời lượng đấu giá từ màn hình nhập liệu
            int durationValue = 7;
            try {
                durationValue = Integer.parseInt(txtDurationValue.getText().trim());
            } catch (Exception ex) {}
            
            String durationUnit = cboDurationUnit.getValue();
            
            final int finalDurationValue = durationValue;
            final String finalDurationUnit = durationUnit;

            // Khởi chạy Thread phụ bất đồng bộ gửi yêu cầu ADD_ITEM qua Socket TCP ngầm
            new Thread(() -> {
                try {
                    // Thiết lập kết nối Socket TCP trực tiếp tới Server
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

                    // Đóng gói gói tin lệnh trung tâm eBid Protocol
                    JsonObject request = new JsonObject();
                    request.addProperty("command", "ADD_ITEM");
                    request.add("data", data);

                    // Tuần tự hóa JSON và gửi đi, kết thúc đóng luồng socket nhanh gọn
                    out.println(new Gson().toJson(request));
                    socket.close();

                    // Sử dụng Platform.runLater thông báo thành công và đóng màn hình nhập liệu an toàn
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