package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record HockeyHandicap(
        String id,
        boolean isSynthetic,
        Map<String, Selection> selections,
        String homeTeam,
        String awayTeam,
        String period,
        double handicap) implements Market {

    public HockeyHandicap { selections = Map.copyOf(selections); }

    @JsonCreator
    public static HockeyHandicap of(
            @JsonProperty("id") String id,
            @JsonProperty("isSynthetic") boolean isSynthetic,
            @JsonProperty("selections") List<Selection> selections,
            @JsonProperty("homeTeam") String homeTeam,
            @JsonProperty("awayTeam") String awayTeam,
            @JsonProperty("period") String period,
            @JsonProperty("handicap") double handicap) {
        Map<String, Selection> map = toMap(selections);
        return new HockeyHandicap(id, isSynthetic, map, homeTeam, awayTeam, period == null ? "full_match" : period, handicap);
    }

    private static Map<String, Selection> toMap(List<Selection> selections) {
        Map<String, Selection> map = new LinkedHashMap<>();
        if (selections != null) for (Selection s : selections) map.put(s.id(), s);
        return map;
    }

    @Override public MarketKind kind() { return MarketKind.HOCKEY_HANDICAP; }
    @Override public SelectionKind selectionKind() { return SelectionKind.HOME_AWAY; }
    @Override public String category() { return "Handicap"; }
    private static String format(double v) { return v > 0 ? "+" + v : String.valueOf(v); }

    @Override
    public String getSelectionName(SelectionResult result) {
        return switch (result) {
            case HOME -> homeTeam + " " + format(handicap) + PeriodLabels.label(period);
            case AWAY -> awayTeam + " " + format(-handicap) + PeriodLabels.label(period);
            default -> throw new IllegalArgumentException("Invalid selection result: " + result);
        };
    }

    @Override public Market withSelections(Map<String, Selection> newSelections) {
        return new HockeyHandicap(id, isSynthetic, newSelections, homeTeam, awayTeam, period, handicap);
    }
}
