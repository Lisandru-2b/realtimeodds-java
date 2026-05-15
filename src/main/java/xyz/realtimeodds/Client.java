package xyz.realtimeodds;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import xyz.realtimeodds.entities.Bookmaker;
import xyz.realtimeodds.entities.Market;
import xyz.realtimeodds.entities.Quote;
import xyz.realtimeodds.entities.Selection;
import xyz.realtimeodds.entities.SportEvent;
import xyz.realtimeodds.events.ConnectedEvent;
import xyz.realtimeodds.events.DisconnectedEvent;
import xyz.realtimeodds.events.ErrorEvent;
import xyz.realtimeodds.events.OddsChangedEvent;
import xyz.realtimeodds.events.ReconnectingEvent;
import xyz.realtimeodds.events.ResyncEvent;
import xyz.realtimeodds.events.SourceClearedEvent;
import xyz.realtimeodds.events.SportEventAddedEvent;
import xyz.realtimeodds.events.SportEventRemovedEvent;
import xyz.realtimeodds.events.SportEventUpdatedEvent;
import xyz.realtimeodds.internal.OddsStore;
import xyz.realtimeodds.internal.TypedEmitter;
import xyz.realtimeodds.internal.gateway.GatewayClient;
import xyz.realtimeodds.internal.gateway.Protocol;

/**
 * The realtimeodds SDK client.
 *
 * <p>Open the WebSocket with {@code client.connect().join()} (or compose with
 * other {@link CompletableFuture}s). Subscribe to events with
 * {@code client.onXxx(callback)}. The client auto-reconnects with exponential
 * backoff on transient drops.
 *
 * <p>Build via {@link RealtimeOddsClient#builder()}.
 */
public final class Client {

    private final String url;
    private final String apiKey;
    private final ReconnectPolicy reconnect;
    private final TypedEmitter emitter = new TypedEmitter();
    private final AtomicReference<ConnectionState> state =
            new AtomicReference<>(ConnectionState.disconnected());
    private final Set<Integer> wiredStores = new HashSet<>();
    private GatewayClient gw;
    private CompletableFuture<Void> pending;

    Client(String url, String apiKey, ReconnectPolicy reconnect) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("url is required");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("apiKey is required");
        }
        this.url = url;
        this.apiKey = apiKey;
        this.reconnect = reconnect != null ? reconnect : ReconnectPolicy.defaults();
    }

    // ─── Lifecycle ──────────────────────────────────────────────────────────

    public ConnectionState connectionState() {
        return state.get();
    }

    /**
     * Open the WebSocket. The returned future completes when the {@code hello}
     * handshake succeeds (or completes exceptionally on a fatal error: invalid
     * apiKey, exhausted reconnect attempts, incompatible protocol).
     *
     * <p>Concurrent calls return the same future. Calling after a successful
     * connection returns an already-completed future.
     */
    public synchronized CompletableFuture<Void> connect() {
        if (pending != null) {
            return pending;
        }
        if (state.get().status() == ConnectionStatus.CONNECTED) {
            return CompletableFuture.completedFuture(null);
        }
        pending = new CompletableFuture<>();
        state.set(new ConnectionState(ConnectionStatus.CONNECTING, null));

        String fullUrl = appendApiKey(url, apiKey);
        gw = new GatewayClient(fullUrl, reconnect);
        attachGatewayHandlers(gw);
        gw.connect();
        return pending;
    }

    /**
     * Close and stop reconnecting. Idempotent. If a {@link #connect()} is in
     * flight, it completes exceptionally.
     */
    public synchronized CompletableFuture<Void> disconnect() {
        CompletableFuture<Void> p = pending;
        if (p != null && !p.isDone()) {
            p.completeExceptionally(
                    new IllegalStateException("disconnect() called before connect() completed"));
        }
        pending = null;
        if (gw != null) {
            gw.disconnect();
            gw = null;
        }
        state.set(ConnectionState.disconnected());
        return CompletableFuture.completedFuture(null);
    }

    public Snapshot snapshot() {
        Map<String, SportEvent> all = new LinkedHashMap<>();
        if (gw != null) {
            for (OddsStore store : gw.getStores().values()) {
                all.putAll(store.getAllSportEvents());
            }
        }
        return new Snapshot(all, state.get().status() != ConnectionStatus.CONNECTED);
    }

    public SportEvent getSportEvent(String sportEventId) {
        if (gw == null) {
            return null;
        }
        for (OddsStore store : gw.getStores().values()) {
            SportEvent ev = store.getSportEvent(sportEventId);
            if (ev != null) {
                return ev;
            }
        }
        return null;
    }

    // ─── Event subscription ─────────────────────────────────────────────────

    public void onConnected(Consumer<ConnectedEvent> listener) {
        emitter.on("connected", listener);
    }

    public void onDisconnected(Consumer<DisconnectedEvent> listener) {
        emitter.on("disconnected", listener);
    }

    public void onReconnecting(Consumer<ReconnectingEvent> listener) {
        emitter.on("reconnecting", listener);
    }

    public void onError(Consumer<ErrorEvent> listener) {
        emitter.on("error", listener);
    }

    public void onSportEventAdded(Consumer<SportEventAddedEvent> listener) {
        emitter.on("sportEvent:added", listener);
    }

    public void onSportEventUpdated(Consumer<SportEventUpdatedEvent> listener) {
        emitter.on("sportEvent:updated", listener);
    }

    public void onSportEventRemoved(Consumer<SportEventRemovedEvent> listener) {
        emitter.on("sportEvent:removed", listener);
    }

    public void onOddsChanged(Consumer<OddsChangedEvent> listener) {
        emitter.on("odds:changed", listener);
    }

    public void onSourceCleared(Consumer<SourceClearedEvent> listener) {
        emitter.on("source:cleared", listener);
    }

    public void onResync(Consumer<ResyncEvent> listener) {
        emitter.on("resync", listener);
    }

    public <T> void off(String event, Consumer<T> listener) {
        emitter.off(event, listener);
    }

    // ─── Internals ──────────────────────────────────────────────────────────

    private static String appendApiKey(String url, String apiKey) {
        URI uri = URI.create(url);
        String existingQuery = uri.getQuery();
        String extra = "apiKey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        String newQuery = existingQuery == null || existingQuery.isEmpty() ? extra : existingQuery + "&" + extra;
        try {
            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), newQuery, uri.getFragment()).toString();
        } catch (Exception e) {
            // Fallback for unusual URLs
            String sep = url.contains("?") ? "&" : "?";
            return url + sep + extra;
        }
    }

    private static long nowMs() {
        return System.currentTimeMillis();
    }

    private void attachGatewayHandlers(GatewayClient gw) {
        gw.events().<Object>on("connected", p -> onGwConnected());
        gw.events().<GatewayClient.DisconnectedPayload>on("disconnected", this::onGwDisconnected);
        gw.events().<GatewayClient.ReconnectScheduledPayload>on("reconnect_scheduled", this::onGwReconnectScheduled);
        gw.events().<GatewayClient.ExhaustedPayload>on("exhausted", this::onGwExhausted);
        gw.events().<GatewayClient.IncompatiblePayload>on("incompatible", this::onGwIncompatible);
        gw.events().<Throwable>on("error", this::onGwError);
        gw.events().<GatewayClient.SourceAddedPayload>on("source:added", this::onGwSourceAdded);
        gw.events().<String>on("source:cleared", this::onGwSourceCleared);
    }

    private synchronized void onGwConnected() {
        state.set(new ConnectionState(ConnectionStatus.CONNECTED, null));
        emitter.emit("connected", ConnectedEvent.INSTANCE);
        if (pending != null && !pending.isDone()) {
            pending.complete(null);
        }
    }

    private synchronized void onGwDisconnected(GatewayClient.DisconnectedPayload p) {
        emitter.emit("disconnected",
                new DisconnectedEvent(p.willReconnect(), p.code(), p.reason() == null ? "" : p.reason()));
        if (Protocol.isAuthCloseCode(p.code())) {
            String msg = Protocol.authCloseMessage(p.code(), p.reason());
            RuntimeException err = new RuntimeException(msg);
            state.set(new ConnectionState(ConnectionStatus.DISCONNECTED, err));
            emitter.emit("error", new ErrorEvent(msg, true));
            failPending(err);
            return;
        }
        if (!p.willReconnect()) {
            state.set(ConnectionState.disconnected());
        }
    }

    private void onGwReconnectScheduled(GatewayClient.ReconnectScheduledPayload p) {
        state.set(new ConnectionState(ConnectionStatus.RECONNECTING, null));
        emitter.emit("reconnecting", new ReconnectingEvent(p.attempt(), p.delayMs()));
    }

    private synchronized void onGwExhausted(GatewayClient.ExhaustedPayload p) {
        RuntimeException err = new RuntimeException(p.reason());
        state.set(new ConnectionState(ConnectionStatus.DISCONNECTED, err));
        emitter.emit("error", new ErrorEvent(p.reason(), true));
        failPending(err);
    }

    private synchronized void onGwIncompatible(GatewayClient.IncompatiblePayload p) {
        RuntimeException err = new RuntimeException(p.reason());
        state.set(new ConnectionState(ConnectionStatus.DISCONNECTED, err));
        emitter.emit("error", new ErrorEvent(p.reason(), true));
        failPending(err);
    }

    private void onGwError(Throwable err) {
        String msg = err == null
                ? "Unknown error"
                : (err.getMessage() == null ? err.getClass().getSimpleName() : err.getMessage());
        emitter.emit("error", new ErrorEvent(msg, false));
    }

    private void onGwSourceAdded(GatewayClient.SourceAddedPayload p) {
        int sentinel = System.identityHashCode(p.store());
        synchronized (wiredStores) {
            if (!wiredStores.add(sentinel)) {
                return;
            }
        }
        Bookmaker bookmaker;
        try {
            bookmaker = Bookmaker.fromValue(p.source());
        } catch (IllegalArgumentException e) {
            // Unknown bookmaker — skip wiring, surface error.
            emitter.emit("error", new ErrorEvent("Unknown bookmaker: " + p.source(), false));
            return;
        }
        wireStore(p.store(), bookmaker);
    }

    private void onGwSourceCleared(String source) {
        Bookmaker bookmaker;
        try {
            bookmaker = Bookmaker.fromValue(source);
        } catch (IllegalArgumentException e) {
            return;
        }
        emitter.emit("source:cleared", new SourceClearedEvent(bookmaker, nowMs()));
    }

    private void wireStore(OddsStore store, Bookmaker bookmaker) {
        store.events().<OddsStore.UpsertedPayload>on("sportEvent:upserted", payload -> {
            long now = nowMs();
            if (payload.isNew()) {
                emitter.emit("sportEvent:added", new SportEventAddedEvent(payload.sportEvent(), now));
            } else {
                emitter.emit("sportEvent:updated", new SportEventUpdatedEvent(payload.sportEvent(), now));
            }
        });
        store.events().<OddsStore.RemovedPayload>on("sportEvent:removed", payload ->
                emitter.emit("sportEvent:removed",
                        new SportEventRemovedEvent(bookmaker, payload.sportEventId(), nowMs())));
        store.events().<OddsStore.PricesUpdatedPayload>on("prices:updated", payload -> {
            long now = nowMs();
            SportEvent updated = store.getSportEvent(payload.sportEventId());
            if (updated != null) {
                emitter.emit("sportEvent:updated", new SportEventUpdatedEvent(updated, now));
            }
            for (String selectionId : payload.prices().keySet()) {
                Market market = store.getMarket(selectionId);
                if (market == null) {
                    continue;
                }
                Selection selection = market.getSelection(selectionId);
                if (selection == null || selection.quote() == null) {
                    continue;
                }
                emitter.emit("odds:changed",
                        new OddsChangedEvent(
                                bookmaker,
                                payload.sportEventId(),
                                market.id(),
                                selectionId,
                                selection.quote(),
                                now));
            }
        });
        store.events().<OddsStore.ResyncedPayload>on("store:resynced", payload ->
                emitter.emit("resync",
                        new ResyncEvent(bookmaker, payload.reason(), payload.sportEvents(), nowMs())));
    }

    private synchronized void failPending(RuntimeException err) {
        if (pending != null && !pending.isDone()) {
            pending.completeExceptionally(err);
        }
    }

    @Override
    public String toString() {
        return "Client{url=" + url + ", state=" + state.get().status() + "}";
    }

    /** @hidden internal use only */
    Object stateForTest() {
        return Objects.toString(state.get());
    }
}
