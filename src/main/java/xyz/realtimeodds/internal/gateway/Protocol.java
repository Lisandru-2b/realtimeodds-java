package xyz.realtimeodds.internal.gateway;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Protocol version compatibility check and close code helpers.
 *
 * <p>Mirrors the contract documented in {@code sb-entities/PROTOCOL.md}.
 */
public final class Protocol {

    private Protocol() {}

    /** The protocol version this SDK speaks. */
    public static final String SDK_PROTOCOL_VERSION = "1.0";

    private static final Pattern VERSION_RE = Pattern.compile("^(\\d+)\\.(\\d+)$");

    public sealed interface VersionCheckResult {}

    public record Compatible() implements VersionCheckResult {}

    public record VersionWarning(String reason) implements VersionCheckResult {}

    public record Incompatible(String reason) implements VersionCheckResult {}

    public static VersionCheckResult checkCompatibility(String serverVersion, String sdkVersion) {
        int[] sdk = parseVersion(sdkVersion);
        if (sdk == null) {
            throw new IllegalArgumentException(
                    "Invalid SDK protocol version " + sdkVersion + " (must be <major>.<minor>)");
        }
        int[] server = parseVersion(serverVersion);
        if (server == null) {
            return new Incompatible("Server sent invalid protocol version " + serverVersion);
        }
        if (server[0] != sdk[0]) {
            return new Incompatible(
                    "Server protocol " + serverVersion + " has a different major than SDK " + sdkVersion);
        }
        if (server[1] > sdk[1]) {
            return new VersionWarning(
                    "Server protocol " + serverVersion + " is newer than SDK " + sdkVersion
                            + "; consider upgrading realtimeodds-java to use new features");
        }
        return new Compatible();
    }

    public static VersionCheckResult checkCompatibility(String serverVersion) {
        return checkCompatibility(serverVersion, SDK_PROTOCOL_VERSION);
    }

    private static int[] parseVersion(String v) {
        Matcher m = VERSION_RE.matcher(v);
        if (!m.matches()) {
            return null;
        }
        return new int[] {Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
    }

    /** 4001 / 4002 / 4003 are fatal auth close codes per the spec. */
    public static boolean isAuthCloseCode(int code) {
        return code == 4001 || code == 4002 || code == 4003;
    }

    public static String authCloseMessage(int code, String reason) {
        String meaning = switch (code) {
            case 4001 -> "missing apiKey";
            case 4002 -> "invalid apiKey";
            case 4003 -> "quota or rate-limit exceeded";
            default -> "auth failed (" + code + ")";
        };
        if (reason == null || reason.isEmpty() || reason.equalsIgnoreCase(meaning)) {
            return meaning;
        }
        return meaning + ": " + reason;
    }
}
