package io.tebex.sdk.placeholder.defaults;

import io.tebex.sdk.obj.QueuedPlayer;
import io.tebex.sdk.placeholder.Placeholder;
import io.tebex.sdk.placeholder.PlaceholderManager;
import io.tebex.sdk.util.UUIDUtil;

import static io.tebex.sdk.util.UUIDUtil.EMPTY_UUID;

public class UuidPlaceholder implements Placeholder {
    private final PlaceholderManager placeholderManager;

    public UuidPlaceholder(PlaceholderManager placeholderManager) {
        this.placeholderManager = placeholderManager;
    }

    @Override
    public String handle(QueuedPlayer player, String command) {
        if (player.getUuid() == null || player.getUuid().equals("null") || player.getUuid().equals(EMPTY_UUID.toString())) {
            return placeholderManager.getUniqueIdRegex().matcher(command).replaceAll(player.getName());
        }
        return placeholderManager.getUniqueIdRegex().matcher(command).replaceAll(UUIDUtil.mojangIdToJavaId(player.getUuid()).toString());
    }
}
