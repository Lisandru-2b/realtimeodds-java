package xyz.realtimeodds.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Simple multi-listener event emitter keyed by event name.
 *
 * <p>Synchronous: callbacks fire on the same thread as the {@code emit()}
 * caller. Listener exceptions are swallowed so one bad handler doesn't poison
 * the rest.
 */
public final class TypedEmitter {

    private final Map<String, List<Consumer<Object>>> listeners = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> void on(String event, Consumer<T> listener) {
        listeners.computeIfAbsent(event, k -> new ArrayList<>()).add((Consumer<Object>) listener);
    }

    @SuppressWarnings("unchecked")
    public <T> void off(String event, Consumer<T> listener) {
        List<Consumer<Object>> list = listeners.get(event);
        if (list != null) {
            list.remove((Consumer<Object>) listener);
        }
    }

    public void emit(String event, Object payload) {
        List<Consumer<Object>> list = listeners.get(event);
        if (list == null || list.isEmpty()) {
            return;
        }
        // Snapshot so listeners can mutate the registry while iterating.
        for (Consumer<Object> listener : List.copyOf(list)) {
            try {
                listener.accept(payload);
            } catch (Exception ignored) {
                // Don't let a bad handler poison the others.
            }
        }
    }

    public void removeAll(String event) {
        listeners.remove(event);
    }

    public void clear() {
        listeners.clear();
    }
}
