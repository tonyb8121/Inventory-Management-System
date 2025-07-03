package com.inventory.ui.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Custom Gson TypeAdapter for serializing and deserializing LocalDateTime objects.
 * This ensures that LocalDateTime objects are converted to/from ISO 8601 strings,
 * which is a standard format compatible with Spring Boot's default JSON handling for time.
 */
public class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

    // Define the formatter to use for ISO Local Date Time format (e.g., "yyyy-MM-ddTHH:mm:ss")
    // IMPORTANT: Changed to public static final so it can be accessed from other classes like ApiClient.
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Serializes a LocalDateTime object to a JSON Primitive (String).
     * @param src The LocalDateTime object to serialize.
     * @param typeOfSrc The type of the source object.
     * @param context The serialization context.
     * @return A JsonPrimitive representing the LocalDateTime as an ISO 8601 string.
     */
    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
        // Format the LocalDateTime object into an ISO_LOCAL_DATE_TIME string
        return new JsonPrimitive(FORMATTER.format(src));
    }

    /**
     * Deserializes a JSON Element (String) to a LocalDateTime object.
     * @param json The JsonElement to deserialize (expected to be a string).
     * @param typeOfT The type of the target object.
     * @param context The deserialization context.
     * @return A LocalDateTime object parsed from the JSON string.
     * @throws JsonParseException If the JSON element cannot be parsed into a LocalDateTime.
     */
    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        // Parse the JSON string into a LocalDateTime object using the defined formatter
        return LocalDateTime.parse(json.getAsString(), FORMATTER);
    }
}
