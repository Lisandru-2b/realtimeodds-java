package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Over/under market for a basketball player prop (points, rebounds, …). */
public record BasketballPlayerPropOverUnder(
        String id,
        boolean isSynthetic,
        Map<String, Selection> selections,
        String playerName,
        PlayerPropType propType,
        double cut) implements Market {

    public BasketballPlayerPropOverUnder {
        selections = Map.copyOf(selections);
    }

    @JsonCreator
    public static BasketballPlayerPropOverUnder of(
            @JsonProperty("id") String id,
            @JsonProperty("isSynthetic") boolean isSynthetic,
            @JsonProperty("selections") List<Selection> selections,
            @JsonProperty("playerName") String playerName,
            @JsonProperty("propType") PlayerPropType propType,
            @JsonProperty("cut") double cut) {
        Map<String, Selection> map = new LinkedHashMap<>();
        if (selections != null) {
            for (Selection s : selections) {
                map.put(s.id(), s);
            }
        }
        return new BasketballPlayerPropOverUnder(id, isSynthetic, map, playerName, propType, cut);
    }

    @Override
    public MarketKind kind() {
        return MarketKind.BASKETBALL_PLAYER_PROP_OVER_UNDER;
    }

    @Override
    public SelectionKind selectionKind() {
        return SelectionKind.OVER_UNDER;
    }

    @Override
    public String category() {
        return "NBA";
    }

    @Override
    public String getSelectionName(SelectionResult result) {
        if (result != SelectionResult.OVER && result != SelectionResult.UNDER) {
            throw new IllegalArgumentException("Invalid selection result: " + result);
        }
        String comparator = result == SelectionResult.OVER ? "plus de" : "moins de";
        return playerName + " " + propType.verb() + " " + comparator + " " + cut + " " + propType.label();
    }

    @Override
    public Market withSelections(Map<String, Selection> newSelections) {
        return new BasketballPlayerPropOverUnder(id, isSynthetic, newSelections, playerName, propType, cut);
    }
}
