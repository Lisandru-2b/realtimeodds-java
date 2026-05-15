package xyz.realtimeodds.events;

import java.util.List;
import xyz.realtimeodds.entities.Bookmaker;
import xyz.realtimeodds.entities.SportEvent;

/**
 * Signals that the source's full state has been replaced atomically
 * (typically because the gateway rotated the primary preset for that
 * bookmaker). Consumers should re-derive any state from {@code sportEvents};
 * no per-event added/updated/removed events bracket this signal.
 */
public record ResyncEvent(
        Bookmaker bookmaker,
        String reason,
        List<SportEvent> sportEvents,
        long receivedAt) {

    public ResyncEvent {
        sportEvents = List.copyOf(sportEvents);
    }
}
