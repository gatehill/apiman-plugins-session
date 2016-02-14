package io.apiman.plugins.session.util;

import io.apiman.plugins.session.model.Session;

/**
 * Utility methods for handling {@link Session}s.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SessionUtil {
    /**
     * Build a new Session for the specified principal, and the given ID and validity period.
     *
     * @param sessionId              the ID of the Session
     * @param authenticatedPrincipal the authenticated principal
     * @param validityPeriod         the session validity period in seconds
     * @return a new Session
     */
    public static Session buildSession(String sessionId, String authenticatedPrincipal, int validityPeriod) {
        final long validityPeriodMillis = (1000 * validityPeriod);
        final long nowMillis = TimeUtil.getNowInMillis();
        final long expiresMillis = (nowMillis + validityPeriodMillis);
        final long absoluteExpiryMillis = (nowMillis + Constants.MAX_SESSION_TTL);

        final Session session = new Session();
        session.setSessionId(sessionId);
        session.setValidityPeriod(validityPeriodMillis);
        session.setStarts(nowMillis);
        session.setExpires(expiresMillis);
        session.setAbsoluteExpiry(absoluteExpiryMillis);
        session.setAuthenticatedPrincipal(authenticatedPrincipal);
        session.setTerminated(false);

        return session;
    }
}
