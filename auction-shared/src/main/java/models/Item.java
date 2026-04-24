package models;

import java.time.LocalDateTime;

public abstract class Item extends Entity {
    private String name;
    private String description;
    private double startingPrice;
    private double currentHighestPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    protected Seller seller;
    // "AUCTION" or "BUY_NOW"
    private String saleType = "AUCTION";
    // comma-separated list of image URIs
    private String imageUrls = "";
    private double reservePrice = 0;
    private double minIncrement = 1000;

    public Item(String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentHighestPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public abstract void printInfo();

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }
    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public double getCurrentHighestPrice() {
        return currentHighestPrice;
    }

    public void setCurrentHighestPrice(double currentHighestPrice) {
        this.currentHighestPrice = currentHighestPrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Seller getSeller(){
        return seller;
    }

    public void setSeller(Seller seller){
        this.seller=seller;
    }

    public String getSaleType() { return saleType == null ? "AUCTION" : saleType; }
    public void setSaleType(String saleType) { this.saleType = saleType; }

    public String getImageUrls() { return imageUrls == null ? "" : imageUrls; }
    public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls != null ? imageUrls : ""; }

    public double getReservePrice() { return reservePrice; }
    public void setReservePrice(double reservePrice) { this.reservePrice = reservePrice; }

    public double getMinIncrement() { return minIncrement; }
    public void setMinIncrement(double minIncrement) { this.minIncrement = minIncrement; }

    /** Returns the first image URL from the comma-separated list, or empty string. */
    public String getFirstImageUrl() {
        if (imageUrls == null || imageUrls.isEmpty()) return "";
        String[] parts = imageUrls.split(",");
        return parts[0].trim();
    }
}
