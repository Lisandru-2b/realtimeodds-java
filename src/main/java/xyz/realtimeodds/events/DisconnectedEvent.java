package xyz.realtimeodds.events;

/**
 * Fired when the socket drops.
 *
 * @param willReconnect whether the SDK will attempt reconnect
 * @param code          the WebSocket close code as reported by the transport
 * @param reason        free-form reason from the server
 */
public record DisconnectedEvent(boolean willReconnect, int code, String reason) {}
