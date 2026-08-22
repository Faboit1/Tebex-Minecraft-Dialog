package io.tebex.sdk.platform.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import lombok.Getter;
import lombok.Setter;

/**
 * The ServerPlatformConfig class holds the configuration for the Tebex SDK on game servers. This is the "full" Tebex configuration.
 */
@Getter @Setter
public class ServerPlatformConfig implements IPlatformConfig {
    private final int configVersion;
    private YamlDocument yamlDocument;


    private String buyCommandName;
    private boolean buyCommandEnabled;
    private boolean checkForUpdates;
    private boolean verbose;
    private boolean proxyMode;
    private String secretKey;

    private boolean autoReportEnabled;
    private int checkIntervalSeconds;

    private String checkoutMessage;
    private String bedrockCheckoutMessage;

    /**
     * Creates a PlatformConfig instance with the provided configuration version.
     *
     * @param configVersion The configuration version.
     */
    public ServerPlatformConfig(int configVersion) {
        this.configVersion = configVersion;
    }

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

    @Override
    public void setSecretKey(String key) {
        this.secretKey = key;
    }

    @Override
    public boolean isVerbose() {
        return verbose;
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
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
