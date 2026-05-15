package xyz.realtimeodds;

/**
 * Factory + builder for {@link Client}.
 *
 * <pre>{@code
 * var client = RealtimeOddsClient.builder()
 *     .url("wss://api.realtimeodds.xyz")
 *     .apiKey(System.getenv("REALTIMEODDS_API_KEY"))
 *     .reconnect(ReconnectPolicy.builder().maxAttempts(20).build())
 *     .build();
 * }</pre>
 */
public final class RealtimeOddsClient {

    private RealtimeOddsClient() {}

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String url;
        private String apiKey;
        private ReconnectPolicy reconnect;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder reconnect(ReconnectPolicy reconnect) {
            this.reconnect = reconnect;
            return this;
        }

        public Client build() {
            return new Client(url, apiKey, reconnect);
        }
    }
}
