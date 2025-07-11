package io.tebex.sdk.platform.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import lombok.*;

/**
 * The ProxyPlatformConfig class holds the configuration for the Tebex SDK on proxy platforms (Bungee, Velocity)
 */
@Getter @Setter @RequiredArgsConstructor @ToString
public class ProxyPlatformConfig implements IPlatformConfig {
    private final int configVersion;

    private YamlDocument yamlDocument;
    private boolean verbose;
    private String secretKey;
    private boolean autoReportEnabled;

    /**
     * Returns the configuration version.
     *
     * @return The configuration version.
     */
    @Override
    public int getConfigVersion() {
        return configVersion;
    }

    /**
     * Returns the secret key.
     *
     * @return The secret key.
     */
    @Override
    public String getSecretKey() {
        return secretKey;
    }

    public boolean isAutoReportEnabled() { return autoReportEnabled; }

    @Override
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Returns the YAML document for this configuration.
     *
     * @return The YAML document.
     */
    @Override
    public YamlDocument getYamlDocument() {
        return yamlDocument;
    }
}
