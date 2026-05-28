package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Discriminator for the SportEvent union. Format: {@code se:<sport_event_name>}.
 */
public enum SportEventKind {
    AMERICAN_FOOTBALL_MATCH("se:american_football_match"),
    BASEBALL_MATCH("se:baseball_match"),
    BASKETBALL_MATCH("se:basketball_match"),
    BOXING_FIGHT("se:boxing_fight"),
    CRICKET_MATCH("se:cricket_match"),
    FOOTBALL_MATCH("se:football_match"),
    HANDBALL_MATCH("se:handball_match"),
    HOCKEY_MATCH("se:hockey_match"),
    MMA_FIGHT("se:mma_fight"),
    RUGBY_LEAGUE_MATCH("se:rugby_league_match"),
    TENNIS_MATCH("se:tennis_match"),
    UNKNOWN("unknown");

    private final String value;

    SportEventKind(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static SportEventKind fromValue(String value) {
        for (SportEventKind k : values()) {
            if (k.value.equals(value)) {
                return k;
            }
        }
        return UNKNOWN;
    }

    public Sport sport() {
        return switch (this) {
            case AMERICAN_FOOTBALL_MATCH -> Sport.AMERICAN_FOOTBALL;
            case BASEBALL_MATCH -> Sport.BASEBALL;
            case BASKETBALL_MATCH -> Sport.BASKETBALL;
            case BOXING_FIGHT -> Sport.BOXING;
            case CRICKET_MATCH -> Sport.CRICKET;
            case FOOTBALL_MATCH -> Sport.FOOTBALL;
            case HANDBALL_MATCH -> Sport.HANDBALL;
            case HOCKEY_MATCH -> Sport.HOCKEY;
            case MMA_FIGHT -> Sport.MMA;
            case RUGBY_LEAGUE_MATCH -> Sport.RUGBY_LEAGUE;
            case TENNIS_MATCH -> Sport.TENNIS;
            case UNKNOWN -> throw new IllegalStateException("Unknown sport event kind");
        };
    }

    @Override
    public String toString() {
        return value;
    }
}
