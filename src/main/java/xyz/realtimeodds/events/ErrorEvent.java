package xyz.realtimeodds.events;

/**
 * Fired when the SDK observes a non-fatal or fatal error.
 *
 * @param message human-readable description
 * @param fatal   when {@code true}, the SDK will not attempt to reconnect
 *                (e.g. invalid apiKey, exhausted attempts, incompatible protocol)
 */
public record ErrorEvent(String message, boolean fatal) {}
