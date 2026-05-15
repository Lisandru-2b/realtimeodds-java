package xyz.realtimeodds.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import xyz.realtimeodds.internal.IdHelper;

/**
 * A bettable outcome within a {@link Market}.
 *
 * <p>A selection is <b>unavailable</b> when {@code quote == null}. Reading
 * {@link #price()} on an unavailable selection throws.
 *
 * @param id     selection id (format {@code <MarketId>:<external_id>})
 * @param kind   discriminator for valid {@code result} values
 * @param result which outcome this selection represents
 * @param quote  top-of-book quote (nullable)
 * @param orderBook full limit order book (nullable; only on CLOB sources)
 */
public record Selection(
        String id,
        SelectionKind kind,
        SelectionResult result,
        Quote quote,
        OrderBook orderBook) {

    public Selection {
        if (id == null) {
            throw new NullPointerException("Selection id is required");
        }
        if (!kind.isValidResult(result)) {
            throw new IllegalArgumentException(
                    "Invalid selection result " + result + " for kind " + kind);
        }
    }

    @JsonCreator
    public static Selection of(
            @JsonProperty("id") String id,
            @JsonProperty("kind") SelectionKind kind,
            @JsonProperty("result") SelectionResult result,
            @JsonProperty("quote") Quote quote,
            @JsonProperty("orderBook") OrderBook orderBook) {
        return new Selection(id, kind, result, quote, orderBook);
    }

    public Bookmaker bookmaker() {
        return IdHelper.getBookmaker(id);
    }

    public boolean isAvailable() {
        return quote != null;
    }

    /** Decimal odds. Throws {@link IllegalStateException} if unavailable. */
    public double price() {
        if (quote == null) {
            throw new IllegalStateException("Selection " + id + " is not available");
        }
        return quote.price();
    }

    /** Internal: return a new Selection with a different quote. */
    public Selection withQuote(Quote newQuote) {
        return new Selection(id, kind, result, newQuote, orderBook);
    }

    /** Internal: return a new Selection with the given price (new Quote with local timestamp). */
    public Selection withPrice(double price) {
        return withQuote(new Quote(price, null, System.currentTimeMillis()));
    }

    /** Internal: return a new Selection marked unavailable. */
    public Selection withUnavailability() {
        return new Selection(id, kind, result, null, null);
    }
}
