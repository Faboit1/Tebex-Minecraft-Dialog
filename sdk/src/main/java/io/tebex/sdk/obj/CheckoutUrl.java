package io.tebex.sdk.obj;

import lombok.Data;
import java.util.Date;

@Data
public class CheckoutUrl {
    private final String url;
    private final Date expires;
}
