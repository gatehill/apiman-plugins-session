package io.apiman.plugins.session.model;

import java.io.Serializable;

/**
 * Represents a session.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class Session implements Serializable {
    private String sessionId;
    private String authenticatedPrincipal;
    private long starts;
    private long expires;
    private boolean terminated;
    private long validityPeriod;
    private long absoluteExpiry;

    @Override
    public String toString() {
        return "Session{" + "sessionId='" + sessionId + '\'' +
                ", authenticatedPrincipal='" + authenticatedPrincipal + '\'' +
                ", starts=" + starts +
                ", expires=" + expires +
                ", terminated=" + terminated +
                ", validityPeriod=" + validityPeriod +
                ", absoluteExpiry=" + absoluteExpiry +
                '}';
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAuthenticatedPrincipal() {
        return authenticatedPrincipal;
    }

    public void setAuthenticatedPrincipal(String authenticatedPrincipal) {
        this.authenticatedPrincipal = authenticatedPrincipal;
    }

    public long getStarts() {
        return starts;
    }

    public void setStarts(long starts) {
        this.starts = starts;
    }

    public long getExpires() {
        return expires;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    public void setValidityPeriod(long validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    public long getValidityPeriod() {
        return validityPeriod;
    }

    public void setAbsoluteExpiry(long absoluteExpiry) {
        this.absoluteExpiry = absoluteExpiry;
    }

    public long getAbsoluteExpiry() {
        return absoluteExpiry;
    }
}
