package xyz.realtimeodds.events;

import xyz.realtimeodds.entities.Bookmaker;

/**
 * Fired when a sport event is no longer reported by its source (match ended,
 * withdrawn, etc.). Carries {@code bookmaker} because the entity is gone —
 * consumers can no longer derive it from {@code id} alone if they want to filter.
 */
public record SportEventRemovedEvent(Bookmaker bookmaker, String sportEventId, long receivedAt) {}
