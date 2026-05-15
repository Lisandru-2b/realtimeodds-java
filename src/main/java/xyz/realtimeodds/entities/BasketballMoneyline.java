package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Moneyline (winner) market for a basketball match. */
public record BasketballMoneyline(
        String id,
        boolean isSynthetic,
        Map<String, Selection> selections,
        String homeTeam,
        String awayTeam,
        BasketballPeriod period) implements Market {

    public BasketballMoneyline {
        selections = Map.copyOf(selections);
        if (period == null) {
            period = BasketballPeriod.FULL_MATCH;
        }
    }

    @JsonCreator
    public static BasketballMoneyline of(
            @JsonProperty("id") String id,
            @JsonProperty("isSynthetic") boolean isSynthetic,
            @JsonProperty("selections") List<Selection> selections,
            @JsonProperty("homeTeam") String homeTeam,
            @JsonProperty("awayTeam") String awayTeam,
            @JsonProperty("period") BasketballPeriod period) {
        Map<String, Selection> map = new LinkedHashMap<>();
        if (selections != null) {
            for (Selection s : selections) {
                map.put(s.id(), s);
            }
        }
        return new BasketballMoneyline(id, isSynthetic, map, homeTeam, awayTeam,
                period == null ? BasketballPeriod.FULL_MATCH : period);
    }

    @Override
    public MarketKind kind() {
        return MarketKind.BASKETBALL_MONEYLINE;
    }

    @Override
    public SelectionKind selectionKind() {
        return SelectionKind.HOME_AWAY;
    }

    @Override
    public String category() {
        return "Moneyline";
    }

    @Override
    public String getSelectionName(SelectionResult result) {
        return switch (result) {
            case HOME -> homeTeam + " vainqueur" + period.label();
            case AWAY -> awayTeam + " vainqueur" + period.label();
            default -> throw new IllegalArgumentException("Invalid selection result: " + result);
        };
    }

    @Override
    public Market withSelections(Map<String, Selection> newSelections) {
        return new BasketballMoneyline(id, isSynthetic, newSelections, homeTeam, awayTeam, period);
    }
}
