package utils;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import models.*;

public class GsonConfig {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(formatter)))
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                        LocalDateTime.parse(json.getAsString(), formatter))
                .registerTypeAdapter(Item.class, (JsonDeserializer<Item>) (json, typeOfT, context) -> {
                    JsonObject jsonObject = json.getAsJsonObject();
                    // We can determine the type based on unique fields or a type property
                    // Since Item is abstract, we need to pick a concrete subclass
                    // For Admin view, we can use a generic implementation or check fields
                    if (jsonObject.has("warrantyMonths")) {
                        return context.deserialize(json, Electronics.class);
                    } else if (jsonObject.has("artistName")) {
                        return context.deserialize(json, Art.class);
                    } else if (jsonObject.has("brand")) {
                        return context.deserialize(json, Vehicle.class);
                    }
                    // Default to something or throw error
                    // If no specific fields, we might need a GeneralItem or similar
                    // Let's assume one of these for now based on what's in the project
                    return context.deserialize(json, Electronics.class); 
                })
                .create();
    }
}