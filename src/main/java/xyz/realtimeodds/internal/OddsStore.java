package xyz.realtimeodds.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import xyz.realtimeodds.entities.Market;
import xyz.realtimeodds.entities.Selection;
import xyz.realtimeodds.entities.SportEvent;
import xyz.realtimeodds.entities.SportEventKind;

/**
 * In-memory replica of {@code sb-odds-store/OddsStore}. Holds the latest known
 * SportEvent per id. Mutations create new SportEvent instances via the
 * internal {@code with*} chain, so consumers tracking by reference can swap
 * their pointer atomically.
 *
 * <p>Emits events:
 * <ul>
 *   <li>{@code sportEvent:upserted} — payload {@link UpsertedPayload}</li>
 *   <li>{@code sportEvent:removed} — payload {@link RemovedPayload}</li>
 *   <li>{@code prices:updated} — payload {@link PricesUpdatedPayload}</li>
 *   <li>{@code store:cleared} — no payload</li>
 * </ul>
 */
public final class OddsStore {

    public record UpsertedPayload(SportEvent sportEvent, boolean isNew) {}

    public record RemovedPayload(String sportEventId) {}

    public record PricesUpdatedPayload(String sportEventId, Map<String, Double> prices) {}

    public record ResyncedPayload(String reason, java.util.List<SportEvent> sportEvents) {}

    private final TypedEmitter emitter = new TypedEmitter();
    private final Map<String, SportEvent> sportEvents = new LinkedHashMap<>();
    private final Map<SportEventKind, Map<String, SportEvent>> byKind = new HashMap<>();

    public TypedEmitter events() {
        return emitter;
    }

    // ─── Mutations ──────────────────────────────────────────────────────────

    public void upsertSportEvent(SportEvent sportEvent) {
        boolean isNew = !sportEvents.containsKey(sportEvent.id());
        sportEvents.put(sportEvent.id(), sportEvent);
        byKind.computeIfAbsent(sportEvent.kind(), k -> new LinkedHashMap<>()).put(sportEvent.id(), sportEvent);
        emitter.emit("sportEvent:upserted", new UpsertedPayload(sportEvent, isNew));
    }

    public boolean updatePrices(String sportEventId, Map<String, Double> prices) {
        SportEvent existing = sportEvents.get(sportEventId);
        if (existing == null) {
            return false;
        }
        SportEvent updated;
        try {
            updated = existing.withUpdatedPrices(prices);
        } catch (RuntimeException e) {
            return false;
        }
        sportEvents.put(sportEventId, updated);
        byKind.computeIfAbsent(updated.kind(), k -> new LinkedHashMap<>()).put(updated.id(), updated);
        emitter.emit("prices:updated", new PricesUpdatedPayload(sportEventId, prices));
        return true;
    }

    public boolean removeSportEvent(String sportEventId) {
        SportEvent existing = sportEvents.remove(sportEventId);
        if (existing == null) {
            return false;
        }
        Map<String, SportEvent> kindMap = byKind.get(existing.kind());
        if (kindMap != null) {
            kindMap.remove(sportEventId);
        }
        emitter.emit("sportEvent:removed", new RemovedPayload(sportEventId));
        return true;
    }

    public void clear() {
        sportEvents.clear();
        byKind.clear();
        emitter.emit("store:cleared", null);
    }

    /**
     * Atomic replacement of the entire store contents.
     *
     * <p>Drops every existing sport event, installs {@code newSportEvents}, and
     * emits a single {@code store:resynced} event. Per-event upsert/removed
     * events are NOT emitted — consumers wanting a full diff must compute it
     * themselves from the new list.
     */
    public void resync(String reason, java.util.List<SportEvent> newSportEvents) {
        sportEvents.clear();
        byKind.clear();
        for (SportEvent ev : newSportEvents) {
            sportEvents.put(ev.id(), ev);
            byKind.computeIfAbsent(ev.kind(), k -> new LinkedHashMap<>()).put(ev.id(), ev);
        }
        emitter.emit("store:resynced", new ResyncedPayload(reason, java.util.List.copyOf(newSportEvents)));
    }

    // ─── Queries ────────────────────────────────────────────────────────────

    public SportEvent getSportEvent(String sportEventId) {
        return sportEvents.get(sportEventId);
    }

    public Market getMarket(String marketOrSelectionId) {
        for (SportEvent ev : sportEvents.values()) {
            Market m = ev.getMarket(marketOrSelectionId);
            if (m != null) {
                return m;
            }
        }
        return null;
    }

    public Selection getSelection(String selectionId) {
        for (SportEvent ev : sportEvents.values()) {
            Selection s = ev.getSelection(selectionId);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    public Map<String, SportEvent> getAllSportEvents() {
        return Collections.unmodifiableMap(sportEvents);
    }

    public Map<String, SportEvent> getSportEventsByKind(SportEventKind kind) {
        Map<String, SportEvent> map = byKind.get(kind);
        return map == null ? Map.of() : Collections.unmodifiableMap(map);
    }

    public int size() {
        return sportEvents.size();
    }
}
