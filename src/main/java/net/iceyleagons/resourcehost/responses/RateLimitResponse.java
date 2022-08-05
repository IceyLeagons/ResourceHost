package net.iceyleagons.resourcehost.responses;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.iceyleagons.resourcehost.endpoints.exceptions.RateLimitedException;

/**
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Apr. 19, 2022
 */
@Data
@RequiredArgsConstructor
public class RateLimitResponse {

    private final long refill;
    private final String error;

    public static RateLimitResponse from(RateLimitedException e) {
        return new RateLimitResponse(e.getRefill(), e.getMessage());
    }

}
