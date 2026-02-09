package io.tebex.sdk.obj;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class QueuedCommandTest {

    @Test
    void offlinePlayersFallbackToNameForIdAndUuid() {
        QueuedPlayer offlinePlayer = new QueuedPlayer(1, "Notch", null);
        QueuedCommand command = new QueuedCommand(
                10,
                "say {username} {id} {uuid}",
                0,
                0,
                0,
                0,
                offlinePlayer,
                false
        );

        String parsed = command.getParsedCommand();

        assertEquals("say Notch Notch Notch", parsed);
        assertFalse(parsed.contains("00000000-0000-0000-0000-000000000000"));
    }

    @Test
    void stringNullUuidAlsoFallsBackToName() {
        QueuedPlayer offlinePlayer = new QueuedPlayer(2, "Alex", "null");
        QueuedCommand command = new QueuedCommand(
                11,
                "grant {id} {uuid}",
                0,
                0,
                0,
                0,
                offlinePlayer,
                false
        );

        String parsed = command.getParsedCommand();

        assertEquals("grant Alex Alex", parsed);
    }

    @Test
    void onlinePlayersKeepUuid() {
        String rawMojangUuid = "123456781234123412341234567890ab";
        QueuedPlayer onlinePlayer = new QueuedPlayer(3, "Steve", rawMojangUuid);
        QueuedCommand command = new QueuedCommand(
                12,
                "lp user {id} parent add default",
                0,
                0,
                0,
                0,
                onlinePlayer,
                true
        );

        String parsed = command.getParsedCommand();

        assertEquals("lp user 12345678-1234-1234-1234-1234567890ab parent add default", parsed);
    }
}
