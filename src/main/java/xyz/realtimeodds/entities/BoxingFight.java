package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BoxingFight(
        String id,
        String competition,
        Map<String, Market> markets,
        String sportRegion,
        OffsetDateTime startDate,
        String matchUrl,
        String competitor1,
        String competitor2) implements SportEvent {

    public BoxingFight {
        markets = Map.copyOf(markets);
    }

    @JsonCreator
    public static BoxingFight of(
            @JsonProperty("id") String id,
            @JsonProperty("competition") String competition,
            @JsonProperty("markets") List<Market> markets,
            @JsonProperty("sportRegion") String sportRegion,
            @JsonProperty("startDate") OffsetDateTime startDate,
            @JsonProperty("matchUrl") String matchUrl,
            @JsonProperty("competitor1") String competitor1,
            @JsonProperty("competitor2") String competitor2) {
        Map<String, Market> map = new LinkedHashMap<>();
        if (markets != null) {
            for (Market m : markets) {
                map.put(m.id(), m);
            }
        }
        return new BoxingFight(id, competition, map, sportRegion, startDate, matchUrl, competitor1, competitor2);
    }

    @Override
    public SportEventKind kind() {
        return SportEventKind.BOXING_FIGHT;
    }

    @Override
    public String name() {
        return competitor1 + " / " + competitor2;
    }

    @Override
    public SportEvent withMarkets(Map<String, Market> newMarkets) {
        return new BoxingFight(id, competition, newMarkets, sportRegion, startDate, matchUrl, competitor1, competitor2);
    }
}
