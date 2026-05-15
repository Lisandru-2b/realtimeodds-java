package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** A single price level in an {@link OrderBook}. */
public record Level(double price, double size) {

    @JsonCreator
    public static Level of(
            @JsonProperty("price") double price,
            @JsonProperty("size") double size) {
        return new Level(price, size);
    }
}
