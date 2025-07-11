package io.tebex.sdk.request.response;

import io.tebex.sdk.obj.QueuedPlayer;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter @AllArgsConstructor
public class DuePlayersResponse {
    private final boolean executeOffline;
    private final int nextCheck;
    private final boolean more;
    private final List<QueuedPlayer> players;
}