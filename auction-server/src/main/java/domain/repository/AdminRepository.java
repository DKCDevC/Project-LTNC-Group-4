package domain.repository;

import models.User;
import models.Auction;
import models.BidTransaction;
import java.util.List;

public interface AdminRepository {
    // User Management
    List<User> getAllUsers();
    boolean updateUserStatus(String username, boolean isLocked);
    boolean verifySeller(String username, boolean isVerified);
    boolean deleteUser(String username);

    // Auction Management
    List<Auction> getAllAuctions();
    boolean updateAuctionStatus(String auctionId, String status);
    boolean cancelAuction(String auctionId);

    // Transaction Management
    List<BidTransaction> getTransactionHistory();
    
    // System Monitoring
    List<String> getSystemLogs();
}
