package xyz.realtimeodds;

import java.util.Map;
import xyz.realtimeodds.entities.SportEvent;

/**
 * Read-only snapshot of every known sport event, keyed by id.
 *
 * <p>{@code stale = true} when the connection is not currently established
 * (data may be outdated).
 */
public record Snapshot(Map<String, SportEvent> sportEvents, boolean stale) {

    public Snapshot {
        sportEvents = Map.copyOf(sportEvents);
    }
}
