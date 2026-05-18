package xyz.realtimeodds.internal;

import xyz.realtimeodds.entities.Bookmaker;

/**
 * Parse the structured id grammar used by the wire format.
 *
 * <ul>
 *   <li>SportEventId : {@code vmid:<bookmaker>:<external_sport_event_id>}</li>
 *   <li>MarketId     : {@code <SportEventId>:<external_market_id>}</li>
 *   <li>SelectionId  : {@code <MarketId>:<external_selection_id>}</li>
 * </ul>
 */
public final class IdHelper {
    private IdHelper() {}

    public static Bookmaker getBookmaker(String entityId) {
        String[] parts = entityId.split(":");
        if (parts.length < 3 || !"vmid".equals(parts[0])) {
            throw new IllegalArgumentException("Invalid entity id format: " + entityId);
        }
        return Bookmaker.fromValue(parts[1]);
    }

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

    public static String makeSportEventId(Bookmaker bookmaker, String externalId) {
        return "vmid:" + bookmaker.value() + ":" + externalId;
    }

    public static String makeMarketId(String sportEventId, String externalMarketId) {
        return sportEventId + ":" + externalMarketId;
    }

    public static String makeSelectionId(String marketId, String externalSelectionId) {
        return marketId + ":" + externalSelectionId;
    }
}
