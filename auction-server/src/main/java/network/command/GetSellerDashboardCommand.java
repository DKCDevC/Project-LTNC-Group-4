package network.command;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import services.DashboardService;
import utils.GsonConfig;
import network.ClientHandler;
import java.io.PrintWriter;

public class GetSellerDashboardCommand implements Command {
    private final Gson gson = GsonConfig.createGson();

    @Override
    public void execute(JsonObject requestData, PrintWriter out, ClientHandler handler) {
        String sellerName = (handler.getCurrentUsername() != null) ? 
                            handler.getCurrentUsername() : 
                            (requestData.has("username") ? requestData.get("username").getAsString() : "");
        
        if (sellerName.isEmpty()) {
            out.println("{\"status\":\"FAILED\", \"message\":\"Username is required\"}");
            return;
        }

        JsonObject dashboardData = DashboardService.getInstance().getSellerDashboardData(sellerName);
        out.println(gson.toJson(dashboardData));
    }
}
