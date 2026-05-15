package xyz.realtimeodds.events;

import xyz.realtimeodds.entities.Bookmaker;
import xyz.realtimeodds.entities.Quote;

/**
 * Fired when the price (or order book) of a specific selection changes.
 * Fires alongside a {@link SportEventUpdatedEvent} for the parent.
 */
public record OddsChangedEvent(
        Bookmaker bookmaker,
        String sportEventId,
        String marketId,
        String selectionId,
        Quote quote,
        long receivedAt) {}
