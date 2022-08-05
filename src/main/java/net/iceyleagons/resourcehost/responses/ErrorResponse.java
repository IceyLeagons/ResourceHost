package net.iceyleagons.resourcehost.responses;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ExecutionException;

/**
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Apr. 19, 2022
 */
@Data
@RequiredArgsConstructor
public class ErrorResponse {

    private final String error;

    public static ErrorResponse from(ExecutionException e) {
        return new ErrorResponse(e.getMessage());
    }
}
