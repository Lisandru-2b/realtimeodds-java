package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Sport family of a sport event. Derived from {@code SportEventKind};
 * not a wire field — exposed as a computed property on the {@link SportEvent} class.
 */
public enum Sport {
    AMERICAN_FOOTBALL("american_football"),
    BASEBALL("baseball"),
    BASKETBALL("basketball"),
    BOXING("boxing"),
    CRICKET("cricket"),
    FOOTBALL("football"),
    HANDBALL("handball"),
    HOCKEY("hockey"),
    MMA("mma"),
    RUGBY_LEAGUE("rugby_league"),
    TENNIS("tennis");

    private final String value;

    Sport(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static Sport fromValue(String value) {
        for (Sport s : values()) {
            if (s.value.equals(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown sport: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
