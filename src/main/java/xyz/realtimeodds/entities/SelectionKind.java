package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;

public enum SelectionKind {
    OVER_UNDER("over/under", List.of(SelectionResult.OVER, SelectionResult.UNDER)),
    HOME_DRAW_AWAY("home/draw/away", List.of(SelectionResult.HOME, SelectionResult.DRAW, SelectionResult.AWAY)),
    HOME_AWAY("home/away", List.of(SelectionResult.HOME, SelectionResult.AWAY)),
    COMPETITOR1_COMPETITOR2("competitor1/competitor2", List.of(SelectionResult.COMPETITOR1, SelectionResult.COMPETITOR2));

    private final String value;
    private final List<SelectionResult> validResults;

    SelectionKind(String value, List<SelectionResult> validResults) {
        this.value = value;
        this.validResults = validResults;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public List<SelectionResult> validResults() {
        return validResults;
    }

    public int numberOfResults() {
        return validResults.size();
    }

    public boolean isValidResult(SelectionResult result) {
        return validResults.contains(result);
    }

    @JsonCreator
    public static SelectionKind fromValue(String value) {
        for (SelectionKind k : values()) {
            if (k.value.equals(value)) {
                return k;
            }
        }
        throw new IllegalArgumentException("Unknown SelectionKind: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
