package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PlayerPropType {
    POINTS("points", "marque", "points"),
    REBOUNDS("rebounds", "effectue", "rebonds"),
    ASSISTS("assists", "fait", "passes décisives"),
    THREES("threes", "marque", "tirs à trois points"),
    STEALS("steals", "fait", "interceptions"),
    BLOCKS("blocks", "fait", "contres"),
    POINTS_REBOUNDS("points_rebounds", "effectue", "points + rebonds"),
    POINTS_ASSISTS("points_assists", "effectue", "points + passes décisives"),
    REBOUNDS_ASSISTS("rebounds_assists", "effectue", "rebonds + passes décisives"),
    POINTS_REBOUNDS_ASSISTS("points_rebounds_assists", "effectue", "points + rebonds + passes décisives"),
    OTHER("other", "effectue", "autre");

    private final String value;
    private final String verb;
    private final String label;

    PlayerPropType(String value, String verb, String label) {
        this.value = value;
        this.verb = verb;
        this.label = label;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public String verb() {
        return verb;
    }

    public String label() {
        return label;
    }

    @JsonCreator
    public static PlayerPropType fromValue(String value) {
        for (PlayerPropType p : values()) {
            if (p.value.equals(value)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown PlayerPropType: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
