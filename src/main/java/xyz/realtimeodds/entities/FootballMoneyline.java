package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 1X2 market for a football match (home / draw / away). */
public record FootballMoneyline(
        String id,
        boolean isSynthetic,
        Map<String, Selection> selections,
        String homeTeam,
        String awayTeam) implements Market {

    public FootballMoneyline {
        selections = Map.copyOf(selections);
    }

    @JsonCreator
    public static FootballMoneyline of(
            @JsonProperty("id") String id,
            @JsonProperty("isSynthetic") boolean isSynthetic,
            @JsonProperty("selections") List<Selection> selections,
            @JsonProperty("homeTeam") String homeTeam,
            @JsonProperty("awayTeam") String awayTeam) {
        Map<String, Selection> map = new LinkedHashMap<>();
        if (selections != null) {
            for (Selection s : selections) {
                map.put(s.id(), s);
            }
        }
        return new FootballMoneyline(id, isSynthetic, map, homeTeam, awayTeam);
    }

    @Override
    public MarketKind kind() {
        return MarketKind.FOOTBALL_MONEYLINE;
    }

    @Override
    public SelectionKind selectionKind() {
        return SelectionKind.HOME_DRAW_AWAY;
    }

    @Override
    public String category() {
        return "Moneyline";
    }

    @Override
    public String getSelectionName(SelectionResult result) {
        return switch (result) {
            case HOME -> homeTeam + " vainqueur";
            case AWAY -> awayTeam + " vainqueur";
            case DRAW -> "Match nul";
            default -> throw new IllegalArgumentException("Invalid selection result: " + result);
        };
    }

    @Override
    public Market withSelections(Map<String, Selection> newSelections) {
        return new FootballMoneyline(id, isSynthetic, newSelections, homeTeam, awayTeam);
    }
}
