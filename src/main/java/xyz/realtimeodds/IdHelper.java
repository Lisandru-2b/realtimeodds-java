package xyz.realtimeodds;

import xyz.realtimeodds.entities.Bookmaker;

/**
 * Parse and compose the structured id grammar used by the wire format.
 *
 * <ul>
 *   <li>SportEventId : {@code vmid:<bookmaker>:<external_sport_event_id>}</li>
 *   <li>MarketId     : {@code <SportEventId>:<external_market_id>}</li>
 *   <li>SelectionId  : {@code <MarketId>:<external_selection_id>}</li>
 * </ul>
 *
 * <p>Equivalent to the {@code IdHelper} class in {@code realtimeodds-js} and
 * {@code realtimeodds-python}: same method names, same semantics. Use these
 * helpers when you need to derive ids from each other (extracting a MarketId
 * from a SelectionId, etc.); avoid parsing the strings yourself.
 *
 * <p>Example:
 * <pre>{@code
 * import xyz.realtimeodds.IdHelper;
 *
 * Bookmaker bm = IdHelper.getBookmaker(selectionId);
 * String sportEventId = IdHelper.getSportEventId(marketId);
 * String marketId = IdHelper.getMarketId(selectionId);
 * }</pre>
 */
public final class IdHelper {
    private IdHelper() {}

    /** Parse the bookmaker from any entity id ({@code vmid:<bookmaker>:...}). */
    public static Bookmaker getBookmaker(String entityId) {
        String[] parts = entityId.split(":");
        if (parts.length < 3 || !"vmid".equals(parts[0])) {
            throw new IllegalArgumentException("Invalid entity id format: " + entityId);
        }
        return Bookmaker.fromValue(parts[1]);
    }

    /** Truncate any entity id to the SportEventId prefix. */
    public static String getSportEventId(String entityId) {
        String[] parts = entityId.split(":");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid entity id format: " + entityId);
        }
        return String.join(":", parts[0], parts[1], parts[2]);
    }

    /**
     * Resolve any MarketId or SelectionId to the MarketId.
     *
     * <p>The grammar reserves {@code :} as the top-level segment separator.
     * Producers SHOULD avoid {@code :} inside {@code external_market_id} /
     * {@code external_selection_id} (use {@code _} instead). This helper
     * tolerates inputs where a producer embedded {@code :} internally: with
     * 5+ segments we assume the input is a SelectionId and strip its
     * trailing segment; with fewer it is returned verbatim.
     */
    public static String getMarketId(String entityId) {
        String[] parts = entityId.split(":");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Cannot extract MarketId from " + entityId);
        }
        if (parts.length < 5) {
            return entityId;
        }
        int lastColon = entityId.lastIndexOf(':');
        return entityId.substring(0, lastColon);
    }

    /** Compose a SportEventId from its parts. */
    public static String makeSportEventId(Bookmaker bookmaker, String externalId) {
        return "vmid:" + bookmaker.value() + ":" + externalId;
    }

    /** Compose a MarketId from a SportEventId and the bookmaker-specific market id. */
    public static String makeMarketId(String sportEventId, String externalMarketId) {
        return sportEventId + ":" + externalMarketId;
    }

    /** Compose a SelectionId from a MarketId and the bookmaker-specific selection id. */
    public static String makeSelectionId(String marketId, String externalSelectionId) {
        return marketId + ":" + externalSelectionId;
    }
}
