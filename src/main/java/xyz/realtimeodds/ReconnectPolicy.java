package xyz.realtimeodds;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Exponential-backoff reconnect policy. Defaults to {@code 1s → 30s}, factor 2,
 * ±30% jitter, unbounded attempts.
 *
 * <pre>{@code
 * var policy = ReconnectPolicy.builder()
 *     .initialDelayMs(500)
 *     .maxDelayMs(10_000)
 *     .maxAttempts(20)
 *     .build();
 * }</pre>
 */
public final class ReconnectPolicy {

    private final long initialDelayMs;
    private final long maxDelayMs;
    private final double factor;
    private final double jitter;
    private final int maxAttempts;

    private ReconnectPolicy(Builder b) {
        this.initialDelayMs = b.initialDelayMs;
        this.maxDelayMs = b.maxDelayMs;
        this.factor = b.factor;
        this.jitter = b.jitter;
        this.maxAttempts = b.maxAttempts;
    }

    public static ReconnectPolicy defaults() {
        return new Builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public long initialDelayMs() {
        return initialDelayMs;
    }

    public long maxDelayMs() {
        return maxDelayMs;
    }

    public double factor() {
        return factor;
    }

    public double jitter() {
        return jitter;
    }

    /** {@link Integer#MAX_VALUE} means unbounded. */
    public int maxAttempts() {
        return maxAttempts;
    }

    /** Compute the next delay in ms for {@code attempt} (1-based). */
    public long computeDelayMs(int attempt) {
        double base = Math.min(initialDelayMs * Math.pow(factor, attempt - 1), maxDelayMs);
        double jitterRange = base * jitter;
        double delay = base - jitterRange + ThreadLocalRandom.current().nextDouble() * 2 * jitterRange;
        return (long) Math.max(0, delay);
    }

    public static final class Builder {
        private long initialDelayMs = 1000;
        private long maxDelayMs = 30_000;
        private double factor = 2.0;
        private double jitter = 0.3;
        private int maxAttempts = Integer.MAX_VALUE;

        public Builder initialDelayMs(long v) {
            this.initialDelayMs = v;
            return this;
        }

        public Builder maxDelayMs(long v) {
            this.maxDelayMs = v;
            return this;
        }

        public Builder factor(double v) {
            this.factor = v;
            return this;
        }

        public Builder jitter(double v) {
            this.jitter = v;
            return this;
        }

        public Builder maxAttempts(int v) {
            this.maxAttempts = v;
            return this;
        }

        public ReconnectPolicy build() {
            return new ReconnectPolicy(this);
        }
    }
}
