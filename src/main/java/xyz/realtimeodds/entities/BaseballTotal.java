package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BaseballTotal(
        String id,
        boolean isSynthetic,
        Map<String, Selection> selections,
        String homeTeam,
        String awayTeam,
        String period,
        String scope,
        double cut) implements Market {

    public BaseballTotal { selections = Map.copyOf(selections); }

    @JsonCreator
    public static BaseballTotal of(
            @JsonProperty("id") String id,
            @JsonProperty("isSynthetic") boolean isSynthetic,
            @JsonProperty("selections") List<Selection> selections,
            @JsonProperty("homeTeam") String homeTeam,
            @JsonProperty("awayTeam") String awayTeam,
            @JsonProperty("period") String period,
            @JsonProperty("scope") String scope,
            @JsonProperty("cut") double cut) {
        Map<String, Selection> map = toMap(selections);
        return new BaseballTotal(id, isSynthetic, map, homeTeam, awayTeam, period == null ? "full_match" : period, scope, cut);
    }

    private static Map<String, Selection> toMap(List<Selection> selections) {
        Map<String, Selection> map = new LinkedHashMap<>();
        if (selections != null) for (Selection s : selections) map.put(s.id(), s);
        return map;
    }

    @Override public MarketKind kind() { return MarketKind.BASEBALL_TOTAL; }
    @Override public SelectionKind selectionKind() { return SelectionKind.OVER_UNDER; }
    @Override public String category() { return "Total"; }

    private String subject() {
        if ("home".equals(scope)) return homeTeam;
        if ("away".equals(scope)) return awayTeam;
        return "Total combine";
    }

    @Override
    public String getSelectionName(SelectionResult result) {
        if (result != SelectionResult.OVER && result != SelectionResult.UNDER) throw new IllegalArgumentException("Invalid selection result: " + result);
        String verb = result == SelectionResult.OVER ? "plus de" : "moins de";
        return subject() + " : " + verb + " " + cut + PeriodLabels.label(period);
    }

    @Override public Market withSelections(Map<String, Selection> newSelections) {
        return new BaseballTotal(id, isSynthetic, newSelections, homeTeam, awayTeam, period, scope, cut);
    }
}
