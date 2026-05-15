package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record FootballMatch(
        String id,
        String competition,
        Map<String, Market> markets,
        String sportRegion,
        OffsetDateTime startDate,
        String matchUrl,
        String homeTeam,
        String awayTeam) implements SportEvent {

    public FootballMatch {
        markets = Map.copyOf(markets);
    }

    @JsonCreator
    public static FootballMatch of(
            @JsonProperty("id") String id,
            @JsonProperty("competition") String competition,
            @JsonProperty("markets") List<Market> markets,
            @JsonProperty("sportRegion") String sportRegion,
            @JsonProperty("startDate") OffsetDateTime startDate,
            @JsonProperty("matchUrl") String matchUrl,
            @JsonProperty("homeTeam") String homeTeam,
            @JsonProperty("awayTeam") String awayTeam) {
        Map<String, Market> map = new LinkedHashMap<>();
        if (markets != null) {
            for (Market m : markets) {
                map.put(m.id(), m);
            }
        }
        return new FootballMatch(id, competition, map, sportRegion, startDate, matchUrl, homeTeam, awayTeam);
    }

    @Override
    public SportEventKind kind() {
        return SportEventKind.FOOTBALL_MATCH;
    }

    @Override
    public String name() {
        return homeTeam + " / " + awayTeam;
    }

    @Override
    public SportEvent withMarkets(Map<String, Market> newMarkets) {
        return new FootballMatch(id, competition, newMarkets, sportRegion, startDate, matchUrl, homeTeam, awayTeam);
    }
}
