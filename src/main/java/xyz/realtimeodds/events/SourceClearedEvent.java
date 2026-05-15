package xyz.realtimeodds.events;

import xyz.realtimeodds.entities.Bookmaker;

/**
 * Fired when an entire bookmaker source becomes unavailable. The SDK has
 * already dropped every sport event belonging to that bookmaker before this
 * event fires.
 */
public record SourceClearedEvent(Bookmaker bookmaker, long receivedAt) {}
