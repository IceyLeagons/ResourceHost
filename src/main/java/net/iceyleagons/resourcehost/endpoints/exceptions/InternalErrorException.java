package net.iceyleagons.resourcehost.endpoints.exceptions;

import java.util.concurrent.ExecutionException;

/**
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Apr. 19, 2022
 */
public class InternalErrorException extends ExecutionException {

    public InternalErrorException() {
        super("Internal Server Error");
    }
}
