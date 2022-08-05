package net.iceyleagons.resourcehost.endpoints.exceptions;

import java.util.concurrent.ExecutionException;

/**
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Aug. 05, 2022
 */
public class UnsupportedMediaTypeException extends ExecutionException {

    public UnsupportedMediaTypeException(String error) {
        super(error);
    }
}
