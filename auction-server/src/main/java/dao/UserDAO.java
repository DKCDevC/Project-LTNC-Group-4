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

public class UserDAO {

    public User loginUser(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String email = rs.getString("email");
                String role = rs.getString("role");

                if ("ADMIN".equalsIgnoreCase(role)) {
                    return new Admin(username, password, email);
                } else if ("SELLER".equalsIgnoreCase(role)) {
                    return new Seller(username, password, email);
                } else {
                    return new Bidder(username, password, email);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ???????????????? S????A: Th??????m email ??????o v??????o c??????u l??nh INSERT ?????????????? kh??????ng b???? SQLite ch??????n
    public boolean insertUser(String username, String password, String role) {
        String query = "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, username + "@gmail.com"); // T???? ??????????????ng t????o email ??????o
            stmt.setString(4, role);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("--- L?????I T??????I DATABASE ---");
            System.out.println(e.getMessage());
            return false;
        }
    }
}
