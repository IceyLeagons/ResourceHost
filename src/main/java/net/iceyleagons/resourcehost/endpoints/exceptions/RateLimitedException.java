package net.iceyleagons.resourcehost.endpoints.exceptions;

import lombok.Getter;

import java.util.concurrent.ExecutionException;

/**
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Apr. 19, 2022
 */
@Getter
public class RateLimitedException extends ExecutionException {

    private final long refill;

    public RateLimitedException(String error, long refill) {
        super(error);
        this.refill = refill;
    }
}
