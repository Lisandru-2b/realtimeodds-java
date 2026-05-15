package xyz.realtimeodds.events;

import xyz.realtimeodds.entities.SportEvent;

/**
 * Fired when an existing sport event's metadata, market list, or any odds value
 * changes. The full updated entity is provided so consumers can replace their
 * local reference atomically.
 */
public record SportEventUpdatedEvent(SportEvent sportEvent, long receivedAt) {}
