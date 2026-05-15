# realtimeodds-java

Real-time betting odds SDK for Java — multi-bookmaker, sport-discriminated, async with `CompletableFuture`.

The SDK is a **strict replica** of the gateway's internal stores: same shapes, same fields, same getters and read-side methods. Discriminated unions implemented as Java 17 `sealed interface` + `record`, so consumer code can `switch` exhaustively with full type narrowing.

> Status: 0.1.0 — early. Stable through the `0.x` line.

## Install

This SDK is distributed via [JitPack](https://jitpack.io). Add the JitPack repository and the dependency to your build.

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.Lisandru-2b:realtimeodds-java:0.1.0")
}
```

### Gradle (Groovy)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.Lisandru-2b:realtimeodds-java:0.1.0'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.Lisandru-2b</groupId>
    <artifactId>realtimeodds-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

Requires Java 17+.

## Quickstart

```java
import xyz.realtimeodds.RealtimeOddsClient;

public class Quickstart {
    public static void main(String[] args) throws Exception {
        var client = RealtimeOddsClient.builder()
            .url("wss://api.realtimeodds.xyz")
            .apiKey(System.getenv("REALTIMEODDS_API_KEY"))
            .build();

        client.onConnected(ev -> System.out.println("connected"));
        client.onSportEventAdded(ev -> {
            var se = ev.sportEvent();
            if (se.sport() == xyz.realtimeodds.entities.Sport.BASKETBALL) {
                System.out.println(se.name() + " (" + se.bookmaker() + ")");
            }
        });
        client.onOddsChanged(ev ->
            System.out.println(ev.bookmaker() + " " + ev.selectionId() + " -> " + ev.quote().price()));

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
| `client.disconnect()` | Close and stop reconnecting. Idempotent. Fails any in-flight `connect()`. |
| `client.snapshot()` | Returns `Snapshot(sportEvents: Map<String, SportEvent>, stale: boolean)`. |
| `client.getSportEvent(id)` | Single lookup by id. Returns `null` if unknown. |
| `client.onXxx(cb)` / `client.off(event, cb)` | Subscribe / unsubscribe. Sync callbacks. |
| `client.connectionState()` | `ConnectionState(status, lastError)`. |

### Events

Synchronous callbacks (`Consumer<T>`). Each event payload is a `record`.

| Event | Payload |
|---|---|
| `connected` | `ConnectedEvent` (singleton) |
| `disconnected` | `DisconnectedEvent(willReconnect, code, reason)` |
| `reconnecting` | `ReconnectingEvent(attempt, delayMs)` |
| `error` | `ErrorEvent(message, fatal)` |
| `sportEvent:added` | `SportEventAddedEvent(sportEvent, receivedAt)` |
| `sportEvent:updated` | `SportEventUpdatedEvent(sportEvent, receivedAt)` — fires on metadata change OR on any odds change |
| `sportEvent:removed` | `SportEventRemovedEvent(bookmaker, sportEventId, receivedAt)` |
| `odds:changed` | `OddsChangedEvent(bookmaker, sportEventId, marketId, selectionId, quote, receivedAt)` |
| `source:cleared` | `SourceClearedEvent(bookmaker, receivedAt)` — a bookmaker source went away |
| `resync` | `ResyncEvent(bookmaker, reason, sportEvents, receivedAt)` — full atomic state replacement |

Close codes 4001/4002/4003 are fatal auth codes; `fatal=true` errors stop the client.

## Entities

Frozen `record` types implementing `sealed interface` unions. Branch with `switch` or `instanceof` for exhaustive type narrowing.

- **`SportEvent`** (`BasketballMatch | FootballMatch | TennisMatch`): `id`, `kind`, `bookmaker`, `sport`, `competition`, `sportRegion`, `startDate` (Java `OffsetDateTime`), `matchUrl`, `name`, `markets: Map`, plus `getMarket(id)`, `getSelection(id)`.
- **`Market`** (6 variants discriminated by `kind`): `id`, `kind`, `selectionKind`, `isSynthetic`, `bookmaker`, `marketName`, `sportEventName`, `sport`, `category`, `isAvailable`, `isFullyAvailable`, `numberOfPossibleResults`, `selections: Map`, plus `getSelection(id)`, `getSelectionByResult(result)`, `getFairOdd(result)`, `calculateMargin()`, etc.
- **`Selection`**: `id`, `kind`, `result`, `quote`, `orderBook`, `bookmaker`, `isAvailable`, `price()` (throws if unavailable).
- **`Quote`**: `price`, `size`, `timestamp`, `impliedProbability()`.
- **`OrderBook`**: `bids`, `asks`, `timestamp`, `bestBid()`, `bestAsk()`, `spread()`, `midPrice()`, `availableSizeUpTo(maxPrice)`.

Sport-specific fields (`homeTeam`/`awayTeam`/`competitor1`/`competitor2`/`period`/`handicap`/`scope`/`cut`/`playerName`/`propType`) live on the relevant subtypes.

## Sport / kind narrowing

```java
client.onSportEventAdded(ev -> {
    var se = ev.sportEvent();
    switch (se) {
        case BasketballMatch b -> System.out.println(b.homeTeam() + " vs " + b.awayTeam());
        case FootballMatch f -> System.out.println(f.homeTeam() + " vs " + f.awayTeam());
        case TennisMatch t -> System.out.println(t.competitor1() + " vs " + t.competitor2());
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
var ps3838 = client.snapshot().sportEvents().values().stream()
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

This is `0.1.0`. The shapes documented above are intended to remain stable through the `0.x` line.

See [`realtimeodds-spec`](https://github.com/Lisandru-2b/realtimeodds-spec) for the wire-format JSON Schemas.

## License

MIT — see [LICENSE](./LICENSE).
