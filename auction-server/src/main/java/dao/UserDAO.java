package dao;

import models.Admin;
import models.Bidder;
import models.Seller;
import models.User;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO implements UserRepository {

    public User loginUser(String identifier, String password) {
        // Hỗ trợ đăng nhập bằng cả username hoặc email
        String query = "SELECT * FROM users WHERE (username = ? OR email = ?) AND password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, identifier);
            stmt.setString(2, identifier);
            stmt.setString(3, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String username = rs.getString("username");
                String email = rs.getString("email");
                String role = rs.getString("role");
                boolean isLocked = rs.getBoolean("isLocked");
                boolean isVerified = rs.getBoolean("isVerified");

                User user;
                if ("ADMIN".equalsIgnoreCase(role)) {
                    user = new Admin(username, password, email);
                } else if ("SELLER".equalsIgnoreCase(role)) {
                    user = new Seller(username, password, email);
                } else {
                    user = new Bidder(username, password, email);
                }
                
                user.setLocked(isLocked);
                user.setVerified(isVerified);
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insertUser(String username, String password, String email, String role) {
        String query = "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, email);
            stmt.setString(4, role);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("--- LỖI TẠI DATABASE ---");
            System.out.println(e.getMessage());
            return false;
        }
    }
}