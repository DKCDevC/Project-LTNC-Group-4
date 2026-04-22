package models;

import java.time.LocalDateTime;

public class Vehicle extends Item {
    private String brand;

    public Vehicle(String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime, String brand) {
        super(name, description, startingPrice, startTime, endTime);
        this.brand = brand;
    }

    @Override
    public void printInfo() {
        System.out.println("[Phương Tiện] " + getName() + " | Hãng: " + brand + " | Giá khởi điểm: " + getStartingPrice());
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
}