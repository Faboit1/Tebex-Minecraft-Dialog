package io.tebex.sdk.obj;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueuedPlayerTest {

    @Test
    void fromJsonTreatsNumericUuidAsXuid() {
        JsonObject playerJson = JsonParser.parseString("{\"id\":1,\"name\":\"BedrockUser\",\"uuid\":\"281474976710655\"}")
                .getAsJsonObject();

        QueuedPlayer player = QueuedPlayer.fromJson(playerJson);

        assertNull(player.getUuid());
        assertEquals("281474976710655", player.getXuid());
        assertEquals(Long.valueOf(281474976710655L), player.getXuidAsLong());
        assertFalse(player.hasUuid());
        assertTrue(player.hasXuid());
    }

    @Test
    void fromJsonKeepsExplicitJavaUuidAndXuid() {
        JsonObject playerJson = JsonParser.parseString("{\"id\":2,\"name\":\"LinkedUser\",\"uuid\":\"123456781234123412341234567890ab\",\"xuid\":\"281474976710655\"}")
                .getAsJsonObject();

        QueuedPlayer player = QueuedPlayer.fromJson(playerJson);

        assertEquals("12345678-1234-1234-1234-1234567890ab", player.getUuid());
        assertEquals("281474976710655", player.getXuid());
        assertTrue(player.hasUuid());
        assertTrue(player.hasXuid());
    }
}
