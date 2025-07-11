package io.tebex.sdk.request.response;

import io.tebex.sdk.obj.QueuedCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter @AllArgsConstructor
public class OfflineCommandsResponse {
    private final boolean limited;
    private final List<QueuedCommand> commands;
}