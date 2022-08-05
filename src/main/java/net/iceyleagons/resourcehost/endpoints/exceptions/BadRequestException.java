package net.iceyleagons.resourcehost.endpoints.exceptions;

import java.util.concurrent.ExecutionException;

/**
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Apr. 19, 2022
 */
public class BadRequestException extends ExecutionException {

    public BadRequestException(String error) {
        super(error);
    }
}
