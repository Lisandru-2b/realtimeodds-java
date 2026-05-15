package xyz.realtimeodds;

/**
 * Snapshot of the SDK's current connection state.
 *
 * @param status     where the client is in its lifecycle
 * @param lastError  the last fatal error observed, if any
 */
public record ConnectionState(ConnectionStatus status, Throwable lastError) {

    public static ConnectionState disconnected() {
        return new ConnectionState(ConnectionStatus.DISCONNECTED, null);
    }
}
