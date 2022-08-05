package net.iceyleagons.resourcehost.responses;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Apr. 19, 2022
 */
@Data
@RequiredArgsConstructor
public class ExistsResponse {

    private final String downloadUrl;
    private final boolean exists;
    private final long available;
    private final long remainingTokens;

    public static ExistsResponse found(String downloadUrl, long available, long remainingTokens) {
        return new ExistsResponse(downloadUrl, true, available, remainingTokens);
    }

    public static ExistsResponse empty(long remainingTokens) {
        return new ExistsResponse("", false, -1, remainingTokens);
    }
}
