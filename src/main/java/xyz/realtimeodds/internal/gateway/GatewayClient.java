package xyz.realtimeodds.internal.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import xyz.realtimeodds.ReconnectPolicy;
import xyz.realtimeodds.entities.SportEvent;
import xyz.realtimeodds.internal.OddsStore;
import xyz.realtimeodds.internal.TypedEmitter;

/**
 * Async WebSocket client for the realtimeodds gateway.
 *
 * <p>Performs the {@code hello} handshake, applies snapshots, then dispatches
 * mutations into per-source {@link OddsStore} instances.
 *
 * <p>Events emitted on {@link #events()}:
 * <ul>
 *   <li>{@code connected} — payload: {@code null}</li>
 *   <li>{@code disconnected} — payload: {@link DisconnectedPayload}</li>
 *   <li>{@code reconnect_scheduled} — payload: {@link ReconnectScheduledPayload}</li>
 *   <li>{@code exhausted} — payload: {@link ExhaustedPayload}</li>
 *   <li>{@code incompatible} — payload: {@link IncompatiblePayload}</li>
 *   <li>{@code warning} — payload: {@link WarningPayload}</li>
 *   <li>{@code error} — payload: {@link Throwable}</li>
 *   <li>{@code source:added} — payload: {@link SourceAddedPayload}</li>
 *   <li>{@code source:cleared} — payload: source id (String)</li>
 * </ul>
 */
public final class GatewayClient {

    public record DisconnectedPayload(boolean willReconnect, int code, String reason) {}

    public record ReconnectScheduledPayload(int attempt, long delayMs) {}

    public record ExhaustedPayload(int attempts, String reason) {}

    public record IncompatiblePayload(String reason, String serverVersion) {}

    public record WarningPayload(String reason) {}

    public record SourceAddedPayload(String source, OddsStore store) {}

    private final String url;
    private final ReconnectPolicy reconnect;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final ScheduledExecutorService scheduler;
    private final TypedEmitter events = new TypedEmitter();
    private final Map<String, OddsStore> stores = new LinkedHashMap<>();

    private volatile boolean running = false;
    private volatile boolean handshakeDone = false;
    private volatile WebSocket ws;
    private volatile int attempt = 0;
    private ScheduledFuture<?> reconnectFuture;
    private final StringBuilder fragmentBuffer = new StringBuilder();

    public GatewayClient(String url, ReconnectPolicy reconnect) {
        this.url = url;
        this.reconnect = reconnect != null ? reconnect : ReconnectPolicy.defaults();
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.http = HttpClient.newHttpClient();
        ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "realtimeodds-reconnect");
            t.setDaemon(true);
            return t;
        });
        exec.setRemoveOnCancelPolicy(true);
        this.scheduler = exec;
    }

    public TypedEmitter events() {
        return events;
    }

    public Map<String, OddsStore> getStores() {
        return stores;
    }

    public OddsStore getOrCreateStore(String source) {
        OddsStore store = stores.get(source);
        if (store == null) {
            store = new OddsStore();
            stores.put(source, store);
            events.emit("source:added", new SourceAddedPayload(source, store));
        }
        return store;
    }

    public synchronized void connect() {
        if (running) {
            return;
        }
        running = true;
        attempt = 0;
        openSocket();
    }

    public synchronized void disconnect() {
        running = false;
        handshakeDone = false;
        if (reconnectFuture != null) {
            reconnectFuture.cancel(false);
            reconnectFuture = null;
        }
        WebSocket current = ws;
        ws = null;
        if (current != null) {
            try {
                current.sendClose(WebSocket.NORMAL_CLOSURE, "client disconnect");
            } catch (Exception ignored) {
            }
        }
    }

    private void openSocket() {
        handshakeDone = false;
        fragmentBuffer.setLength(0);
        WebSocket.Listener listener = new Listener();
        http.newWebSocketBuilder()
                .buildAsync(URI.create(url), listener)
                .whenComplete((sock, err) -> {
                    if (err != null) {
                        events.emit("error", err);
                        onClose(0, err.getMessage() == null ? "open failed" : err.getMessage());
                    } else {
                        ws = sock;
                    }
                });
    }

    private synchronized void onClose(int code, String reason) {
        if (Protocol.isAuthCloseCode(code)) {
            running = false;
        }
        boolean willReconnect = running;
        handshakeDone = false;
        events.emit("disconnected", new DisconnectedPayload(willReconnect, code, reason));
        ws = null;
        if (willReconnect) {
            scheduleReconnect();
        }
    }

    private synchronized void scheduleReconnect() {
        if (reconnectFuture != null && !reconnectFuture.isDone()) {
            return;
        }
        attempt += 1;
        if (attempt > reconnect.maxAttempts()) {
            running = false;
            events.emit("exhausted",
                    new ExhaustedPayload(attempt,
                            "max reconnect attempts (" + reconnect.maxAttempts() + ") exceeded"));
            return;
        }
        long delay = reconnect.computeDelayMs(attempt);
        events.emit("reconnect_scheduled", new ReconnectScheduledPayload(attempt, delay));
        reconnectFuture = scheduler.schedule(() -> {
            synchronized (this) {
                reconnectFuture = null;
                if (running) {
                    openSocket();
                }
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    // ─── Message handling ───────────────────────────────────────────────────

    private void handleMessage(String text) {
        JsonNode msg;
        try {
            msg = mapper.readTree(text);
        } catch (Exception err) {
            events.emit("error", err);
            return;
        }
        if (!handshakeDone) {
            handleHandshake(msg);
            return;
        }
        try {
            dispatch(msg);
        } catch (Exception err) {
            events.emit("error", err);
        }
    }

    private void handleHandshake(JsonNode msg) {
        String type = msg.path("type").asText();
        if (!"hello".equals(type)) {
            String reason = "First message was " + type + ", expected 'hello'. "
                    + "Refusing connection per PROTOCOL.md.";
            refuse(reason, null);
            return;
        }
        String serverVersion = msg.path("data").path("protocolVersion").asText("");
        Protocol.VersionCheckResult result = Protocol.checkCompatibility(serverVersion);
        if (result instanceof Protocol.Incompatible inc) {
            refuse(inc.reason(), serverVersion);
            return;
        }
        if (result instanceof Protocol.VersionWarning warn) {
            events.emit("warning", new WarningPayload(warn.reason()));
        }
        handshakeDone = true;
        attempt = 0;
        events.emit("connected", null);
    }

    private void refuse(String reason, String serverVersion) {
        running = false;
        WebSocket current = ws;
        ws = null;
        if (current != null) {
            try {
                current.sendClose(4000, reason);
            } catch (Exception ignored) {
            }
        }
        events.emit("incompatible", new IncompatiblePayload(reason, serverVersion));
    }

    private void dispatch(JsonNode msg) throws Exception {
        String type = msg.path("type").asText();
        switch (type) {
            case "snapshot" -> {
                for (JsonNode entry : msg.path("data")) {
                    String source = entry.path("source").asText(null);
                    if (source == null) {
                        continue;
                    }
                    ObjectNode payload = ((ObjectNode) entry).deepCopy();
                    payload.remove("source");
                    SportEvent ev = mapper.treeToValue(payload, SportEvent.class);
                    getOrCreateStore(source).upsertSportEvent(ev);
                }
            }
            case "new_event", "update_event" -> {
                String source = msg.path("source").asText(null);
                if (source == null) {
                    return;
                }
                SportEvent ev = mapper.treeToValue(msg.path("data"), SportEvent.class);
                getOrCreateStore(source).upsertSportEvent(ev);
            }
            case "prices_updated" -> {
                String source = msg.path("source").asText(null);
                if (source == null) {
                    return;
                }
                JsonNode data = msg.path("data");
                String sportEventId = data.path("sportEventId").asText();
                Map<String, Double> prices = new HashMap<>();
                for (JsonNode p : data.path("prices")) {
                    prices.put(p.path("selectionId").asText(), p.path("price").doubleValue());
                }
                getOrCreateStore(source).updatePrices(sportEventId, prices);
            }
            case "remove_event" -> {
                String source = msg.path("source").asText(null);
                if (source == null) {
                    return;
                }
                String sportEventId = msg.path("data").path("sportEventId").asText();
                getOrCreateStore(source).removeSportEvent(sportEventId);
            }
            case "store_cleared" -> {
                String source = msg.path("source").asText(null);
                if (source == null) {
                    return;
                }
                OddsStore existing = stores.get(source);
                if (existing != null) {
                    existing.clear();
                    events.emit("source:cleared", source);
                }
            }
            case "resync" -> {
                String source = msg.path("source").asText(null);
                if (source == null) {
                    return;
                }
                String reason = msg.path("reason").asText("");
                java.util.List<SportEvent> newEvents = new java.util.ArrayList<>();
                for (JsonNode entry : msg.path("data")) {
                    newEvents.add(mapper.treeToValue(entry, SportEvent.class));
                }
                getOrCreateStore(source).resync(reason, newEvents);
            }
            case "hello" -> {
                // Unexpected post-handshake hello — ignore.
            }
            default -> {
                // Unknown type: ignore for forward-compat.
            }
        }
    }

    // ─── WebSocket listener ─────────────────────────────────────────────────

    private final class Listener implements WebSocket.Listener {
        @Override
        public void onOpen(WebSocket sock) {
            ws = sock;
            sock.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket sock, CharSequence data, boolean last) {
            fragmentBuffer.append(data);
            if (last) {
                String text = fragmentBuffer.toString();
                fragmentBuffer.setLength(0);
                handleMessage(text);
            }
            sock.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket sock, int code, String reason) {
            GatewayClient.this.onClose(code, reason == null ? "" : reason);
            return null;
        }

        @Override
        public void onError(WebSocket sock, Throwable err) {
            events.emit("error", err);
            GatewayClient.this.onClose(0, err.getMessage() == null ? "transport error" : err.getMessage());
        }
    }
}
