package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Bookmaker identifier. Derived from a {@code SportEventId}
 * (format {@code vmid:<bookmaker>:<external_id>}); not a wire field on the
 * entity itself.
 */
public enum Bookmaker {
    PS3838("ps3838"),
    WINAMAX("winamax"),
    BETCLIC("betclic"),
    PARIONS_SPORT("parions_sport"),
    UNIBET("unibet"),
    STAKE("stake"),
    POLYMARKET("polymarket");

    private final String value;

    Bookmaker(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static Bookmaker fromValue(String value) {
        for (Bookmaker b : values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unknown bookmaker: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
