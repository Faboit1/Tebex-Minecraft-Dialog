package io.tebex.sdk.obj;

import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ServerEvent {
    @SerializedName("username_id")
    private final String uuid;

    private final String username;
    private final String ip;

    @SerializedName("event_type")
    private final String eventType;

    @SerializedName("event_date")
    private final String eventDate;

    public ServerEvent(String uuid, String username, String ip, ServerEventType eventType) {
        this.uuid = uuid;
        this.username = username;
        this.ip = anonymizeIp(ip);
        this.eventType = eventType.getName();
        this.eventDate = Instant.now()
            .atZone(ZoneId.of("UTC"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"));
    }

    /**
     * Anonymizes the last octet in a given IP.
     *
     * @param ipIn The full IP address, ex. 192.168.1.100
     * @return An anonymized IP, ex. 192.168.1.x
     */
    private String anonymizeIp(String ipIn) {
        int lastOctetStart = ipIn.lastIndexOf(".");
        if (lastOctetStart == -1) {
            return ipIn;
        }
        return ipIn.substring(0, lastOctetStart) + ".x";
    }

    public String getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public String getIp() {
        return ip;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventDate() {
        return eventDate;
    }

    @Override
    public String toString() {
        return "ServerEvent{" +
                "uuid='" + uuid + '\'' +
                ", username='" + username + '\'' +
                ", ip='" + ip + '\'' +
                ", eventType=" + eventType +
                ", eventDate='" + eventDate + '\'' +
                '}';
    }
}
