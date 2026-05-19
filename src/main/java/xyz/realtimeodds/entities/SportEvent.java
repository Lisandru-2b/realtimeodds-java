package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import xyz.realtimeodds.IdHelper;

/**
 * A sport event (match) reported by a bookmaker. Discriminated union over
 * {@link #kind()}. Three variants in v1.
 *
 * <p>{@code bookmaker} and {@code sport} are NOT wire fields — they are
 * computed properties derived from {@code id} and {@code kind}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BasketballMatch.class, name = "se:basketball_match"),
        @JsonSubTypes.Type(value = FootballMatch.class, name = "se:football_match"),
        @JsonSubTypes.Type(value = TennisMatch.class, name = "se:tennis_match"),
})
public sealed interface SportEvent permits BasketballMatch, FootballMatch, TennisMatch {

    String id();

    SportEventKind kind();

    String competition();

    /** Read-only map of markets, keyed by MarketId. */
    Map<String, Market> markets();

    String sportRegion();

    OffsetDateTime startDate();

    String matchUrl();

    /** Human-readable match name (subclass-specific). */
    String name();

    // ─── Computed properties ────────────────────────────────────────────────

    default Bookmaker bookmaker() {
        return IdHelper.getBookmaker(id());
    }

    default String sportEventName() {
        return kind().value().split(":")[1];
    }

    default Sport sport() {
        return kind().sport();
    }

    // ─── Lookups ────────────────────────────────────────────────────────────

    /**
     * Lookup by MarketId or SelectionId. Returns {@code null} if unknown.
     *
     * <p>Tries a direct lookup first (input is a MarketId), then falls back
     * to stripping the trailing selection segment. Robust to MarketIds whose
     * {@code external_market_id} part contains internal {@code :} separators.
     */
    default Market getMarket(String entityId) {
        // Direct: the caller passed a MarketId.
        Market direct = markets().get(entityId);
        if (direct != null) return direct;
        // Fall back: the caller passed a SelectionId — drop the last segment.
        int lastColon = entityId.lastIndexOf(':');
        if (lastColon == -1) return null;
        return markets().get(entityId.substring(0, lastColon));
    }

    default Selection getSelection(String selectionId) {
        Market market = getMarket(selectionId);
        return market == null ? null : market.getSelection(selectionId);
    }

    // ─── Internal mutators ──────────────────────────────────────────────────

    /** Subclass must return its own concrete type. */
    SportEvent withMarkets(Map<String, Market> newMarkets);

    default SportEvent withUpdatedMarket(Market updated) {
        if (!markets().containsKey(updated.id())) {
            throw new IllegalStateException("Market " + updated.id() + " not found in " + id());
        }
        Map<String, Market> next = new LinkedHashMap<>(markets());
        next.put(updated.id(), updated);
        return withMarkets(next);
    }

    default SportEvent withUpdatedPrices(Map<String, Double> prices) {
        if (prices.isEmpty()) {
            return this;
        }
        String firstSelId = prices.keySet().iterator().next();
        String marketId = IdHelper.getMarketId(firstSelId);
        Market market = markets().get(marketId);
        if (market == null) {
            throw new IllegalStateException("Market " + marketId + " not found in " + id());
        }
        for (String selId : prices.keySet()) {
            if (!IdHelper.getMarketId(selId).equals(market.id())) {
                throw new IllegalStateException("Selections are not all from the same market");
            }
        }
        return withUpdatedMarket(market.withUpdatedPrices(prices));
    }
}
