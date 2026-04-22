package services;

import models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class AuctionManagerTest {
    private AuctionManager manager;
    private Auction auction;
    private Bidder bidder1;
    private Bidder bidder2;

    @BeforeEach
    void setUp() {
        manager = AuctionManager.getInstance();

        Seller seller = new Seller("seller", "123", "s@test.com");
        Item laptop = new Electronics("Laptop", "Test", 1000,
                LocalDateTime.now(), LocalDateTime.now().plusSeconds(10), 12);

        auction = new Auction(laptop, seller);
        auction.setAuctionId("test_001");
        manager.addAuction(auction);

        bidder1 = new Bidder("user1", "123", "u1@test.com");
        bidder2 = new Bidder("user2", "123", "u2@test.com");
    }

    @Test
    void testValidBid() {
        boolean result = manager.placeBid("test_001", bidder1, 2000);
        assertTrue(result, "Giá 2000 phải lớn hơn giá khởi điểm 1000");
        assertEquals(2000, auction.getItem().getCurrentHighestPrice());
    }

    @Test
    void testInvalidLowerBid() {
        manager.placeBid("test_001", bidder1, 2000);
        boolean result = manager.placeBid("test_001", bidder2, 1500);
        assertFalse(result, "Không được đặt giá 1500 khi giá hiện tại là 2000");
    }

    @Test
    void testAntiSnipingExtension() {
        LocalDateTime oldEndTime = auction.getItem().getEndTime();

        manager.placeBid("test_001", bidder1, 3000);

        LocalDateTime newEndTime = auction.getItem().getEndTime();
        assertTrue(newEndTime.isAfter(oldEndTime), "Thời gian kết thúc phải được gia hạn");
    }
}