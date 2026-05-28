package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import xyz.realtimeodds.IdHelper;

/**
 * A betting market within a {@link SportEvent}. Discriminated union over
 * {@link #kind()}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", defaultImpl = UnknownMarket.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AmericanFootballMoneyline.class, name = "market:american_football_match.moneyline"),
        @JsonSubTypes.Type(value = AmericanFootballHandicap.class, name = "market:american_football_match.handicap"),
        @JsonSubTypes.Type(value = AmericanFootballTotal.class, name = "market:american_football_match.total"),
        @JsonSubTypes.Type(value = BaseballMoneyline.class, name = "market:baseball_match.moneyline"),
        @JsonSubTypes.Type(value = BaseballHandicap.class, name = "market:baseball_match.handicap"),
        @JsonSubTypes.Type(value = BaseballTotal.class, name = "market:baseball_match.total"),
        @JsonSubTypes.Type(value = BasketballMoneyline.class, name = "market:basketball_match.moneyline"),
        @JsonSubTypes.Type(value = BasketballHandicap.class, name = "market:basketball_match.handicap"),
        @JsonSubTypes.Type(value = BasketballTotal.class, name = "market:basketball_match.total"),
        @JsonSubTypes.Type(value = BasketballPlayerPropOverUnder.class, name = "market:basketball_match.player_prop_over_under"),
        @JsonSubTypes.Type(value = BoxingMoneyline.class, name = "market:boxing_fight.moneyline"),
        @JsonSubTypes.Type(value = CricketMoneyline.class, name = "market:cricket_match.moneyline"),
        @JsonSubTypes.Type(value = FootballMoneyline.class, name = "market:football_match.moneyline"),
        @JsonSubTypes.Type(value = FootballHandicap.class, name = "market:football_match.handicap"),
        @JsonSubTypes.Type(value = FootballTotal.class, name = "market:football_match.total"),
        @JsonSubTypes.Type(value = HandballMoneyline.class, name = "market:handball_match.moneyline"),
        @JsonSubTypes.Type(value = HandballHandicap.class, name = "market:handball_match.handicap"),
        @JsonSubTypes.Type(value = HandballTotal.class, name = "market:handball_match.total"),
        @JsonSubTypes.Type(value = HockeyMoneyline.class, name = "market:hockey_match.moneyline"),
        @JsonSubTypes.Type(value = HockeyRegulationMoneyline.class, name = "market:hockey_match.regulation_moneyline"),
        @JsonSubTypes.Type(value = HockeyHandicap.class, name = "market:hockey_match.handicap"),
        @JsonSubTypes.Type(value = HockeyTotal.class, name = "market:hockey_match.total"),
        @JsonSubTypes.Type(value = MmaMoneyline.class, name = "market:mma_fight.moneyline"),
        @JsonSubTypes.Type(value = RugbyLeagueMoneyline.class, name = "market:rugby_league_match.moneyline"),
        @JsonSubTypes.Type(value = RugbyLeagueHandicap.class, name = "market:rugby_league_match.handicap"),
        @JsonSubTypes.Type(value = RugbyLeagueTotal.class, name = "market:rugby_league_match.total"),
        @JsonSubTypes.Type(value = TennisMoneyline.class, name = "market:tennis_match.moneyline"),
        @JsonSubTypes.Type(value = TennisHandicap.class, name = "market:tennis_match.handicap"),
        @JsonSubTypes.Type(value = TennisTotal.class, name = "market:tennis_match.total"),
})
public sealed interface Market permits
        AmericanFootballMoneyline,
        AmericanFootballHandicap,
        AmericanFootballTotal,
        BaseballMoneyline,
        BaseballHandicap,
        BaseballTotal,
        BasketballMoneyline,
        BasketballHandicap,
        BasketballTotal,
        BasketballPlayerPropOverUnder,
        BoxingMoneyline,
        CricketMoneyline,
        FootballMoneyline,
        FootballHandicap,
        FootballTotal,
        HandballMoneyline,
        HandballHandicap,
        HandballTotal,
        HockeyMoneyline,
        HockeyRegulationMoneyline,
        HockeyHandicap,
        HockeyTotal,
        MmaMoneyline,
        RugbyLeagueMoneyline,
        RugbyLeagueHandicap,
        RugbyLeagueTotal,
        TennisMoneyline,
        TennisHandicap,
        TennisTotal,
        UnknownMarket {

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
            case "american_football_match" -> Sport.AMERICAN_FOOTBALL;
            case "baseball_match" -> Sport.BASEBALL;
            case "basketball_match" -> Sport.BASKETBALL;
            case "boxing_fight" -> Sport.BOXING;
            case "cricket_match" -> Sport.CRICKET;
            case "football_match" -> Sport.FOOTBALL;
            case "handball_match" -> Sport.HANDBALL;
            case "hockey_match" -> Sport.HOCKEY;
            case "mma_fight" -> Sport.MMA;
            case "rugby_league_match" -> Sport.RUGBY_LEAGUE;
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
