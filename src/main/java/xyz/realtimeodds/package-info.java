/**
 * realtimeodds — Real-time betting odds SDK for Java.
 *
 * <p>Multi-bookmaker, sport-discriminated, async with {@link java.util.concurrent.CompletableFuture}.
 * Wraps the realtimeodds gateway WebSocket and exposes spec-compliant events to consumers.
 *
 * <p>Quickstart:
 * <pre>{@code
 * var client = RealtimeOddsClient.builder()
 *     .url("wss://api.realtimeodds.xyz")
 *     .apiKey(System.getenv("REALTIMEODDS_API_KEY"))
 *     .build();
 *
 * client.onSportEventAdded(ev -> System.out.println(ev.sportEvent().name()));
 * client.onOddsChanged(ev -> System.out.println(ev.bookmaker() + " " + ev.quote().price()));
 *
 * client.connect().join();
 * Thread.sleep(60_000);
 * client.disconnect().join();
 * }</pre>
 */
package xyz.realtimeodds;
