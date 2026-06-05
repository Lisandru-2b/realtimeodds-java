# realtimeodds-java

Real-time betting odds SDK for Java — multi-bookmaker, sport-discriminated, async with `CompletableFuture`.

The SDK is a **strict replica** of the gateway's internal stores: same shapes, same fields, same getters and read-side methods. Discriminated unions implemented as Java 17 `sealed interface` + `record`, so consumer code can `switch` exhaustively with full type narrowing.

> Status: 0.3.2 — alpha. Stable through the `0.x` line.

## Install

Published on **Maven Central**. Requires Java 17+.

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("xyz.realtimeodds:realtimeodds-java:0.3.2")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'xyz.realtimeodds:realtimeodds-java:0.3.2'
}
```

### Maven

```xml
<dependency>
    <groupId>xyz.realtimeodds</groupId>
    <artifactId>realtimeodds-java</artifactId>
    <version>0.3.2</version>
</dependency>
```

No custom repository to declare — `mavenCentral()` is the default in modern Gradle, and Maven resolves it out of the box. Artifacts are GPG-signed (key id `65A344DC`) and ship with sources + javadoc jars.

## Quickstart

```java
import xyz.realtimeodds.RealtimeOddsClient;
import xyz.realtimeodds.entities.BasketballMatch;

public class Quickstart {
    public static void main(String[] args) throws Exception {
        var client = RealtimeOddsClient.builder()
            .url("wss://api.realtimeodds.xyz/ws")
            .apiKey(System.getenv("REALTIMEODDS_API_KEY"))
            .build();

        client.onOddsChanged(ev -> {
            var ctx = client.odds().findContext(ev.selectionId());
            if (ctx == null || !(ctx.sportEvent() instanceof BasketballMatch match)) return;

            System.out.printf("[%s] %s · %s · %s → %.2f%n",
                ev.bookmaker(), match.name(),
                ctx.market().kind(), ctx.selection().result(), ev.quote().price()
            );
        });

        client.connect().join();
        Thread.sleep(60_000);
        client.disconnect().join();
    }
}
```

## API

| Method | Behaviour |
|---|---|
| `RealtimeOddsClient.builder().url(...).apiKey(...).build()` | Construct a client. |
| `client.connect()` | Open the WebSocket. Returns `CompletableFuture<Void>` resolving on first successful connection; completes exceptionally on fatal errors. Concurrent calls share the same future. |
| `client.disconnect()` | Close and stop reconnecting. Idempotent. Fails any in-flight `connect()`. The live `OddsBook` is emptied immediately. |
| `client.odds()` | Live `OddsBook` — same instance every read, mutated in place as wire messages arrive. |
| `client.snapshot()` | Frozen clone of the live book taken at the moment of the call. Use when you need a stable view across multiple reads. |
| `client.getSportEvent(id)` | O(1) single lookup. Returns `null` if unknown. Convenience shortcut for `client.odds().getSportEvent(id)`. |
| `client.onXxx(cb)` / `client.off(event, cb)` | Subscribe / unsubscribe. Synchronous callbacks (`Consumer<T>`). |
| `client.connectionState()` | `ConnectionState(status, lastError)`. |

### `OddsBook`

Read-only view of every sport event the SDK knows about, across every bookmaker. All lookups are O(1) thanks to maintained inverse indexes. Implements `Iterable<SportEvent>` for direct iteration.

```java
client.odds().size();                              // int
client.odds().getSportEvent(sportEventId);         // SportEvent | null
client.odds().getMarket(marketId);                 // Market | null
client.odds().getSelection(selectionId);           // Selection | null
client.odds().findContext(selectionId);            // OddsContext | null
for (var ev : client.odds()) { ... }               // iterate every event
```

`findContext` returns an `OddsContext(SportEvent sportEvent, Market market, Selection selection)` record in one O(1) lookup — recommended inside an `onOddsChanged` handler when you need more than just the price.

### Events

Synchronous callbacks (`Consumer<T>`). Each event payload is a `record`.

| Event | Payload |
|---|---|
| `connected` | `ConnectedEvent` (singleton) |
| `disconnected` | `DisconnectedEvent(willReconnect, code, reason)` — the live `OddsBook` is emptied before this fires. |
| `reconnecting` | `ReconnectingEvent(attempt, delayMs)` |
| `error` | `ErrorEvent(message, fatal)` |
| `sportEvent:added` | `SportEventAddedEvent(sportEvent, receivedAt)` |
| `sportEvent:updated` | `SportEventUpdatedEvent(sportEvent, receivedAt)` — fires on metadata change OR on any odds change |
| `sportEvent:removed` | `SportEventRemovedEvent(bookmaker, sportEventId, receivedAt)` |
| `odds:changed` | `OddsChangedEvent(bookmaker, sportEventId, marketId, selectionId, quote, receivedAt)` |
| `source:cleared` | `SourceClearedEvent(bookmaker, receivedAt)` — a bookmaker source went away. The SDK has already purged its events from the live book. |
| `resync` | `ResyncEvent(bookmaker, reason, sportEvents, receivedAt)` — atomic full-state replacement for one bookmaker. The live book has already swapped the affected slice. |

Close codes 4001/4002/4003 are fatal auth codes; `fatal=true` errors stop the client.

## Entities

Frozen `record` types implementing `sealed interface` unions. Branch with `switch` or `instanceof` for exhaustive type narrowing.

- **`SportEvent`**: sport-specific match records, plus `UnknownSportEvent` fallback for forward compatibility.
- **`Market`**: sport-specific market records, including tennis moneyline/handicap/total, plus `UnknownMarket` fallback for forward compatibility.
- **`Selection`**: `id`, `kind`, `result`, `quote`, `orderBook`, `bookmaker`, `isAvailable`, `price()` (throws if unavailable).
- **`Quote`**: `price`, `size`, `timestamp`, `impliedProbability()`.
- **`OrderBook`**: `bids`, `asks`, `timestamp`, `bestBid()`, `bestAsk()`, `spread()`, `midPrice()`, `availableSizeUpTo(maxPrice)`.

Sport-specific fields (`homeTeam`/`awayTeam`/`competitor1`/`competitor2`/`period`/`handicap`/`scope`/`cut`/`playerName`/`propType`) live on the relevant subtypes.

## Sport / kind narrowing

```java
client.onSportEventAdded(ev -> {
    var se = ev.sportEvent();
    if (se instanceof BasketballMatch b) {
        System.out.println(b.homeTeam() + " vs " + b.awayTeam());
    } else if (se instanceof FootballMatch f) {
        System.out.println(f.homeTeam() + " vs " + f.awayTeam());
    } else if (se instanceof TennisMatch t) {
        System.out.println(t.competitor1() + " vs " + t.competitor2());
    }
    for (var market : se.markets().values()) {
        if (market instanceof BasketballHandicap h) {
            System.out.println("handicap=" + h.handicap());
        }
    }
});
```

## Multi-bookmaker behaviour

Every `SportEvent` exposes a `bookmaker()` property (derived from its `id`). The same underlying match reported by two bookmakers is two distinct entries with different `id` and `bookmaker`. Filter:

```java
var ps3838 = client.odds().sportEvents().stream()
    .filter(ev -> ev.bookmaker() == Bookmaker.PS3838)
    .toList();
```

## Reconnect tuning

Default: exponential backoff `1s → 30s`, factor 2, ±30% jitter, unbounded attempts. Override:

```java
var policy = ReconnectPolicy.builder()
    .initialDelayMs(500)
    .maxDelayMs(10_000)
    .maxAttempts(20)
    .build();

var client = RealtimeOddsClient.builder()
    .url(url).apiKey(key).reconnect(policy).build();

client.onError(ev -> {
    if (ev.fatal()) System.err.println("giving up: " + ev.message());
});
```

## Time semantics

- `receivedAt` (on every event payload) — local clock when the SDK received the message. Authoritative for SDK-side latency analysis.
- `quote.timestamp()` / `orderBook.timestamp()` — observation time set by whichever party constructed the object (gateway or SDK at hydration). Approximates freshness; not the bookmaker's authoritative emit time.

## Stability

This is `0.3.2`. The shapes documented above are intended to remain stable through the `0.x` line.

See [`realtimeodds-spec`](https://github.com/Lisandru-2b/realtimeodds-spec) for the wire-format JSON Schemas.

## License

MIT — see [LICENSE](./LICENSE).
