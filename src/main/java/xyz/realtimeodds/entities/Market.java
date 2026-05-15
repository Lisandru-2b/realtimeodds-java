package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import xyz.realtimeodds.internal.IdHelper;

/**
 * A betting market within a {@link SportEvent}. Discriminated union over
 * {@link #kind()}. Six variants in v1.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BasketballMoneyline.class, name = "market:basketball_match.moneyline"),
        @JsonSubTypes.Type(value = BasketballHandicap.class, name = "market:basketball_match.handicap"),
        @JsonSubTypes.Type(value = BasketballTotal.class, name = "market:basketball_match.total"),
        @JsonSubTypes.Type(value = BasketballPlayerPropOverUnder.class, name = "market:basketball_match.player_prop_over_under"),
        @JsonSubTypes.Type(value = FootballMoneyline.class, name = "market:football_match.moneyline"),
        @JsonSubTypes.Type(value = TennisMoneyline.class, name = "market:tennis_match.moneyline"),
})
public sealed interface Market permits
        BasketballMoneyline,
        BasketballHandicap,
        BasketballTotal,
        BasketballPlayerPropOverUnder,
        FootballMoneyline,
        TennisMoneyline {

    String id();

    MarketKind kind();

    SelectionKind selectionKind();

    boolean isSynthetic();

    /** Read-only map of selections, keyed by SelectionId. */
    Map<String, Selection> selections();

    // ─── Computed properties ────────────────────────────────────────────────

    default Bookmaker bookmaker() {
        return IdHelper.getBookmaker(id());
    }

    /** Subclass-specific human-readable category (e.g. "Moneyline", "Total"). */
    String category();

    /** Subclass-specific human-readable selection name (French). */
    String getSelectionName(SelectionResult result);

    default String marketName() {
        // `market:<sport_event_name>.<market_name>` -> `<market_name>`
        String[] parts = kind().value().split(":")[1].split("\\.");
        return parts[1];
    }

    default String sportEventName() {
        return kind().value().split(":")[1].split("\\.")[0];
    }

    default Sport sport() {
        return switch (sportEventName()) {
            case "basketball_match" -> Sport.BASKETBALL;
            case "football_match" -> Sport.FOOTBALL;
            case "tennis_match" -> Sport.TENNIS;
            default -> throw new IllegalStateException("Unknown sport for " + kind());
        };
    }

    default boolean isAvailable() {
        return selections().values().stream().anyMatch(Selection::isAvailable);
    }

    default boolean isFullyAvailable() {
        if (!areAllSelectionsPresent()) {
            return false;
        }
        return selections().values().stream().allMatch(Selection::isAvailable);
    }

    default boolean areAllSelectionsPresent() {
        return selections().size() == numberOfPossibleResults();
    }

    default int numberOfPossibleResults() {
        return selectionKind().numberOfResults();
    }

    // ─── Lookups ────────────────────────────────────────────────────────────

    default Selection getSelection(String selectionId) {
        return selections().get(selectionId);
    }

    default Selection getSelectionByResult(SelectionResult result) {
        for (Selection s : selections().values()) {
            if (s.result() == result) {
                return s;
            }
        }
        return null;
    }

    default List<Selection> getSelectionsExceptForResult(SelectionResult result) {
        return selections().values().stream().filter(s -> s.result() != result).toList();
    }

    default SelectionResult getResult(String selectionId) {
        Selection s = getSelection(selectionId);
        return s == null ? null : s.result();
    }

    default boolean isSelectionAvailable(SelectionResult result) {
        Selection s = getSelectionByResult(result);
        return s != null && s.isAvailable();
    }

    // ─── Margin / fair odds ─────────────────────────────────────────────────

    /** Sum of implied probabilities minus 1. Throws if not fully available. */
    default double calculateMargin() {
        if (!isFullyAvailable()) {
            throw new IllegalStateException("Market " + id() + " is not fully available");
        }
        double margin = 0.0;
        for (Selection s : selections().values()) {
            margin += s.quote().impliedProbability();
        }
        return margin - 1.0;
    }

    default boolean isFairOddAvailable(SelectionResult result) {
        return isSynthetic() ? isSelectionAvailable(result) : isFullyAvailable();
    }

    /**
     * Margin-adjusted "true" odd for {@code result}. Synthetic markets bypass
     * the margin computation and return the raw price.
     */
    default double getFairOdd(SelectionResult result) {
        if (isSynthetic()) {
            Selection s = getSelectionByResult(result);
            if (s == null || !s.isAvailable()) {
                throw new IllegalStateException(
                        "Synthetic market " + id() + " cannot compute fair odd for " + result);
            }
            return s.price();
        }
        if (!isFullyAvailable()) {
            throw new IllegalStateException("Market " + id() + " is not fully available");
        }
        Selection s = getSelectionByResult(result);
        if (s == null) {
            throw new IllegalStateException("Selection " + result + " not found in " + id());
        }
        int n = selections().size();
        double margin = calculateMargin();
        return (n * s.price()) / (n - s.price() * margin);
    }

    // ─── Internal mutators (subclasses must implement) ──────────────────────

    Market withSelections(Map<String, Selection> newSelections);

    default Market withUpdatedSelection(Selection updated) {
        if (!selections().containsKey(updated.id())) {
            throw new IllegalStateException("Selection " + updated.id() + " not found");
        }
        Map<String, Selection> next = new LinkedHashMap<>(selections());
        next.put(updated.id(), updated);
        return withSelections(next);
    }

    default Market withUpdatedPrice(String selectionId, double price) {
        Selection s = getSelection(selectionId);
        if (s == null) {
            throw new IllegalStateException("Selection " + selectionId + " not found");
        }
        return withUpdatedSelection(s.withPrice(price));
    }

    default Market withUpdatedPrices(Map<String, Double> prices) {
        if (prices.isEmpty()) {
            return this;
        }
        Map<String, Selection> next = new LinkedHashMap<>(selections());
        for (var entry : prices.entrySet()) {
            Selection s = next.get(entry.getKey());
            if (s == null) {
                throw new IllegalStateException("Selection " + entry.getKey() + " not found");
            }
            next.put(entry.getKey(), s.withPrice(entry.getValue()));
        }
        return withSelections(next);
    }
}
