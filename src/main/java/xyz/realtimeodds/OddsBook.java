package xyz.realtimeodds;

import java.util.Iterator;
import java.util.List;
import xyz.realtimeodds.entities.Market;
import xyz.realtimeodds.entities.Selection;
import xyz.realtimeodds.entities.SportEvent;

/**
 * Read-only view of every sport event the SDK currently knows about, across
 * every bookmaker, with constant-time lookups by id at every level.
 *
 * <p>Two access patterns:
 * <ul>
 *   <li>{@link Client#odds()} returns the <b>live</b> instance — it is mutated
 *       in place as wire messages arrive. Read it inside event handlers
 *       (e.g. {@link Client#onOddsChanged}) to access the freshest state.
 *       Beware: {@link #size()} and lookup results change between two reads
 *       if events are flowing.</li>
 *   <li>{@link Client#snapshot()} returns a <b>frozen clone</b> taken at the
 *       moment of the call. Use it when you need a stable view (audits,
 *       exports, multi-step computations that must not see torn state).</li>
 * </ul>
 *
 * <p>On disconnect the live book is emptied immediately and a
 * {@code disconnected} event is fired. On reconnect it repopulates from the
 * server snapshot and {@code sportEvent:added} fires for each event as usual.
 */
public interface OddsBook extends Iterable<SportEvent> {

    /** Number of sport events currently tracked, across all bookmakers. */
    int size();

    /** Look up a single sport event by id. Returns {@code null} if unknown. */
    SportEvent getSportEvent(String sportEventId);

    /**
     * Look up a market by its global id. Returns {@code null} if no sport
     * event holds a market with this id.
     */
    Market getMarket(String marketId);

    /**
     * Look up a selection by its global id. Returns {@code null} if no market
     * holds a selection with this id.
     */
    Selection getSelection(String selectionId);

    /**
     * Resolve a selection to its full hierarchical context — the parent
     * market and parent sport event — in a single lookup. Recommended inside
     * an {@link Client#onOddsChanged} handler when you need more than just
     * the price.
     *
     * <p>Returns {@code null} if the selection is unknown or its parents have
     * already been removed.
     */
    OddsContext findContext(String selectionId);

    /**
     * Materialize every sport event into an unmodifiable list. Order is
     * unspecified (insertion order in practice, but you should not depend on
     * it). Cheap snapshot of references — no entity is cloned.
     */
    List<SportEvent> sportEvents();

    /** Iterate every sport event. Same caveats as {@link #sportEvents()}. */
    @Override
    Iterator<SportEvent> iterator();
}
