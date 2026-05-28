package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TennisHandicap(
        String id,
        boolean isSynthetic,
        Map<String, Selection> selections,
        String competitor1,
        String competitor2,
        String period,
        String unit,
        double handicap) implements Market {

    public TennisHandicap { selections = Map.copyOf(selections); }

    @JsonCreator
    public static TennisHandicap of(
            @JsonProperty("id") String id,
            @JsonProperty("isSynthetic") boolean isSynthetic,
            @JsonProperty("selections") List<Selection> selections,
            @JsonProperty("competitor1") String competitor1,
            @JsonProperty("competitor2") String competitor2,
            @JsonProperty("period") String period,
            @JsonProperty("unit") String unit,
            @JsonProperty("handicap") double handicap) {
        Map<String, Selection> map = new LinkedHashMap<>();
        if (selections != null) for (Selection s : selections) map.put(s.id(), s);
        return new TennisHandicap(id, isSynthetic, map, competitor1, competitor2, period == null ? "full_match" : period, unit, handicap);
    }

    @Override public MarketKind kind() { return MarketKind.TENNIS_HANDICAP; }
    @Override public SelectionKind selectionKind() { return SelectionKind.COMPETITOR1_COMPETITOR2; }
    @Override public String category() { return "Handicap"; }
    private static String format(double v) { return v > 0 ? "+" + v : String.valueOf(v); }

    @Override
    public String getSelectionName(SelectionResult result) {
        return switch (result) {
            case COMPETITOR1 -> competitor1 + " " + format(handicap) + " " + unit + PeriodLabels.label(period);
            case COMPETITOR2 -> competitor2 + " " + format(-handicap) + " " + unit + PeriodLabels.label(period);
            default -> throw new IllegalArgumentException("Invalid selection result: " + result);
        };
    }

    @Override public Market withSelections(Map<String, Selection> newSelections) {
        return new TennisHandicap(id, isSynthetic, newSelections, competitor1, competitor2, period, unit, handicap);
    }
}
