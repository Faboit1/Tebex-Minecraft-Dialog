package io.tebex.sdk.obj;

import com.google.gson.JsonObject;
import io.tebex.sdk.util.UUIDUtil;
import lombok.Data;

@Data
public class QueuedPlayer {
    private final int id;
    private final String name;
    private final String uuid;
    private final String xuid;

    /**
     * Constructs a Player instance.
     *
     * @param id The Tebex player ID.
     * @param name The player name.
     * @param uuid The player UUID. If truncated, is transformed into java-style uuid ("00000000-0000-0000-etc...")
     */
    public QueuedPlayer(int id, String name, String uuid) {
        this(id, name, uuid, null);
    }

    public QueuedPlayer(int id, String name, String uuid, String xuid) {
        this.id = id;
        this.name = name;
        this.uuid = normalizeUuid(uuid);
        this.xuid = normalizeXuid(xuid);
    }

    public static QueuedPlayer fromJson(JsonObject object) {
        String rawUuid = object.has("uuid") && !object.get("uuid").isJsonNull()
                ? object.get("uuid").getAsString()
                : null;
        String rawXuid = object.has("xuid") && !object.get("xuid").isJsonNull()
                ? object.get("xuid").getAsString()
                : null;

        if (rawXuid == null && looksLikeXuid(rawUuid)) {
            rawXuid = rawUuid;
            rawUuid = null;
        }

        return new QueuedPlayer(
                object.get("id").getAsInt(),
                object.get("name").getAsString(),
                rawUuid,
                rawXuid
        );
    }

    public boolean hasUuid() {
        return uuid != null && !UUIDUtil.EMPTY_UUID.toString().equals(uuid);
    }

    public boolean hasXuid() {
        return xuid != null && !xuid.isEmpty();
    }

    public Long getXuidAsLong() {
        if (!hasXuid()) {
            return null;
        }

        try {
            if (xuid.matches("\\d+")) {
                return Long.parseUnsignedLong(xuid);
            }

            if (xuid.matches("(?i)[0-9a-f]{16}")) {
                return Long.parseUnsignedLong(xuid, 16);
            }
        } catch (NumberFormatException ignored) {
            return null;
        }

        return null;
    }

    public String getDefaultCommandIdentifier() {
        return hasUuid() ? uuid : (name == null ? "" : name);
    }

    private static String normalizeUuid(String uuid) {
        if (isBlank(uuid) || looksLikeXuid(uuid)) {
            return null;
        }

        return UUIDUtil.mojangIdToJavaId(uuid.trim()).toString();
    }

    private static String normalizeXuid(String xuid) {
        if (isBlank(xuid)) {
            return null;
        }

        return xuid.trim();
    }

    private static boolean looksLikeXuid(String identifier) {
        if (isBlank(identifier)) {
            return false;
        }

        String trimmed = identifier.trim();
        return trimmed.matches("\\d+") || trimmed.matches("(?i)[0-9a-f]{16}");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty() || value.trim().equalsIgnoreCase("null");
    }
}
