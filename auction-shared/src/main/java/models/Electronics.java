package models;

import java.time.LocalDateTime;

public class Electronics extends Item {
    private int warrantyMonths;

    public Electronics(String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime, int warrantyMonths) {
        super(name, description, startingPrice, startTime, endTime);
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void printInfo() {
        System.out.println("[Điện tử] " + getName() + " - Bảo hành: " + warrantyMonths + " tháng - Giá hiện tại: " + getCurrentHighestPrice());
    }

    public int getWarrantyMonths() {
            return warrantyMonths;
    }
}