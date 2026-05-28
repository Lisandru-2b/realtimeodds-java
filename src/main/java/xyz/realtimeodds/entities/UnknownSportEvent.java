package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record UnknownSportEvent(
        String id,
        String competition,
        Map<String, Market> markets,
        String sportRegion,
        OffsetDateTime startDate,
        String matchUrl) implements SportEvent {

    public UnknownSportEvent {
        markets = Map.copyOf(markets);
    }

    @JsonCreator
    public static UnknownSportEvent of(
            @JsonProperty("id") String id,
            @JsonProperty("competition") String competition,
            @JsonProperty("markets") List<Market> markets,
            @JsonProperty("sportRegion") String sportRegion,
            @JsonProperty("startDate") OffsetDateTime startDate,
            @JsonProperty("matchUrl") String matchUrl) {
        Map<String, Market> map = new LinkedHashMap<>();
        if (markets != null) {
            for (Market m : markets) {
                map.put(m.id(), m);
            }
        }
        return new UnknownSportEvent(id, competition, map, sportRegion, startDate, matchUrl);
    }

    @Override
    public SportEventKind kind() {
        return SportEventKind.UNKNOWN;
    }

    @Override
    public String name() {
        return id;
    }

    @Override
    public SportEvent withMarkets(Map<String, Market> newMarkets) {
        return new UnknownSportEvent(id, competition, newMarkets, sportRegion, startDate, matchUrl);
    }
}
