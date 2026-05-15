package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Limit order book for a selection. Only present on bookmakers that expose
 * a CLOB (e.g. Polymarket).
 *
 * @param bids bid levels, sorted DESC by price (best first).
 * @param asks ask levels, sorted ASC by price (best first).
 * @param timestamp Unix timestamp in ms.
 */
public record OrderBook(
        List<Level> bids,
        List<Level> asks,
        long timestamp) {

    public OrderBook {
        bids = List.copyOf(bids);
        asks = List.copyOf(asks);
    }

    @JsonCreator
    public static OrderBook of(
            @JsonProperty("bids") List<Level> bids,
            @JsonProperty("asks") List<Level> asks,
            @JsonProperty("timestamp") long timestamp) {
        return new OrderBook(bids == null ? List.of() : bids, asks == null ? List.of() : asks, timestamp);
    }

    public Level bestBid() {
        return bids.isEmpty() ? null : bids.get(0);
    }

    public Level bestAsk() {
        return asks.isEmpty() ? null : asks.get(0);
    }

    public Double spread() {
        Level bb = bestBid();
        Level ba = bestAsk();
        return (bb == null || ba == null) ? null : ba.price() - bb.price();
    }

    public Double midPrice() {
        Level bb = bestBid();
        Level ba = bestAsk();
        return (bb == null || ba == null) ? null : (ba.price() + bb.price()) / 2.0;
    }

    /** Total ask-side size available up to (and including) {@code maxPrice}. */
    public double availableSizeUpTo(double maxPrice) {
        double total = 0.0;
        for (Level level : asks) {
            if (level.price() > maxPrice) {
                break;
            }
            total += level.size();
        }
        return total;
    }
}
