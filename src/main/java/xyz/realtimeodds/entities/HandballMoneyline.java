package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record HandballMoneyline(
        String id,
        boolean isSynthetic,
        Map<String, Selection> selections,
        String homeTeam,
        String awayTeam,
        String period) implements Market {

    public HandballMoneyline { selections = Map.copyOf(selections); }

    @JsonCreator
    public static HandballMoneyline of(
            @JsonProperty("id") String id,
            @JsonProperty("isSynthetic") boolean isSynthetic,
            @JsonProperty("selections") List<Selection> selections,
            @JsonProperty("homeTeam") String homeTeam,
            @JsonProperty("awayTeam") String awayTeam,
            @JsonProperty("period") String period) {
        Map<String, Selection> map = toMap(selections);
        return new HandballMoneyline(id, isSynthetic, map, homeTeam, awayTeam, period == null ? "full_match" : period);
    }

    private static Map<String, Selection> toMap(List<Selection> selections) {
        Map<String, Selection> map = new LinkedHashMap<>();
        if (selections != null) for (Selection s : selections) map.put(s.id(), s);
        return map;
    }

    @Override public MarketKind kind() { return MarketKind.HANDBALL_MONEYLINE; }
    @Override public SelectionKind selectionKind() { return SelectionKind.HOME_DRAW_AWAY; }
    @Override public String category() { return "Moneyline"; }

    @Override
    public String getSelectionName(SelectionResult result) {
        return switch (result) {
            case HOME -> homeTeam + " vainqueur" + PeriodLabels.label(period);
            case AWAY -> awayTeam + " vainqueur" + PeriodLabels.label(period);
            case DRAW -> "Match nul";
            default -> throw new IllegalArgumentException("Invalid selection result: " + result);
        };
    }

    @Override public Market withSelections(Map<String, Selection> newSelections) {
        return new HandballMoneyline(id, isSynthetic, newSelections, homeTeam, awayTeam, period);
    }
}
