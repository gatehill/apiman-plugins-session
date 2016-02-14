package io.apiman.plugins.session.util;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.plugins.session.model.Cookie;
import org.apache.commons.lang.StringUtils;

/**
 * Utility methods for handling cookies.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class CookieUtil {
    private static final String COOKIE_PATH = "Path";
    private static final String COOKIE_MAX_AGE = "Max-Age";
    private static final String COOKIE_DOMAIN = "Domain";
    private static final String COOKIE_SECURE = "Secure";
    private static final String COOKIE_HTTP_ONLY = "HttpOnly";

    /**
     * Adds the given Cookie to the response.
     *
     * @param response the service response
     * @param cookie   the cookie to set
     */
    public static void addResponseCookie(ApiResponse response, Cookie cookie) {
        setResponseCookie(response, cookie);
    }

    /**
     * Add the 'Set-Cookie' header with the value of the given Cookie.
     *
     * @param response the service response
     * @param cookie   the cookie to set
     */
    private static void setResponseCookie(ApiResponse response, Cookie cookie) {
        final StringBuilder sb = new StringBuilder();

        // name and value
        sb.append(cookie.getName()).append("=").append(cookie.getValue()).append(";");

        // optional
        if (StringUtils.isNotBlank(cookie.getPath())) {
            sb.append(" ").append(COOKIE_PATH).append("=").append(cookie.getPath()).append(";");
        }
        if (null != cookie.getMaxAge()) {
            sb.append(" ").append(COOKIE_MAX_AGE).append("=").append(cookie.getMaxAge()).append(";");
        }
        if (null != cookie.getDomain()) {
            sb.append(" ").append(COOKIE_DOMAIN).append("=").append(cookie.getDomain()).append(";");
        }
        if (null != cookie.getSecure()) {
            sb.append(" ").append(COOKIE_SECURE).append(";");
        }
        if (null != cookie.getHttpOnly()) {
            sb.append(" ").append(COOKIE_HTTP_ONLY).append(";");
        }

        response.getHeaders().put(Constants.HEADER_SET_COOKIE, sb.toString());
    }

    /**
     * Parse the 'Cookie' request header to extract the value of a cookie with the given name.
     *
     * @param request    the service request
     * @param cookieName the name of the cookie to extract
     * @return a Cookie containing the value of the named cookie, or <code>null</code> if not found
     */
    public static Cookie getCookie(ApiRequest request, String cookieName) {
        final String headerValue = request.getHeaders().get(Constants.HEADER_COOKIE);
        if (StringUtils.isNotBlank(headerValue)) {

            for (String keyValuePair : headerValue.split(";")) {
                final String[] cookie = keyValuePair.split("=");
                if (2 == cookie.length) {
                    // build a Cookie with the given name and value
                    final String trimmedCookieName = cookie[0].trim();
                    if (cookieName.equalsIgnoreCase(trimmedCookieName)) {
                        return new Cookie(trimmedCookieName, cookie[1].trim());
                    }
                }
            }

        }
        return null;
    }

    /**
     * Removes the cookie with the given name.
     *
     * @param response the service response]
     * @param cookie   the cookie to remove
     */
    public static void removeCookie(ApiResponse response, Cookie cookie) {
        // belt and braces
        cookie.setValue("");

        /*
         * A zero value causes the cookie to be deleted by the browser.
         * Note: Max-Age is supported on most browsers except IE <= 8.
         * In unsupported browsers, the cookie will behave like a session cookie.
         */
        cookie.setMaxAge(0);
        setResponseCookie(response, cookie);
    }

    /**
     * Parse the 'Set-Cookie' response header to extract the cookie.
     *
     * @param cookieHeader the response header
     * @return a Cookie
     */
    public static Cookie parseResponseCookie(String cookieHeader) {
        final Cookie cookie = new Cookie();

        final String[] properties = cookieHeader.split(";");
        for (int i = 0; i < properties.length; i++) {
            final String property = properties[i].trim();

            if (0 == i) {
                // first is name=value pair
                final KeyAndValue kv = getKeyAndValue(property);
                cookie.setName(kv.name);
                cookie.setValue(kv.value);

            } else {
                // match optional properties
                if (property.startsWith(COOKIE_PATH)) {
                    cookie.setPath(getKeyAndValue(property).value);
                } else if (property.startsWith(COOKIE_MAX_AGE)) {
                    cookie.setMaxAge(Integer.valueOf(getKeyAndValue(property).value));
                } else if (property.startsWith(COOKIE_DOMAIN)) {
                    cookie.setDomain(getKeyAndValue(property).value);
                } else if (property.equals(COOKIE_SECURE)) {
                    cookie.setSecure(true);
                } else if (property.equals(COOKIE_HTTP_ONLY)) {
                    cookie.setHttpOnly(true);
                }
            }
        }

        return cookie;
    }

    /**
     * Splits a value like this:
     * <literal>
     * myKey=myValue
     * </literal>
     * into its key and value.
     *
     * @param propertyToSplit the property to split
     * @return the key and value
     */
    private static KeyAndValue getKeyAndValue(String propertyToSplit) {
        final String[] keyAndValue = propertyToSplit.split("=");
        switch (keyAndValue.length) {
            case 2:
                return new KeyAndValue(keyAndValue[0].trim(), keyAndValue[1].trim());
            case 1:
                return new KeyAndValue(keyAndValue[0].trim(), null);
            default:
                throw new RuntimeException(String.format("Property '%s' not in format key=value", propertyToSplit));
        }
    }

    /***
     * Holds the key and value
     */
    private static class KeyAndValue {
        final String name;
        final String value;

        private KeyAndValue(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
}
