package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SelectionResult {
    OVER("over"),
    UNDER("under"),
    HOME("home"),
    DRAW("draw"),
    AWAY("away"),
    COMPETITOR1("competitor1"),
    COMPETITOR2("competitor2");

    private final String value;

    SelectionResult(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static SelectionResult fromValue(String value) {
        for (SelectionResult r : values()) {
            if (r.value.equals(value)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown SelectionResult: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
