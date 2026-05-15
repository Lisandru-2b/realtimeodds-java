package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A decimal-odds quote at a point in time.
 *
 * <p>{@code timestamp} is in milliseconds since epoch — observation time set
 * by whichever party constructed the Quote (gateway, then SDK at hydration).
 * Approximates freshness; not the bookmaker's authoritative emit time.
 *
 * @param price decimal odds (&gt; 1.0). Implied probability = {@code 1/price}.
 * @param size  available stake at this price (may be {@code null}).
 * @param timestamp Unix timestamp in ms.
 */
public record Quote(
        double price,
        Double size,
        long timestamp) {

    public Quote {
        if (price <= 1.0) {
            throw new IllegalArgumentException("Quote price must be > 1.0, got " + price);
        }
    }

    @JsonCreator
    public static Quote of(
            @JsonProperty("price") double price,
            @JsonProperty("size") Double size,
            @JsonProperty("timestamp") long timestamp) {
        return new Quote(price, size, timestamp);
    }

    /** {@code 1 / price}. */
    public double impliedProbability() {
        return 1.0 / price;
    }
}
