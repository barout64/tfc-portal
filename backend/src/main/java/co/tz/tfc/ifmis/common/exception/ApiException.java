package co.tz.tfc.ifmis.common.exception;

import org.springframework.http.HttpStatus;

/** Any portal-level business rule failure that should surface a clear message to the caller. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
