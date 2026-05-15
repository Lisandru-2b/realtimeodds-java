package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BasketballTotalScope {
    MATCH("match"),
    HOME("home"),
    AWAY("away");

    private final String value;

    BasketballTotalScope(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static BasketballTotalScope fromValue(String value) {
        for (BasketballTotalScope s : values()) {
            if (s.value.equals(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown BasketballTotalScope: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
