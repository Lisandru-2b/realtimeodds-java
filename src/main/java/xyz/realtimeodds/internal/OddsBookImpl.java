package xyz.realtimeodds.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import xyz.realtimeodds.OddsBook;
import xyz.realtimeodds.OddsContext;
import xyz.realtimeodds.entities.Bookmaker;
import xyz.realtimeodds.entities.Market;
import xyz.realtimeodds.entities.Selection;
import xyz.realtimeodds.entities.SportEvent;

/**
 * Mutable implementation of {@link OddsBook}. Not part of the public surface
 * — only the read-only interface escapes.
 *
 * <p>Indexes (market → parent event id, selection → parent market id,
 * bookmaker → set of event ids) are maintained eagerly on every mutation:
 * a market lookup never scans the events, a selection lookup never scans
 * the markets.
 *
 * <p>The book is shallow: it holds references to entity instances coming from
 * the gateway client's internal store. Those entities are immutable (each
 * upsert produces a new instance) so a shallow clone is enough to freeze a
 * snapshot.
 */
public final class OddsBookImpl implements OddsBook {

    private final Map<String, SportEvent> events = new LinkedHashMap<>();
    /** marketId → parent sportEventId */
    private final Map<String, String> marketParent = new HashMap<>();
    /** selectionId → parent marketId */
    private final Map<String, String> selectionParent = new HashMap<>();
    /** bookmaker → set of sportEventIds belonging to that bookmaker */
    private final Map<Bookmaker, Set<String>> byBookmaker = new HashMap<>();

    @Override
    public int size() {
        return events.size();
    }

    @Override
    public SportEvent getSportEvent(String sportEventId) {
        return events.get(sportEventId);
    }

    @Override
    public Market getMarket(String marketId) {
        String sportEventId = marketParent.get(marketId);
        if (sportEventId == null) {
            return null;
        }
        SportEvent ev = events.get(sportEventId);
        return ev == null ? null : ev.markets().get(marketId);
    }

    @Override
    public Selection getSelection(String selectionId) {
        String marketId = selectionParent.get(selectionId);
        if (marketId == null) {
            return null;
        }
        Market market = getMarket(marketId);
        return market == null ? null : market.getSelection(selectionId);
    }

    @Override
    public OddsContext findContext(String selectionId) {
        String marketId = selectionParent.get(selectionId);
        if (marketId == null) {
            return null;
        }
        String sportEventId = marketParent.get(marketId);
        if (sportEventId == null) {
            return null;
        }
        SportEvent sportEvent = events.get(sportEventId);
        if (sportEvent == null) {
            return null;
        }
        Market market = sportEvent.markets().get(marketId);
        if (market == null) {
            return null;
        }
        Selection selection = market.getSelection(selectionId);
        if (selection == null) {
            return null;
        }
        return new OddsContext(sportEvent, market, selection);
    }

    @Override
    public List<SportEvent> sportEvents() {
        return List.copyOf(events.values());
    }

    @Override
    public Iterator<SportEvent> iterator() {
        return Collections.unmodifiableCollection(events.values()).iterator();
    }

    // ─── Mutation surface (not part of OddsBook) ────────────────────────────

    /**
     * Insert or replace a sport event, re-indexing its markets and selections.
     * If a previous version was already indexed, its stale indexes are
     * dropped first.
     */
    public void upsert(SportEvent sportEvent) {
        String id = sportEvent.id();
        SportEvent previous = events.get(id);
        if (previous != null) {
            dropIndexes(previous);
        }
        events.put(id, sportEvent);
        addIndexes(sportEvent);
    }

    /** Remove a sport event by id, dropping all its indexes. */
    public void remove(String sportEventId) {
        SportEvent previous = events.remove(sportEventId);
        if (previous == null) {
            return;
        }
        dropIndexes(previous);
    }

    /**
     * Drop every sport event belonging to {@code bookmaker}. Returns the
     * count of removed events — callers can use it to decide whether to
     * emit a {@code source:cleared} public event.
     */
    public int clearBookmaker(Bookmaker bookmaker) {
        Set<String> ids = byBookmaker.get(bookmaker);
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        // Materialize before iterating: dropIndexes() mutates the same set,
        // which would otherwise corrupt the loop and zero out the count.
        List<String> idsToDrop = new ArrayList<>(ids);
        for (String id : idsToDrop) {
            SportEvent ev = events.remove(id);
            if (ev != null) {
                dropIndexes(ev);
            }
        }
        // Safety net: dropIndexes deletes the bookmaker key when the set
        // hits 0, but make sure it's gone even on partial cleanup.
        byBookmaker.remove(bookmaker);
        return idsToDrop.size();
    }

    /**
     * Atomically replace every sport event of {@code bookmaker} with the
     * new ground truth. Used by {@code resync} handling — the previous view
     * for that bookmaker is discarded before the new one is inserted.
     */
    public void replaceBookmaker(Bookmaker bookmaker, List<SportEvent> nextEvents) {
        clearBookmaker(bookmaker);
        for (SportEvent ev : nextEvents) {
            upsert(ev);
        }
    }

    /** Drop every sport event, from every bookmaker. */
    public void clear() {
        events.clear();
        marketParent.clear();
        selectionParent.clear();
        byBookmaker.clear();
    }

    /** Shallow clone — independent maps sharing the same entity references. */
    public OddsBookImpl cloneBook() {
        OddsBookImpl c = new OddsBookImpl();
        c.events.putAll(this.events);
        c.marketParent.putAll(this.marketParent);
        c.selectionParent.putAll(this.selectionParent);
        for (Map.Entry<Bookmaker, Set<String>> e : this.byBookmaker.entrySet()) {
            c.byBookmaker.put(e.getKey(), new HashSet<>(e.getValue()));
        }
        return c;
    }

    // ─── Private indexing helpers ───────────────────────────────────────────

    private void addIndexes(SportEvent sportEvent) {
        String id = sportEvent.id();
        Bookmaker bookmaker = sportEvent.bookmaker();
        byBookmaker.computeIfAbsent(bookmaker, k -> new HashSet<>()).add(id);
        for (Map.Entry<String, Market> entry : sportEvent.markets().entrySet()) {
            String marketId = entry.getKey();
            marketParent.put(marketId, id);
            for (String selectionId : entry.getValue().selections().keySet()) {
                selectionParent.put(selectionId, marketId);
            }
        }
    }

    private void dropIndexes(SportEvent sportEvent) {
        String id = sportEvent.id();
        Bookmaker bookmaker = sportEvent.bookmaker();
        Set<String> ids = byBookmaker.get(bookmaker);
        if (ids != null) {
            ids.remove(id);
            if (ids.isEmpty()) {
                byBookmaker.remove(bookmaker);
            }
        }
        for (Map.Entry<String, Market> entry : sportEvent.markets().entrySet()) {
            String marketId = entry.getKey();
            marketParent.remove(marketId);
            for (String selectionId : entry.getValue().selections().keySet()) {
                selectionParent.remove(selectionId);
            }
        }
    }
}
