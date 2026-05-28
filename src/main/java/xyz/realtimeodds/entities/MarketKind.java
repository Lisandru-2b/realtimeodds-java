package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Discriminator for the Market union. Format: {@code market:<sport_event_name>.<market_name>}.
 */
public enum MarketKind {
    AMERICAN_FOOTBALL_MONEYLINE("market:american_football_match.moneyline"),
    AMERICAN_FOOTBALL_HANDICAP("market:american_football_match.handicap"),
    AMERICAN_FOOTBALL_TOTAL("market:american_football_match.total"),
    BASEBALL_MONEYLINE("market:baseball_match.moneyline"),
    BASEBALL_HANDICAP("market:baseball_match.handicap"),
    BASEBALL_TOTAL("market:baseball_match.total"),
    BASKETBALL_MONEYLINE("market:basketball_match.moneyline"),
    BASKETBALL_HANDICAP("market:basketball_match.handicap"),
    BASKETBALL_TOTAL("market:basketball_match.total"),
    BASKETBALL_PLAYER_PROP_OVER_UNDER("market:basketball_match.player_prop_over_under"),
    BOXING_MONEYLINE("market:boxing_fight.moneyline"),
    CRICKET_MONEYLINE("market:cricket_match.moneyline"),
    FOOTBALL_MONEYLINE("market:football_match.moneyline"),
    FOOTBALL_HANDICAP("market:football_match.handicap"),
    FOOTBALL_TOTAL("market:football_match.total"),
    HANDBALL_MONEYLINE("market:handball_match.moneyline"),
    HANDBALL_HANDICAP("market:handball_match.handicap"),
    HANDBALL_TOTAL("market:handball_match.total"),
    HOCKEY_MONEYLINE("market:hockey_match.moneyline"),
    HOCKEY_REGULATION_MONEYLINE("market:hockey_match.regulation_moneyline"),
    HOCKEY_HANDICAP("market:hockey_match.handicap"),
    HOCKEY_TOTAL("market:hockey_match.total"),
    MMA_MONEYLINE("market:mma_fight.moneyline"),
    RUGBY_LEAGUE_MONEYLINE("market:rugby_league_match.moneyline"),
    RUGBY_LEAGUE_HANDICAP("market:rugby_league_match.handicap"),
    RUGBY_LEAGUE_TOTAL("market:rugby_league_match.total"),
    TENNIS_MONEYLINE("market:tennis_match.moneyline"),
    TENNIS_HANDICAP("market:tennis_match.handicap"),
    TENNIS_TOTAL("market:tennis_match.total"),
    UNKNOWN("unknown");

    private final String value;

    MarketKind(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static MarketKind fromValue(String value) {
        for (MarketKind k : values()) {
            if (k.value.equals(value)) {
                return k;
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return value;
    }
}
