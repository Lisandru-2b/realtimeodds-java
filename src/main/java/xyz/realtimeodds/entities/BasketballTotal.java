package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Total (over/under) market for a basketball match. */
public record BasketballTotal(
        String id,
        boolean isSynthetic,
        Map<String, Selection> selections,
        String homeTeam,
        String awayTeam,
        BasketballPeriod period,
        BasketballTotalScope scope,
        double cut) implements Market {

    public BasketballTotal {
        selections = Map.copyOf(selections);
    }

    @JsonCreator
    public static BasketballTotal of(
            @JsonProperty("id") String id,
            @JsonProperty("isSynthetic") boolean isSynthetic,
            @JsonProperty("selections") List<Selection> selections,
            @JsonProperty("homeTeam") String homeTeam,
            @JsonProperty("awayTeam") String awayTeam,
            @JsonProperty("period") BasketballPeriod period,
            @JsonProperty("scope") BasketballTotalScope scope,
            @JsonProperty("cut") double cut) {
        Map<String, Selection> map = new LinkedHashMap<>();
        if (selections != null) {
            for (Selection s : selections) {
                map.put(s.id(), s);
            }
        }
        return new BasketballTotal(id, isSynthetic, map, homeTeam, awayTeam, period, scope, cut);
    }

    @Override
    public MarketKind kind() {
        return MarketKind.BASKETBALL_TOTAL;
    }

    @Override
    public SelectionKind selectionKind() {
        return SelectionKind.OVER_UNDER;
    }

    @Override
    public String category() {
        return "Total";
    }

    private String scopeSubject() {
        return switch (scope) {
            case MATCH -> "Total combiné";
            case HOME -> homeTeam;
            case AWAY -> awayTeam;
        };
    }

    @Override
    public String getSelectionName(SelectionResult result) {
        if (result != SelectionResult.OVER && result != SelectionResult.UNDER) {
            throw new IllegalArgumentException("Invalid selection result: " + result);
        }
        String verb = result == SelectionResult.OVER ? "plus de" : "moins de";
        return scopeSubject() + " : " + verb + " " + cut + " points" + period.label();
    }

    @Override
    public Market withSelections(Map<String, Selection> newSelections) {
        return new BasketballTotal(id, isSynthetic, newSelections, homeTeam, awayTeam, period, scope, cut);
    }
}
