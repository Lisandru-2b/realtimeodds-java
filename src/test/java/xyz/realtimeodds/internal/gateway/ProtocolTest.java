package xyz.realtimeodds.internal.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProtocolTest {

    @Test
    void compatibleSameVersion() {
        assertThat(Protocol.checkCompatibility("1.0", "1.0"))
                .isInstanceOf(Protocol.Compatible.class);
    }

    @Test
    void warningWhenServerMinorNewer() {
        assertThat(Protocol.checkCompatibility("1.5", "1.0"))
                .isInstanceOf(Protocol.VersionWarning.class);
    }

    @Test
    void compatibleWhenSdkMinorNewer() {
        assertThat(Protocol.checkCompatibility("1.0", "1.5"))
                .isInstanceOf(Protocol.Compatible.class);
    }

    @Test
    void incompatibleWhenMajorDiffers() {
        assertThat(Protocol.checkCompatibility("2.0", "1.0"))
                .isInstanceOf(Protocol.Incompatible.class);
    }

    @Test
    void incompatibleWhenServerVersionInvalid() {
        assertThat(Protocol.checkCompatibility("not-a-version", "1.0"))
                .isInstanceOf(Protocol.Incompatible.class);
    }

    @Test
    void invalidSdkVersionThrows() {
        assertThatThrownBy(() -> Protocol.checkCompatibility("1.0", "bad"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultSdkVersionIsUsed() {
        assertThat(Protocol.checkCompatibility(Protocol.SDK_PROTOCOL_VERSION))
                .isInstanceOf(Protocol.Compatible.class);
    }

    @Test
    void authCloseCodeDetection() {
        assertThat(Protocol.isAuthCloseCode(4001)).isTrue();
        assertThat(Protocol.isAuthCloseCode(4002)).isTrue();
        assertThat(Protocol.isAuthCloseCode(4003)).isTrue();
        assertThat(Protocol.isAuthCloseCode(1006)).isFalse();
    }

    @Test
    void authCloseMessageIncludesMeaning() {
        assertThat(Protocol.authCloseMessage(4001, "")).contains("missing apiKey");
        assertThat(Protocol.authCloseMessage(4002, "")).contains("invalid apiKey");
        assertThat(Protocol.authCloseMessage(4003, "")).contains("quota");
    }
}
