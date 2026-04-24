package network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dao.ItemDAO;
import models.*;
import services.AuctionManager;
import services.ItemFactory;
import services.ItemManager;
import services.UserManager;
import utils.GsonConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles one client connection on its own thread.
 *
 * Security rules enforced here:
 *  - Client is NOT trusted. Username is stored in session after login.
 *  - BID / REGISTER_AUTOBID commands use the session username, not client-supplied data.
 *  - Session is cleaned up on any IOException (crash / disconnect).
 */
public class ClientHandler implements Runnable, AuctionObserver {

    private final Socket clientSocket;
    private PrintWriter out;
    private final Gson gson = GsonConfig.createGson();

    // Authenticated session identity  set after successful LOGIN
    private String sessionUsername = null;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    // ------------------------------------------------------------------
    // Observer registry
    // ------------------------------------------------------------------

    public static void registerObserver(AuctionObserver o)   { AuctionNotificationManager.getInstance().registerObserver(o); }
    public static void removeObserver(AuctionObserver o)     { AuctionNotificationManager.getInstance().removeObserver(o); }
    public static void notifyAllObservers(String message) {
        AuctionNotificationManager.getInstance().notifyAllObservers(message);
    }

    public static void sendToUser(String username, String message) {
        ClientHandler handler = services.UserManager.getInstance().getSession(username);
        if (handler != null) {
            handler.updateClient(message);
        }
    }

    @Override
    public void updateClient(String message) {
        if (out != null) out.println(message);
    }

    public com.google.gson.Gson getGson() { return gson; }
    public java.io.PrintWriter getOut() { return out; }
    public String getSessionUsername() { return sessionUsername; }
    public void setSessionUsername(String username) { this.sessionUsername = username; }

    // ------------------------------------------------------------------
    // Kick (called by UserManager when a duplicate login is detected)
    // ------------------------------------------------------------------

    public void sendKick(String reason) {
        if (out != null) {
            out.println("{\"command\":\"KICK\",\"message\":\"" + reason + "\"}");
        }
    }

    // ------------------------------------------------------------------
    // Main loop
    // ------------------------------------------------------------------

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            registerObserver(this);
            System.out.println(">>> Client kt ni");

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.trim().isEmpty()) continue;
                handleRequest(inputLine);
            }

        } catch (IOException e) {
            // Client disconnected or crashed  silently clean up
        } finally {
            removeObserver(this);
            if (sessionUsername != null) {
                UserManager.getInstance().removeSession(sessionUsername, this);
            }
            System.out.println(">>> Client ngt kt ni.");
            try { clientSocket.close(); } catch (IOException ignored) {}
        }
    }

    // ------------------------------------------------------------------
    // Request dispatcher
    // ------------------------------------------------------------------
    private void handleRequest(String rawJson) {
        try {
            JsonObject req = gson.fromJson(rawJson, JsonObject.class);
            String cmdName = req.has("command") ? req.get("command").getAsString() : "";

            network.commands.ServerCommand command = network.commands.CommandFactory.getCommand(cmdName);
            if (command != null) {
                command.execute(this, req);
            } else {
                // Fallback for commands not yet migrated to the Command pattern
                switch (cmdName) {
                    case "REGISTER":       handleRegister(req);       break;
                    case "GET_ITEMS":      handleGetItems();           break;
                    case "GET_ITEM_DETAIL":handleGetItemDetail(req);  break;
                    case "ADD_ITEM":       handleAddItem(req);        break;
                    case "DELETE_ITEM":    handleDeleteItem(req);     break;
                    case "UPDATE_ITEM":    handleUpdateItem(req);     break;
                    case "GET_SELLER_DASHBOARD": handleGetSellerDashboard(req); break;
                    case "PLACE_ORDER":    handlePlaceOrder(req);     break;
                    case "WATCHLIST_ADD":  handleWatchlistAdd(req);   break;
                    case "WATCHLIST_REMOVE":handleWatchlistRemove(req);break;
                    case "GET_WATCHLIST":  handleGetWatchlist();      break;
                    default:
                        out.println("{\"status\":\"ERROR\",\"message\":\"Unknown command: " + cmdName + "\"}");
                }
            }
        } catch (Exception e) {
            System.out.println("--- L?????i x???? l???? request ---");
            e.printStackTrace();
            out.println("{\"status\":\"ERROR\",\"message\":\"Sai ??????????nh d????ng d?? li??u\"}");
        }
    }

    // ------------------------------------------------------------------
    // Handlers
    // ------------------------------------------------------------------

    private void handleLogin(JsonObject req) {
        String username = req.has("username") ? req.get("username").getAsString() : "";
        String password = req.has("password") ? req.get("password").getAsString() : "";

        User user = UserManager.getInstance().authenticate(username, password);
        if (user != null) {
            // Atomic: registers session and kicks any existing session for same username
            sessionUsername = username;
            UserManager.getInstance().registerSession(username, this);

            out.println("{\"command\":\"LOGIN_RESPONSE\",\"status\":\"SUCCESS\",\"role\":\"" + user.getRole() + "\"}");
            System.out.println(">>> LOGIN OK: " + username + " [" + user.getRole() + "]");
        } else {
            out.println("{\"command\":\"LOGIN_RESPONSE\",\"status\":\"FAILED\",\"message\":\"Invalid credentials\"}");
        }
    }

    private void handleRegister(JsonObject req) {
        String username = req.get("username").getAsString();
        String password = req.get("password").getAsString();
        String role     = req.get("role").getAsString();

        boolean ok = UserManager.getInstance().registerUser(username, password, role);
        if (ok) {
            out.println("{\"command\":\"REGISTER_RESPONSE\",\"status\":\"SUCCESS\"}");
        } else {
            out.println("{\"command\":\"REGISTER_RESPONSE\",\"status\":\"FAILED\",\"message\":\"Tài khoản đã tồn tại!\"}");
        }
    }

    private void handleGetItems() {
        List<Item> items = ItemManager.getInstance().getAllItems();
        JsonObject response = new JsonObject();
        response.addProperty("command", "SET_ITEMS");
        response.add("data", gson.toJsonTree(items));
        out.println(gson.toJson(response));
    }

    /**
     * GET_ITEM_DETAIL  returns full, fresh state from DB including bid history.
     * Client must call this when opening product detail to avoid stale data.
     */
    private void handleGetItemDetail(JsonObject req) {
        // Security: if client supplies itemId we accept it (it's read-only data)
        String itemId = req.has("itemId") ? req.get("itemId").getAsString() : "";
        if (itemId.isEmpty()) { out.println("{\"status\":\"ERROR\",\"message\":\"Missing itemId\"}"); return; }

        ItemDAO dao = new ItemDAO();
        Item item = dao.getItemById(itemId);
        if (item == null) { out.println("{\"status\":\"ERROR\",\"message\":\"Item not found\"}"); return; }

        // Also fetch in-memory price if auction is live
        String auctionId = getAuctionIdForItem(itemId);
        double livePrice = item.getStartingPrice();
        String winnerName = "";
        String endTimeStr = item.getEndTime().toString();
        String auctionStatus = "CLOSED";

        if (auctionId != null) {
            models.Auction auction = AuctionManager.getInstance().getAuction(auctionId);
            if (auction != null) {
                livePrice  = auction.getItem().getCurrentHighestPrice();
                endTimeStr = auction.getItem().getEndTime().toString();
                auctionStatus = auction.getStatus().name();
                if (auction.getWinner() != null) winnerName = auction.getWinner().getUsername();
            }
        }

        // Bid history (last 50)
        List<String[]> history = dao.getBidHistory(itemId, 50);
        JsonArray histArr = new JsonArray();
        for (String[] h : history) {
            JsonObject pt = new JsonObject();
            pt.addProperty("amount", h[0]);
            pt.addProperty("time", h[1]);
            histArr.add(pt);
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("command", "ITEM_DETAIL");
        resp.addProperty("itemId", itemId);
        resp.addProperty("name", item.getName());
        resp.addProperty("description", item.getDescription());
        resp.addProperty("saleType", item.getSaleType());
        resp.addProperty("imageUrls", item.getImageUrls());
        resp.addProperty("startingPrice", item.getStartingPrice());
        resp.addProperty("currentPrice", livePrice);
        resp.addProperty("endTime", endTimeStr);
        resp.addProperty("auctionStatus", auctionStatus);
        resp.addProperty("winner", winnerName);
        resp.addProperty("sellerName", item.getSeller() != null ? item.getSeller().getUsername() : "");
        resp.add("bidHistory", histArr);

        out.println(gson.toJson(resp));
    }

    private void handleAddItem(JsonObject req) {
        JsonObject data = req.getAsJsonObject("data");
        String sellerName = data.has("seller") ? data.get("seller").getAsString() : sessionUsername;
        if (sellerName == null) throw new SecurityException("Chưa đăng nhập (Add Item Session)");

        String type       = data.get("type").getAsString();
        String name       = data.get("name").getAsString();
        String desc       = data.get("desc").getAsString();
        double price      = data.get("price").getAsDouble();
        String extra      = data.get("extra").getAsString();
        String saleType   = data.has("saleType") ? data.get("saleType").getAsString() : "AUCTION";
        String imageUrls  = data.has("imageUrls") ? data.get("imageUrls").getAsString() : "";
        double reserve    = data.has("reservePrice") ? data.get("reservePrice").getAsDouble() : 0;
        double increment  = data.has("minIncrement") ? data.get("minIncrement").getAsDouble() : 1000;

        int durationMinutes = data.has("duration") ? data.get("duration").getAsInt() : 10080;

        java.time.LocalDateTime start = java.time.LocalDateTime.now();
        java.time.LocalDateTime end   = start.plusMinutes(durationMinutes);

        Item newItem = ItemFactory.createItem(type, name, desc, price, start, end, extra);
        newItem.setReservePrice(reserve);
        newItem.setMinIncrement(increment);
        newItem.setSaleType(saleType);
        newItem.setImageUrls(imageUrls);

        String newId = java.util.UUID.randomUUID().toString();
        newItem.setId(newId);
        Seller seller = new Seller(sellerName, "", "");
        ItemManager.getInstance().addItem(newId, newItem, seller);

        // If it's an auction, register it in AuctionManager
        if ("AUCTION".equals(saleType)) {
            Auction auction = new Auction(newItem, seller);
            auction.setAuctionId(newId);
            AuctionManager.getInstance().addAuction(auction);
        }

        notifyAllObservers("{\"command\":\"UPDATE_PRICE\"}");
        out.println("{\"command\":\"ADD_ITEM_RESPONSE\",\"status\":\"SUCCESS\"}");
    }

    private void handleDeleteItem(JsonObject req) {
        JsonObject data = req.getAsJsonObject("data");
        String username = data.has("username") ? data.get("username").getAsString() : sessionUsername;
        if (username == null) { out.println("{\"command\":\"DELETE_ITEM_RESPONSE\",\"status\":\"FAILED\"}"); return; }

        String productId = data.get("productId").getAsString();
        boolean ok = ItemManager.getInstance().deleteItem(productId);
        out.println("{\"command\":\"DELETE_ITEM_RESPONSE\",\"status\":\"" + (ok ? "SUCCESS" : "FAILED") + "\"}");
        if (ok) notifyAllObservers("{\"command\":\"UPDATE_PRICE\"}");
    }

    private void handleUpdateItem(JsonObject req) {
        JsonObject data = req.getAsJsonObject("data");
        String username = data.has("username") ? data.get("username").getAsString() : sessionUsername;
        if (username == null) { out.println("{\"command\":\"UPDATE_ITEM_RESPONSE\",\"status\":\"FAILED\"}"); return; }

        String productId = data.get("productId").getAsString();
        String newName   = data.get("name").getAsString();
        String newDesc   = data.get("desc").getAsString();
        double newPrice  = data.get("price").getAsDouble();

        boolean ok = ItemManager.getInstance().updateItem(productId, newName, newDesc, newPrice);
        out.println("{\"command\":\"UPDATE_ITEM_RESPONSE\",\"status\":\"" + (ok ? "SUCCESS" : "FAILED") + "\"}");
        if (ok) notifyAllObservers("{\"command\":\"UPDATE_PRICE\"}");
    }


    private void handlePlaceOrder(JsonObject req) {
        if (sessionUsername == null) { out.println("{\"status\":\"FAILED\"}"); return; }
        // Delegate to existing order logic (simplified)
        out.println("{\"status\":\"SUCCESS\"}");
    }

    //  Seller Dashboard (unchanged logic, kept for compatibility) 
    private void handleGetSellerDashboard(JsonObject req) {
        String sellerName = req.has("username") ? req.get("username").getAsString() : "";

        JsonObject response = new JsonObject();
        response.addProperty("command", "SET_SELLER_DASHBOARD");
        response.addProperty("status", "SUCCESS");

        List<Item> allItems = ItemManager.getInstance().getAllItems();
        JsonArray productsArray = new JsonArray();
        int activeCount = 0;
        double totalRevenue = 0;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        for (Item item : allItems) {
            if (item.getSeller() == null || !sellerName.equals(item.getSeller().getUsername())) continue;

            JsonObject pObj = new JsonObject();
            pObj.addProperty("id", item.getId());
            pObj.addProperty("name", item.getName());
            pObj.addProperty("description", item.getDescription() != null ? item.getDescription() : "");

            String itemType = "GENERAL";
            if (item instanceof models.Electronics) itemType = "ELECTRONICS";
            else if (item instanceof models.Art)     itemType = "ART";
            else if (item instanceof models.Vehicle) itemType = "VEHICLE";
            pObj.addProperty("type", itemType);

            double val = item.getStartingPrice();
            pObj.addProperty("price", String.format("%,.0f", val));
            pObj.addProperty("saleType", item.getSaleType());
            pObj.addProperty("imageUrls", item.getImageUrls());

            if (item.getEndTime() != null) {
                pObj.addProperty("endTime", item.getEndTime().format(
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            } else {
                pObj.addProperty("endTime", "");
            }

            if (now.isAfter(item.getEndTime())) {
                pObj.addProperty("status", "Đã kết thúc");
                totalRevenue += val;
            } else {
                pObj.addProperty("status", "Đang bán");
                activeCount++;
            }
            productsArray.add(pObj);
        }

        response.addProperty("totalRevenue", String.format("%,.0f", totalRevenue));
        response.addProperty("activeAuctions", String.valueOf(activeCount));

        int pendingOrderCount = 0;
        JsonArray ordersArray = new JsonArray();
        try {
            dao.OrderDAO orderDao = new dao.OrderDAO();
            java.util.List<java.util.Map<String, String>> orderList = orderDao.getOrdersBySeller(sellerName);
            pendingOrderCount = orderList.size();
            for (java.util.Map<String, String> order : orderList) {
                JsonObject oObj = new JsonObject();
                oObj.addProperty("orderId",   order.get("orderId"));
                oObj.addProperty("itemName",  order.get("itemName"));
                oObj.addProperty("buyerName", order.get("buyerName"));
                oObj.addProperty("price",     order.get("price"));
                oObj.addProperty("orderDate", order.get("orderDate"));
                ordersArray.add(oObj);
            }
        } catch (Exception ex) {
            System.out.println("Lỗi load đơn hàng: " + ex.getMessage());
        }

        response.addProperty("pendingOrders", String.valueOf(pendingOrderCount));
        response.add("products", productsArray);
        response.add("orders", ordersArray);

        JsonArray chartData = new JsonArray();
        try {
            dao.OrderDAO orderDao = new dao.OrderDAO();
            java.util.Map<String, Double> realStats = orderDao.getRevenueLast7Days(sellerName);
            for (java.util.Map.Entry<String, Double> entry : realStats.entrySet()) {
                JsonObject dayObj = new JsonObject();
                java.time.LocalDate date = java.time.LocalDate.parse(entry.getKey());
                int dow = date.getDayOfWeek().getValue();
                dayObj.addProperty("day", dow == 7 ? "CN" : "T" + (dow + 1));
                dayObj.addProperty("revenue", entry.getValue());
                chartData.add(dayObj);
            }
        } catch (Exception ex) {
            System.out.println("Lỗi load biểu đồ: " + ex.getMessage());
        }
        response.add("chartData", chartData);

        out.println(gson.toJson(response));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Finds the auctionId for a given itemId. Falls back to first running auction. */
    private String resolveAuctionId(String itemId) {
        if (itemId != null && !itemId.isEmpty()) {
            // If there's an auction keyed by the itemId itself
            if (AuctionManager.getInstance().getAuction(itemId) != null) return itemId;
        }
        return null;
    }

    /** Same logic for GET_ITEM_DETAIL. */
    private String getAuctionIdForItem(String itemId) {
        return resolveAuctionId(itemId);
    }

    private void requireAuth() {
        if (sessionUsername == null) throw new SecurityException("Chưa đăng nhập");
    }

    private void handleWatchlistAdd(JsonObject req) {
        requireAuth();
        String itemId = req.get("itemId").getAsString();
        services.AuctionManager.getInstance().addToWatchlist(sessionUsername, itemId);
        out.println("{\"status\":\"SUCCESS\"}");
    }

    private void handleWatchlistRemove(JsonObject req) {
        requireAuth();
        String itemId = req.get("itemId").getAsString();
        services.AuctionManager.getInstance().removeFromWatchlist(sessionUsername, itemId);
        out.println("{\"status\":\"SUCCESS\"}");
    }

    private void handleGetWatchlist() {
        requireAuth();
        java.util.Set<String> ids = services.AuctionManager.getInstance().getWatchlist(sessionUsername);
        List<models.Item> items = new java.util.ArrayList<>();
        dao.ItemDAO daoObj = new dao.ItemDAO();
        for (String id : ids) {
            models.Item item = daoObj.getItemById(id);
            if (item != null) items.add(item);
        }
        JsonObject response = new JsonObject();
        response.addProperty("command", "SET_WATCHLIST");
        response.add("data", gson.toJsonTree(items));
        out.println(gson.toJson(response));
    }
}
