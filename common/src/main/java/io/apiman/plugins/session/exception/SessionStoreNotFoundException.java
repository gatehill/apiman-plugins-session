package io.apiman.plugins.session.exception;

/**
 * Thrown when the session store implementation is not found.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SessionStoreNotFoundException extends RuntimeException {
    public SessionStoreNotFoundException(String sessionStoreImpl, Throwable cause) {
        super("ISessionStore implementation not found: " + sessionStoreImpl, cause);
    }
}
