package io.tebex.sdk.request.response;

import io.tebex.sdk.util.Pagination;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter @AllArgsConstructor @ToString
public class PaginatedResponse<T> {
    private final Pagination pagination;
    private final List<T> data;
}