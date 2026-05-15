package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Discriminator for the SportEvent union. Format: {@code se:<sport_event_name>}.
 */
public enum SportEventKind {
    BASKETBALL_MATCH("se:basketball_match"),
    FOOTBALL_MATCH("se:football_match"),
    TENNIS_MATCH("se:tennis_match");

    private final String value;

    SportEventKind(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static SportEventKind fromValue(String value) {
        for (SportEventKind k : values()) {
            if (k.value.equals(value)) {
                return k;
            }
        }
        throw new IllegalArgumentException("Unknown SportEventKind: " + value);
    }

    public Sport sport() {
        return switch (this) {
            case BASKETBALL_MATCH -> Sport.BASKETBALL;
            case FOOTBALL_MATCH -> Sport.FOOTBALL;
            case TENNIS_MATCH -> Sport.TENNIS;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}
