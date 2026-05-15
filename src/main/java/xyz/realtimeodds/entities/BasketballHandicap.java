package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Handicap market for a basketball match. Handicap applied to the home team. */
public record BasketballHandicap(
        String id,
        boolean isSynthetic,
        Map<String, Selection> selections,
        String homeTeam,
        String awayTeam,
        BasketballPeriod period,
        double handicap) implements Market {

    public BasketballHandicap {
        selections = Map.copyOf(selections);
    }

    @JsonCreator
    public static BasketballHandicap of(
            @JsonProperty("id") String id,
            @JsonProperty("isSynthetic") boolean isSynthetic,
            @JsonProperty("selections") List<Selection> selections,
            @JsonProperty("homeTeam") String homeTeam,
            @JsonProperty("awayTeam") String awayTeam,
            @JsonProperty("period") BasketballPeriod period,
            @JsonProperty("handicap") double handicap) {
        Map<String, Selection> map = new LinkedHashMap<>();
        if (selections != null) {
            for (Selection s : selections) {
                map.put(s.id(), s);
            }
        }
        return new BasketballHandicap(id, isSynthetic, map, homeTeam, awayTeam, period, handicap);
    }

    @Override
    public MarketKind kind() {
        return MarketKind.BASKETBALL_HANDICAP;
    }

    @Override
    public SelectionKind selectionKind() {
        return SelectionKind.HOME_AWAY;
    }

    @Override
    public String category() {
        return "Handicap";
    }

    private static String formatHandicap(double v) {
        return v > 0 ? "+" + v : String.valueOf(v);
    }

    @Override
    public String getSelectionName(SelectionResult result) {
        return switch (result) {
            case HOME -> homeTeam + " " + formatHandicap(handicap) + period.label();
            case AWAY -> awayTeam + " " + formatHandicap(-handicap) + period.label();
            default -> throw new IllegalArgumentException("Invalid selection result: " + result);
        };
    }

    @Override
    public Market withSelections(Map<String, Selection> newSelections) {
        return new BasketballHandicap(id, isSynthetic, newSelections, homeTeam, awayTeam, period, handicap);
    }
}
