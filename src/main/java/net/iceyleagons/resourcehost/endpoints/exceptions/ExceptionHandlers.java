package net.iceyleagons.resourcehost.endpoints.exceptions;

import net.iceyleagons.resourcehost.responses.ErrorResponse;
import net.iceyleagons.resourcehost.responses.RateLimitResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Apr. 19, 2022
 */
@ControllerAdvice
public class ExceptionHandlers {

    @ResponseBody
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    @ExceptionHandler(PayloadTooLargeException.class)
    public ErrorResponse handleException(PayloadTooLargeException e) {
        return ErrorResponse.from(e);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    @ExceptionHandler(UnsupportedMediaTypeException.class)
    public ErrorResponse handleException(UnsupportedMediaTypeException e) {
        return ErrorResponse.from(e);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BadRequestException.class)
    public ErrorResponse handleException(BadRequestException e) {
        return ErrorResponse.from(e);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(IpBlacklistedException.class)
    public ErrorResponse handleException(IpBlacklistedException e) {
        return ErrorResponse.from(e);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ExceptionHandler(RateLimitedException.class)
    public RateLimitResponse handleException(RateLimitedException e) {
        return RateLimitResponse.from(e);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(InternalErrorException.class)
    public ErrorResponse handleException(InternalErrorException e) {
        return ErrorResponse.from(e);
    }
}
