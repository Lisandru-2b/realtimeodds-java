package xyz.realtimeodds;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReconnectPolicyTest {

    @Test
    void defaultPolicyGrowsExponentially() {
        ReconnectPolicy policy = ReconnectPolicy.builder().jitter(0.0).build();
        assertThat(policy.computeDelayMs(1)).isEqualTo(1000);
        assertThat(policy.computeDelayMs(2)).isEqualTo(2000);
        assertThat(policy.computeDelayMs(3)).isEqualTo(4000);
        assertThat(policy.computeDelayMs(4)).isEqualTo(8000);
    }

    @Test
    void cappedAtMaxDelay() {
        ReconnectPolicy policy = ReconnectPolicy.builder().jitter(0.0).maxDelayMs(5000).build();
        // Without cap, attempt 4 would be 8000ms.
        assertThat(policy.computeDelayMs(4)).isEqualTo(5000);
        assertThat(policy.computeDelayMs(10)).isEqualTo(5000);
    }

    @Test
    void jitterStaysWithinRange() {
        ReconnectPolicy policy = ReconnectPolicy.builder().jitter(0.3).build();
        for (int i = 0; i < 50; i++) {
            long delay = policy.computeDelayMs(1);
            assertThat(delay).isBetween(699L, 1301L); // ±30% of 1000ms
        }
    }

    @Test
    void firstAttemptReturnsInitialDelay() {
        ReconnectPolicy policy = ReconnectPolicy.builder().jitter(0.0).initialDelayMs(1234).build();
        assertThat(policy.computeDelayMs(1)).isEqualTo(1234);
    }
}
