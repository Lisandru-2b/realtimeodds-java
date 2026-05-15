package xyz.realtimeodds.events;

/**
 * Fired when a reconnect attempt is scheduled.
 *
 * @param attempt 1-based attempt counter, reset on successful connection
 * @param delayMs the actual delay (after backoff + jitter) before this attempt fires
 */
public record ReconnectingEvent(int attempt, long delayMs) {}
