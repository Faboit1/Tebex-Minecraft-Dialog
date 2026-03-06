package io.tebex.sdk.placeholder.defaults;

import io.tebex.sdk.obj.QueuedPlayer;
import io.tebex.sdk.placeholder.PlaceholderManager;
import io.tebex.sdk.util.UUIDUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UuidPlaceholderTest {

    @Test
    void onlyReplacesIdAndUuidWhenUuidMissing() {
        PlaceholderManager placeholderManager = new PlaceholderManager();
        UuidPlaceholder placeholder = new UuidPlaceholder(placeholderManager);
        QueuedPlayer player = new QueuedPlayer(1, "Notch", null);

        String parsed = placeholder.handle(player, "cmd {username} {name} {player} {id} {uuid}");

        assertEquals("cmd {username} {name} {player} Notch Notch", parsed);
    }

    @Test
    void onlyReplacesIdAndUuidWhenUuidPresent() {
        PlaceholderManager placeholderManager = new PlaceholderManager();
        UuidPlaceholder placeholder = new UuidPlaceholder(placeholderManager);
        String mojangUuid = "123456781234123412341234567890ab";
        QueuedPlayer player = new QueuedPlayer(2, "Steve", mojangUuid);

        String parsed = placeholder.handle(player, "cmd {username} {name} {player} {id} {uuid}");

        String javaUuid = UUIDUtil.mojangIdToJavaId(mojangUuid).toString();
        assertEquals("cmd {username} {name} {player} " + javaUuid + " " + javaUuid, parsed);
    }
}
