package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record UnknownMarket(
        String id,
        boolean isSynthetic,
        Map<String, Selection> selections,
        SelectionKind selectionKind) implements Market {

    public UnknownMarket {
        selections = Map.copyOf(selections);
    }

    @JsonCreator
    public static UnknownMarket of(
            @JsonProperty("id") String id,
            @JsonProperty("isSynthetic") boolean isSynthetic,
            @JsonProperty("selections") List<Selection> selections,
            @JsonProperty("selectionKind") SelectionKind selectionKind) {
        Map<String, Selection> map = new LinkedHashMap<>();
        if (selections != null) for (Selection s : selections) map.put(s.id(), s);
        return new UnknownMarket(id, isSynthetic, map, selectionKind == null ? SelectionKind.HOME_AWAY : selectionKind);
    }

    @Override public MarketKind kind() { return MarketKind.UNKNOWN; }
    @Override public String category() { return "unknown"; }
    @Override public String getSelectionName(SelectionResult result) { return result.value(); }
    @Override public Market withSelections(Map<String, Selection> newSelections) {
        return new UnknownMarket(id, isSynthetic, newSelections, selectionKind);
    }
}
