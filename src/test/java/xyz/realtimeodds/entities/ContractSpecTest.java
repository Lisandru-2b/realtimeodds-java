package xyz.realtimeodds.entities;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContractSpecTest {
    private ObjectMapper mapper;
    private Map<String, Object> defs;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Map<String, Object> common = mapper.readValue(
                Path.of("..", "realtimeodds-spec", "schemas", "v1", "common.schema.json").toFile(),
                new TypeReference<>() {});
        defs = castMap(common.get("$defs"));
    }

    @Test
    void enumKindsMatchSpec() {
        assertThat(nonUnknownSportEventKindValues()).isEqualTo(enumValues("SportEventKind"));
        assertThat(nonUnknownMarketKindValues()).isEqualTo(enumValues("MarketKind"));
    }

    @Test
    void allSpecMarketKindsDeserializeWithoutUnknownFallback() throws Exception {
        for (String kind : enumValues("MarketKind")) {
            Market market = mapper.readValue(mapper.writeValueAsString(marketFixture(kind)), Market.class);
            assertThat(market.kind().value()).isEqualTo(kind);
            assertThat(market).isNotInstanceOf(UnknownMarket.class);
        }
    }

    @Test
    void allSpecSportEventKindsDeserializeWithoutUnknownFallback() throws Exception {
        for (String kind : enumValues("SportEventKind")) {
            SportEvent event = mapper.readValue(mapper.writeValueAsString(eventFixture(kind)), SportEvent.class);
            assertThat(event.kind().value()).isEqualTo(kind);
            assertThat(event).isNotInstanceOf(UnknownSportEvent.class);
        }
    }

    @Test
    void unknownMarketFallsBack() throws Exception {
        Market market = mapper.readValue(mapper.writeValueAsString(Map.of(
                "id", "vmid:ps3838:1:unknown",
                "kind", "market:unknown.kind",
                "selectionKind", "home/away",
                "isSynthetic", false,
                "selections", List.of())), Market.class);
        assertThat(market).isInstanceOf(UnknownMarket.class);
    }

    private List<String> enumValues(String name) {
        return castList(castMap(defs.get(name)).get("enum"));
    }

    private List<String> nonUnknownSportEventKindValues() {
        List<String> values = new ArrayList<>();
        for (SportEventKind kind : SportEventKind.values()) {
            if (kind != SportEventKind.UNKNOWN) values.add(kind.value());
        }
        return values;
    }

    private List<String> nonUnknownMarketKindValues() {
        List<String> values = new ArrayList<>();
        for (MarketKind kind : MarketKind.values()) {
            if (kind != MarketKind.UNKNOWN) values.add(kind.value());
        }
        return values;
    }

    private Map<String, Object> eventFixture(String eventKind) {
        String eventName = eventKind.substring("se:".length());
        String sport = eventName.replace("_match", "").replace("_fight", "");
        Map<String, Object> fixture = new LinkedHashMap<>();
        fixture.put("id", "vmid:ps3838:1");
        fixture.put("kind", eventKind);
        fixture.put("competition", "comp:" + sport + ".test");
        fixture.put("markets", List.of());
        if (eventName.equals("tennis_match") || eventName.equals("boxing_fight") || eventName.equals("mma_fight")) {
            fixture.put("competitor1", "A");
            fixture.put("competitor2", "B");
        } else {
            fixture.put("homeTeam", "Home");
            fixture.put("awayTeam", "Away");
        }
        return fixture;
    }

    private Map<String, Object> marketFixture(String marketKind) {
        String body = marketKind.substring("market:".length());
        String eventName = body.substring(0, body.indexOf('.'));
        String marketName = body.substring(body.indexOf('.') + 1);
        String selectionKind = selectionKind(eventName, marketName);
        Map<String, Object> fixture = new LinkedHashMap<>();
        fixture.put("id", "vmid:ps3838:1:" + marketName);
        fixture.put("kind", marketKind);
        fixture.put("selectionKind", selectionKind);
        fixture.put("isSynthetic", false);
        fixture.put("selections", selections(selectionKind, marketName));
        if (eventName.equals("tennis_match") || eventName.equals("boxing_fight") || eventName.equals("mma_fight")) {
            fixture.put("competitor1", "A");
            fixture.put("competitor2", "B");
        } else if (!marketName.equals("player_prop_over_under")) {
            fixture.put("homeTeam", "Home");
            fixture.put("awayTeam", "Away");
        }
        if ((marketName.equals("moneyline") || marketName.equals("handicap") || marketName.equals("total"))
                && !eventName.equals("boxing_fight") && !eventName.equals("cricket_match") && !eventName.equals("mma_fight")) {
            fixture.put("period", "full_match");
        }
        if (marketName.equals("handicap")) fixture.put("handicap", -1.5);
        if (marketName.equals("total")) {
            fixture.put("scope", "match");
            fixture.put("cut", 2.5);
        }
        if (eventName.equals("tennis_match") && (marketName.equals("handicap") || marketName.equals("total"))) {
            fixture.put("unit", "games");
        }
        if (marketName.equals("player_prop_over_under")) {
            fixture.put("playerName", "Player");
            fixture.put("propType", "points");
            fixture.put("cut", 20.5);
        }
        return fixture;
    }

    private String selectionKind(String eventName, String marketName) {
        if (marketName.equals("total") || marketName.equals("player_prop_over_under")) return "over/under";
        if (eventName.equals("tennis_match") || eventName.equals("boxing_fight") || eventName.equals("mma_fight")) {
            return "competitor1/competitor2";
        }
        if (marketName.equals("regulation_moneyline")) return "home/draw/away";
        if ((eventName.equals("football_match") || eventName.equals("handball_match") || eventName.equals("rugby_league_match"))
                && marketName.equals("moneyline")) {
            return "home/draw/away";
        }
        return "home/away";
    }

    private List<Map<String, Object>> selections(String selectionKind, String marketName) {
        List<String> results = switch (selectionKind) {
            case "over/under" -> List.of("over", "under");
            case "home/draw/away" -> List.of("home", "draw", "away");
            case "home/away" -> List.of("home", "away");
            case "competitor1/competitor2" -> List.of("competitor1", "competitor2");
            default -> throw new IllegalArgumentException(selectionKind);
        };
        List<Map<String, Object>> selections = new ArrayList<>();
        for (String result : results) {
            selections.add(Map.of(
                    "id", "vmid:ps3838:1:" + marketName + ":" + result,
                    "kind", selectionKind,
                    "result", result,
                    "quote", Map.of("price", 2.0, "timestamp", 1)));
        }
        return selections;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<String> castList(Object value) {
        return (List<String>) value;
    }
}
