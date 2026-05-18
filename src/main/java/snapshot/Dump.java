package snapshot;

// Template — copy into realtimeodds-java/src/main/java/snapshot/Dump.java
// and add the `application` plugin to build.gradle.kts with mainClass=snapshot.Dump.
// Run: ./gradlew run --args="--api-key <key> --output snapshot.json --wait 2"

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import xyz.realtimeodds.Client;
import xyz.realtimeodds.OddsBook;
import xyz.realtimeodds.RealtimeOddsClient;
import xyz.realtimeodds.entities.Market;
import xyz.realtimeodds.entities.Selection;
import xyz.realtimeodds.entities.SportEvent;

public final class Dump {

    public static void main(String[] args) throws Exception {
        String url = "wss://api.realtimeodds.xyz/ws";
        String apiKey = System.getenv("REALTIMEODDS_API_KEY");
        // Default output: <workspace>/tmp/snapshot.json, assuming this is
        // launched from the realtimeodds-java repo (user.dir = repo). If you
        // run from elsewhere, pass --output explicitly.
        String output = Path.of(System.getProperty("user.dir"), "..", "tmp", "snapshot.json")
                .normalize()
                .toString();
        double wait = 2.0;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--url" -> url = args[++i];
                case "--api-key" -> apiKey = args[++i];
                case "--output" -> output = args[++i];
                case "--wait" -> wait = Double.parseDouble(args[++i]);
            }
        }
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("--api-key (or $REALTIMEODDS_API_KEY) is required");
            System.exit(2);
        }

        Client client = RealtimeOddsClient.builder().url(url).apiKey(apiKey).build();
        client.onError(e -> {
            if (e.fatal()) {
                System.err.println("fatal: " + e.message());
                System.exit(3);
            }
        });

        client.connect().orTimeout(15, java.util.concurrent.TimeUnit.SECONDS).join();
        Thread.sleep((long) (wait * 1000));

        OddsBook book = client.snapshot();
        List<Map<String, Object>> payload = new ArrayList<>();
        for (SportEvent ev : book) {
            payload.add(serializeSportEvent(ev));
        }

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        byte[] json = mapper.writeValueAsBytes(payload);
        Path outPath = Path.of(output).toAbsolutePath();
        Files.createDirectories(outPath.getParent() == null ? Path.of(".") : outPath.getParent());
        Files.write(outPath, json);

        System.out.printf("wrote %d sport events to %s (%,d bytes)%n",
                book.size(), outPath, json.length);

        client.disconnect().join();
    }

    private static Map<String, Object> serializeSportEvent(SportEvent ev) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", ev.id());
        m.put("bookmaker", ev.bookmaker().value());
        m.put("sport", ev.sport().value());
        m.put("kind", ev.kind().value());
        m.put("competition", ev.competition());
        m.put("sportRegion", ev.sportRegion());
        m.put("startDate", ev.startDate());
        m.put("matchUrl", ev.matchUrl());
        m.put("name", ev.name());
        List<Map<String, Object>> markets = new ArrayList<>();
        for (Market mk : ev.markets().values()) {
            markets.add(serializeMarket(mk));
        }
        m.put("markets", markets);
        return m;
    }

    private static Map<String, Object> serializeMarket(Market mk) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", mk.id());
        m.put("kind", mk.kind().value());
        m.put("selectionKind", mk.selectionKind().value());
        m.put("isSynthetic", mk.isSynthetic());
        List<Map<String, Object>> sels = new ArrayList<>();
        for (Selection s : mk.selections().values()) {
            sels.add(serializeSelection(s));
        }
        m.put("selections", sels);
        return m;
    }

    private static Map<String, Object> serializeSelection(Selection s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.id());
        m.put("kind", s.kind().value());
        m.put("result", s.result().value());
        if (s.quote() != null) {
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("price", s.quote().price());
            q.put("size", s.quote().size());
            q.put("timestamp", s.quote().timestamp());
            m.put("quote", q);
        } else {
            m.put("quote", null);
        }
        // orderBook intentionally omitted: large + CLOB-only.
        return m;
    }
}
