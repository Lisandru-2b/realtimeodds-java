package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TennisTotal(
        String id,
        boolean isSynthetic,
        Map<String, Selection> selections,
        String competitor1,
        String competitor2,
        String period,
        String scope,
        String unit,
        double cut) implements Market {

    public TennisTotal { selections = Map.copyOf(selections); }

    @JsonCreator
    public static TennisTotal of(
            @JsonProperty("id") String id,
            @JsonProperty("isSynthetic") boolean isSynthetic,
            @JsonProperty("selections") List<Selection> selections,
            @JsonProperty("competitor1") String competitor1,
            @JsonProperty("competitor2") String competitor2,
            @JsonProperty("period") String period,
            @JsonProperty("scope") String scope,
            @JsonProperty("unit") String unit,
            @JsonProperty("cut") double cut) {
        Map<String, Selection> map = new LinkedHashMap<>();
        if (selections != null) for (Selection s : selections) map.put(s.id(), s);
        return new TennisTotal(id, isSynthetic, map, competitor1, competitor2, period == null ? "full_match" : period, scope, unit, cut);
    }

    @Override public MarketKind kind() { return MarketKind.TENNIS_TOTAL; }
    @Override public SelectionKind selectionKind() { return SelectionKind.OVER_UNDER; }
    @Override public String category() { return "Total"; }

    private String subject() {
        if ("competitor1".equals(scope)) return competitor1;
        if ("competitor2".equals(scope)) return competitor2;
        return "Total combine";
    }

    @Override
    public String getSelectionName(SelectionResult result) {
        if (result != SelectionResult.OVER && result != SelectionResult.UNDER) throw new IllegalArgumentException("Invalid selection result: " + result);
        String verb = result == SelectionResult.OVER ? "plus de" : "moins de";
        return subject() + " : " + verb + " " + cut + " " + unit + PeriodLabels.label(period);
    }

    @Override public Market withSelections(Map<String, Selection> newSelections) {
        return new TennisTotal(id, isSynthetic, newSelections, competitor1, competitor2, period, scope, unit, cut);
    }
}
