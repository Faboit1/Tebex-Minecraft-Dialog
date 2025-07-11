package io.tebex.sdk.obj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public enum EnumServerEventType {
    JOIN("server.join"),
    LEAVE("server.leave");

    private final String name;
}
