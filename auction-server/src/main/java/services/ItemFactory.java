package services;

import models.Art;
import models.Electronics;
import models.GeneralItem;
import models.Item;
import models.Vehicle;

import java.time.LocalDateTime;

public class ItemFactory {

    public static Item createItem(String type, String name, String description, double startingPrice,
                                  LocalDateTime startTime, LocalDateTime endTime, String extraInfo) {

        if (type == null) type = "GENERAL";
        
        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                int warranty = 0;
                try { 
                    if (extraInfo != null && !extraInfo.isEmpty()) {
                        warranty = Integer.parseInt(extraInfo); 
                    }
                } catch (NumberFormatException e) { 
                    // Default to 0 if not a number
                }
                return new Electronics(name, description, startingPrice, startTime, endTime, warranty);

            case "ART":
                return new Art(name, description, startingPrice, startTime, endTime, extraInfo);

            case "VEHICLE":
                return new Vehicle(name, description, startingPrice, startTime, endTime, extraInfo);

            case "GENERAL":
            default:
                return new GeneralItem(name, description, startingPrice, startTime, endTime);
        }
    }
}
