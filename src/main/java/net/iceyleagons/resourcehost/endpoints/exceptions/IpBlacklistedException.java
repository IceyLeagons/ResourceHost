package net.iceyleagons.resourcehost.endpoints.exceptions;

import java.util.concurrent.ExecutionException;

/**
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Apr. 19, 2022
 */
public class IpBlacklistedException extends ExecutionException {

    public IpBlacklistedException(String error) {
        super(error);
    }
}
