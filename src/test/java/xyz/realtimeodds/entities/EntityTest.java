package xyz.realtimeodds.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntityTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // ─── Quote ─────────────────────────────────────────────────────────────

    @Test
    void quoteImpliedProbability() {
        Quote q = new Quote(2.0, null, 0L);
        assertThat(q.impliedProbability()).isEqualTo(0.5);

        Quote q2 = new Quote(4.0, null, 0L);
        assertThat(q2.impliedProbability()).isEqualTo(0.25);
    }

    @Test
    void quoteRejectsInvalidPrice() {
        assertThatThrownBy(() -> new Quote(1.0, null, 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Quote(0.5, null, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── OrderBook ─────────────────────────────────────────────────────────

    @Test
    void orderBookBestBidAskSpreadMid() {
        OrderBook ob = new OrderBook(
                java.util.List.of(new Level(1.72, 8400), new Level(1.71, 12000)),
                java.util.List.of(new Level(1.74, 5600), new Level(1.76, 9000)),
                0L);
        assertThat(ob.bestBid()).isEqualTo(new Level(1.72, 8400));
        assertThat(ob.bestAsk()).isEqualTo(new Level(1.74, 5600));
        assertThat(ob.spread()).isEqualTo(0.02, within(1e-9));
        assertThat(ob.midPrice()).isEqualTo(1.73, within(1e-9));
    }

    @Test
    void orderBookAvailableSizeUpTo() {
        OrderBook ob = new OrderBook(
                java.util.List.of(),
                java.util.List.of(new Level(1.74, 5600), new Level(1.76, 9000), new Level(1.80, 12000)),
                0L);
        assertThat(ob.availableSizeUpTo(1.74)).isEqualTo(5600);
        assertThat(ob.availableSizeUpTo(1.78)).isEqualTo(5600 + 9000);
        assertThat(ob.availableSizeUpTo(1.80)).isEqualTo(5600 + 9000 + 12000);
    }

    @Test
    void orderBookEmpty() {
        OrderBook ob = new OrderBook(java.util.List.of(), java.util.List.of(), 0L);
        assertThat(ob.bestBid()).isNull();
        assertThat(ob.bestAsk()).isNull();
        assertThat(ob.spread()).isNull();
        assertThat(ob.midPrice()).isNull();
    }

    // ─── Selection ─────────────────────────────────────────────────────────

    @Test
    void selectionRejectsResultIncompatibleWithKind() {
        assertThatThrownBy(() ->
                new Selection("vmid:ps3838:1:m:bad", SelectionKind.HOME_AWAY, SelectionResult.OVER, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void selectionUnavailableThrowsOnPrice() {
        Selection s = new Selection(
                "vmid:ps3838:1:m:home", SelectionKind.HOME_AWAY, SelectionResult.HOME, null, null);
        assertThat(s.isAvailable()).isFalse();
        assertThatThrownBy(s::price).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void selectionBookmakerDerivedFromId() {
        Selection s = new Selection(
                "vmid:polymarket:0xabc:m:c1",
                SelectionKind.COMPETITOR1_COMPETITOR2,
                SelectionResult.COMPETITOR1,
                null, null);
        assertThat(s.bookmaker()).isEqualTo(Bookmaker.POLYMARKET);
    }

    // ─── Market: margin / fair odd ─────────────────────────────────────────

    @Test
    void basketballMoneylineMarginAndFairOdd() {
        Selection home = new Selection(
                "vmid:ps3838:1:m:home", SelectionKind.HOME_AWAY, SelectionResult.HOME,
                new Quote(1.91, null, 0L), null);
        Selection away = new Selection(
                "vmid:ps3838:1:m:away", SelectionKind.HOME_AWAY, SelectionResult.AWAY,
                new Quote(1.95, null, 0L), null);
        BasketballMoneyline market = new BasketballMoneyline(
                "vmid:ps3838:1:m", false,
                Map.of(home.id(), home, away.id(), away),
                "Lakers", "Celtics", BasketballPeriod.FULL_MATCH);

        assertThat(market.isFullyAvailable()).isTrue();
        assertThat(market.calculateMargin()).isPositive();
        // Fair odd > raw price (margin is removed)
        assertThat(market.getFairOdd(SelectionResult.HOME)).isGreaterThan(home.price());
        assertThat(market.getFairOdd(SelectionResult.AWAY)).isGreaterThan(away.price());
    }

    @Test
    void basketballHandicapSelectionName() {
        Selection home = new Selection(
                "vmid:ps3838:1:hcap:home", SelectionKind.HOME_AWAY, SelectionResult.HOME,
                new Quote(1.91, null, 0L), null);
        Selection away = new Selection(
                "vmid:ps3838:1:hcap:away", SelectionKind.HOME_AWAY, SelectionResult.AWAY,
                new Quote(1.91, null, 0L), null);
        BasketballHandicap market = new BasketballHandicap(
                "vmid:ps3838:1:hcap", false,
                Map.of(home.id(), home, away.id(), away),
                "Lakers", "Celtics", BasketballPeriod.FULL_MATCH, -3.5);

        assertThat(market.getSelectionName(SelectionResult.HOME)).contains("Lakers", "-3.5");
        assertThat(market.getSelectionName(SelectionResult.AWAY)).contains("Celtics", "+3.5");
    }

    @Test
    void footballMoneylineSelectionName() {
        FootballMoneyline market = new FootballMoneyline(
                "vmid:winamax:1:1x2", false, Map.of(), "Manchester City", "Arsenal");
        assertThat(market.getSelectionName(SelectionResult.HOME)).contains("Manchester City");
        assertThat(market.getSelectionName(SelectionResult.DRAW)).isEqualTo("Match nul");
    }

    // ─── SportEvent ─────────────────────────────────────────────────────────

    @Test
    void basketballMatchLookupAndNarrowing() {
        BasketballMatch ev = new BasketballMatch(
                "vmid:ps3838:1", "comp:basketball.nba", Map.of(),
                null, null, null, "Lakers", "Celtics");
        assertThat(ev.sport()).isEqualTo(Sport.BASKETBALL);
        assertThat(ev.bookmaker()).isEqualTo(Bookmaker.PS3838);
        assertThat(ev.name()).isEqualTo("Lakers / Celtics");
        assertThat(ev.getMarket("vmid:ps3838:1:nope")).isNull();
    }

    // ─── JSON deserialization ──────────────────────────────────────────────

    private static final String BASKETBALL_JSON = """
            {
              "id": "vmid:ps3838:1610547234",
              "kind": "se:basketball_match",
              "competition": "comp:basketball.nba",
              "sportRegion": "USA",
              "startDate": "2026-05-05T00:00:00Z",
              "homeTeam": "Lakers",
              "awayTeam": "Celtics",
              "markets": [
                {
                  "id": "vmid:ps3838:1610547234:ml",
                  "kind": "market:basketball_match.moneyline",
                  "selectionKind": "home/away",
                  "isSynthetic": false,
                  "homeTeam": "Lakers",
                  "awayTeam": "Celtics",
                  "period": "full_match",
                  "selections": [
                    {
                      "id": "vmid:ps3838:1610547234:ml:home",
                      "kind": "home/away",
                      "result": "home",
                      "quote": {"price": 1.91, "timestamp": 1714823400000, "size": 1500}
                    },
                    {
                      "id": "vmid:ps3838:1610547234:ml:away",
                      "kind": "home/away",
                      "result": "away",
                      "quote": {"price": 1.95, "timestamp": 1714823400000}
                    }
                  ]
                }
              ]
            }
            """;

    @Test
    void sportEventFromJsonBasketball() throws Exception {
        SportEvent ev = mapper.readValue(BASKETBALL_JSON, SportEvent.class);
        assertThat(ev).isInstanceOf(BasketballMatch.class);
        assertThat(ev.bookmaker()).isEqualTo(Bookmaker.PS3838);
        assertThat(ev.startDate()).isNotNull();
        assertThat(ev.startDate().getYear()).isEqualTo(2026);

        Market market = ev.markets().values().iterator().next();
        assertThat(market).isInstanceOf(BasketballMoneyline.class);
        assertThat(market.isFullyAvailable()).isTrue();
    }
}
