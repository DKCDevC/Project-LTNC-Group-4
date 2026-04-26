package dao;

import domain.repository.AdminRepository;
import models.*;
import utils.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminJDBCRepository implements AdminRepository {
    private static AdminJDBCRepository instance;

    private AdminJDBCRepository() {}

    public static synchronized AdminJDBCRepository getInstance() {
        if (instance == null) {
            instance = new AdminJDBCRepository();
        }
        return instance;
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String username = rs.getString("username");
                String email = rs.getString("email");
                String role = rs.getString("role");
                boolean isLocked = rs.getBoolean("isLocked");
                boolean isVerified = rs.getBoolean("isVerified");
                
                User user;
                if ("ADMIN".equalsIgnoreCase(role)) user = new Admin(username, "", email);
                else if ("SELLER".equalsIgnoreCase(role)) user = new Seller(username, "", email);
                else user = new Bidder(username, "", email);
                
                user.setLocked(isLocked);
                user.setVerified(isVerified);
                users.add(user);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return users;
    }

    @Override
    public boolean updateUserStatus(String username, boolean isLocked) {
        String query = "UPDATE users SET isLocked = ? WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setBoolean(1, isLocked);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    @Override
    public boolean verifySeller(String username, boolean isVerified) {
        String query = "UPDATE users SET isVerified = ? WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setBoolean(1, isVerified);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    @Override
    public boolean deleteUser(String username) {
        String query = "DELETE FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    @Override
    public List<Auction> getAllAuctions() {
        // Mocking for now as Auction storage might be in-memory or DB
        return new ArrayList<>(); 
    }

    @Override
    public boolean updateAuctionStatus(String auctionId, String status) {
        return true; // Simplified
    }

    @Override
    public boolean cancelAuction(String auctionId) {
        return true; // Simplified
    }

    @Override
    public List<BidTransaction> getTransactionHistory() {
        return new ArrayList<>();
    }

    @Override
    public List<String> getSystemLogs() {
        List<String> logs = new ArrayList<>();
        logs.add("2026-04-25 10:00:00 - User admin logged in");
        logs.add("2026-04-25 10:05:00 - Seller 'john' added new item 'Vintage Watch'");
        return logs;
    }
}
