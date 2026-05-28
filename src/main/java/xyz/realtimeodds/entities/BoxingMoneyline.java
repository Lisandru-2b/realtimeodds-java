package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BoxingMoneyline(
        String id,
        boolean isSynthetic,
        Map<String, Selection> selections,
        String competitor1,
        String competitor2) implements Market {

    public BoxingMoneyline { selections = Map.copyOf(selections); }

    @JsonCreator
    public static BoxingMoneyline of(
            @JsonProperty("id") String id,
            @JsonProperty("isSynthetic") boolean isSynthetic,
            @JsonProperty("selections") List<Selection> selections,
            @JsonProperty("competitor1") String competitor1,
            @JsonProperty("competitor2") String competitor2) {
        Map<String, Selection> map = toMap(selections);
        return new BoxingMoneyline(id, isSynthetic, map, competitor1, competitor2);
    }

    private static Map<String, Selection> toMap(List<Selection> selections) {
        Map<String, Selection> map = new LinkedHashMap<>();
        if (selections != null) for (Selection s : selections) map.put(s.id(), s);
        return map;
    }

    @Override public MarketKind kind() { return MarketKind.BOXING_MONEYLINE; }
    @Override public SelectionKind selectionKind() { return SelectionKind.COMPETITOR1_COMPETITOR2; }
    @Override public String category() { return "Moneyline"; }

    @Override
    public String getSelectionName(SelectionResult result) {
        return switch (result) {
            case COMPETITOR1 -> competitor1 + " vainqueur";
            case COMPETITOR2 -> competitor2 + " vainqueur";
            default -> throw new IllegalArgumentException("Invalid selection result: " + result);
        };
    }

    @Override public Market withSelections(Map<String, Selection> newSelections) {
        return new BoxingMoneyline(id, isSynthetic, newSelections, competitor1, competitor2);
    }
}
