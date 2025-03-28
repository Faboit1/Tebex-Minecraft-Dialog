package io.tebex.sdk.platform.config;

import dev.dejvokep.boostedyaml.YamlDocument;

/**
 * The base PlatformConfig class holds the configuration for the Tebex SDK.
 */
public interface IPlatformConfig {
    int getConfigVersion();
    String getSecretKey();
    void setSecretKey(String key);
    boolean isAutoReportEnabled();
    boolean isVerbose();
    void setVerbose(boolean verbose);
    YamlDocument getYamlDocument();
}
