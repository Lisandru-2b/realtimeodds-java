package xyz.realtimeodds.events;

import xyz.realtimeodds.entities.SportEvent;

/** Fired the first time a sport event is observed (per source). */
public record SportEventAddedEvent(SportEvent sportEvent, long receivedAt) {}
