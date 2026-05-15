package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BasketballPeriod {
    FULL_MATCH("full_match", ""),
    FIRST_HALF("1st_half", " (1ère mi-temps)"),
    SECOND_HALF("2nd_half", " (2ème mi-temps)"),
    FIRST_QUARTER("1st_quarter", " (1er quart-temps)"),
    SECOND_QUARTER("2nd_quarter", " (2ème quart-temps)"),
    THIRD_QUARTER("3rd_quarter", " (3ème quart-temps)"),
    FOURTH_QUARTER("4th_quarter", " (4ème quart-temps)"),
    OVERTIME("overtime", " (prolongation)");

    private final String value;
    private final String label;

    BasketballPeriod(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    @JsonCreator
    public static BasketballPeriod fromValue(String value) {
        for (BasketballPeriod p : values()) {
            if (p.value.equals(value)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown BasketballPeriod: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
