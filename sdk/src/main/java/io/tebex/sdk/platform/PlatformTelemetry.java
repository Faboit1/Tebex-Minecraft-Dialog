package io.tebex.sdk.platform;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * The PlatformTelemetry class contains information about the server platform
 * and environment, such as server software, plugin version, and Java version.
 */
@Getter @AllArgsConstructor @ToString
public class PlatformTelemetry {
    private final String pluginVersion;
    private final String serverSoftware;
    private final String serverVersion;
    private final String javaVersion;
    private final String systemArch;
    private final boolean onlineMode;
}