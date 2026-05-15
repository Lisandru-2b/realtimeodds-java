package xyz.realtimeodds.events;

/** Fired once the WebSocket handshake has completed and the protocol is compatible. */
public record ConnectedEvent() {
    public static final ConnectedEvent INSTANCE = new ConnectedEvent();
}
