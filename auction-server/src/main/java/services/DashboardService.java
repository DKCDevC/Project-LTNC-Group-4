package services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dao.OrderDAO;
import models.Item;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class DashboardService {
    private static DashboardService instance;

    private DashboardService() {}

    public static synchronized DashboardService getInstance() {
        if (instance == null) {
            instance = new DashboardService();
        }
        return instance;
    }

    public JsonObject getSellerDashboardData(String sellerName) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "SUCCESS");

        List<Item> allItems = ItemManager.getInstance().getAllItems();
        JsonArray productsArray = new JsonArray();

        int activeAuctionsCount = 0;
        double totalRevenue = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Item item : allItems) {
            if (item.getSeller() != null && sellerName.equals(item.getSeller().getUsername())) {
                JsonObject pObj = new JsonObject();
                pObj.addProperty("id", item.getId());
                pObj.addProperty("name", item.getName());
                pObj.addProperty("description", item.getDescription() != null ? item.getDescription() : "");

                String itemType = "GENERAL";
                if (item instanceof models.Electronics) itemType = "ELECTRONICS";
                else if (item instanceof models.Art) itemType = "ART";
                else if (item instanceof models.Vehicle) itemType = "VEHICLE";
                pObj.addProperty("type", itemType);

                double currentVal = item.getStartingPrice();
                pObj.addProperty("price", String.format("%,.0f", currentVal));

                if (item.getEndTime() != null) {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    pObj.addProperty("endTime", item.getEndTime().format(fmt));
                } else {
                    pObj.addProperty("endTime", "");
                }

                if (now.isAfter(item.getEndTime())) {
                    pObj.addProperty("status", "Đã kết thúc");
                    totalRevenue += currentVal;
                } else {
                    pObj.addProperty("status", "Đang đấu giá");
                    activeAuctionsCount++;
                }
                productsArray.add(pObj);
            }
        }

        response.addProperty("totalRevenue", String.format("%,.0f", totalRevenue));
        response.addProperty("activeAuctions", String.valueOf(activeAuctionsCount));

        JsonArray ordersArray = new JsonArray();
        OrderDAO orderDao = new OrderDAO();
        List<Map<String, String>> orderList = orderDao.getOrdersBySeller(sellerName);
        for (Map<String, String> order : orderList) {
            JsonObject oObj = new JsonObject();
            oObj.addProperty("orderId", order.get("orderId"));
            oObj.addProperty("itemName", order.get("itemName"));
            oObj.addProperty("buyerName", order.get("buyerName"));
            oObj.addProperty("price", order.get("price"));
            oObj.addProperty("orderDate", order.get("orderDate"));
            ordersArray.add(oObj);
        }

        response.addProperty("pendingOrders", String.valueOf(orderList.size()));
        response.add("products", productsArray);
        response.add("orders", ordersArray);

        JsonArray chartData = new JsonArray();
        Map<String, Double> realStats = orderDao.getRevenueLast7Days(sellerName);
        for (Map.Entry<String, Double> entry : realStats.entrySet()) {
            JsonObject dayObj = new JsonObject();
            java.time.LocalDate date = java.time.LocalDate.parse(entry.getKey());
            int dayOfWeek = date.getDayOfWeek().getValue();
            String label = (dayOfWeek == 7) ? "CN" : "T" + (dayOfWeek + 1);
            dayObj.addProperty("day", label);
            dayObj.addProperty("revenue", entry.getValue());
            chartData.add(dayObj);
        }
        response.add("chartData", chartData);

        return response;
    }
}
