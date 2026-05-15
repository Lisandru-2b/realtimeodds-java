package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Discriminator for the Market union. Format: {@code market:<sport_event_name>.<market_name>}.
 */
public enum MarketKind {
    BASKETBALL_MONEYLINE("market:basketball_match.moneyline"),
    BASKETBALL_HANDICAP("market:basketball_match.handicap"),
    BASKETBALL_TOTAL("market:basketball_match.total"),
    BASKETBALL_PLAYER_PROP_OVER_UNDER("market:basketball_match.player_prop_over_under"),
    FOOTBALL_MONEYLINE("market:football_match.moneyline"),
    TENNIS_MONEYLINE("market:tennis_match.moneyline");

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
        throw new IllegalArgumentException("Unknown MarketKind: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
