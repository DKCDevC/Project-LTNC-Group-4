package network.command;

import com.google.gson.JsonObject;
import services.ItemManager;
import network.ClientHandler;
import java.io.PrintWriter;

public class DeleteItemCommand implements Command {
    @Override
    public void execute(JsonObject requestData, PrintWriter out, ClientHandler handler) {
        String productId = requestData.has("productId") ? requestData.get("productId").getAsString() : "";
        String username = handler.getCurrentUsername();

        if (username == null) {
            out.println("{\"status\":\"FAILED\", \"message\":\"Unauthorized: Please login first\"}");
            return;
        }

        boolean success = ItemManager.getInstance().deleteItem(productId, username);

        if (success) {
            out.println("{\"status\":\"SUCCESS\", \"message\":\"Da xoa san pham\"}");
        } else {
            out.println("{\"status\":\"FAILED\", \"message\":\"Khong the xoa: Sai quyen so huu hoac dau gia dang dien ra\"}");
        }
    }
}
