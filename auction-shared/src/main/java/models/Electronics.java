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
        System.out.println("[????i??n t????] " + getName() + " - B??????o h????nh: " + warrantyMonths + " th??ng - Gi?? hi??n t????i: " + getCurrentHighestPrice());
    }

    public int getWarrantyMonths() {
            return warrantyMonths;
    }
}
