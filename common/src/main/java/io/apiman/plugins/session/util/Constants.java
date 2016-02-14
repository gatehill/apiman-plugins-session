package io.apiman.plugins.session.util;

/**
 * Shared constants.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class Constants {
    /**
     * 12 hours in milliseconds.
     * TODO: make configurable
     */
    public static final long MAX_SESSION_TTL = 43200000;

    /**
     * Don't give clues about authentication failures to clients.
     */
    public static final String GENERIC_AUTH_FAILURE = "Unauthorized";

    /**
     * HTTP request header name for cookie.
     */
    public static final String HEADER_COOKIE = "Cookie";

    /**
     * HTTP response header name for cookie.
     */
    public static final String HEADER_SET_COOKIE = "Set-Cookie";

    /**
     * HTTP response header name for content length.
     */
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";

    /**
     * HTTP response header name for redirect location.
     */
    public static final String HEADER_LOCATION = "Location";

    private Constants() {
    }
}
