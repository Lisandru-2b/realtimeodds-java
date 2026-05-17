package xyz.realtimeodds;

import xyz.realtimeodds.entities.Market;
import xyz.realtimeodds.entities.Selection;
import xyz.realtimeodds.entities.SportEvent;

/**
 * The full hierarchical context of a selection — its parent market and the
 * parent sport event — returned in one O(1) lookup by
 * {@link OddsBook#findContext(String)}.
 *
 * <p>Useful inside an {@link Client#onOddsChanged} handler when you want more
 * than just the price.
 */
public record OddsContext(SportEvent sportEvent, Market market, Selection selection) {}
