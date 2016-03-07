package io.apiman.plugins.session.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Common configuration object for the cookie policies.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class AbstractCookieConfigBean extends AbstractSessionConfigBean {

    @JsonProperty
    private String cookieName;
    @JsonProperty
    private String cookiePath;

    /**
     * Whether to pass through a successful authentication response or redirect the caller to another location.
     */
    @JsonProperty
    private ResponseBehaviour responseBehaviour;

    /**
     * The location to redirect the user to on successful policy execution.
     */
    @JsonProperty
    private String redirectUrl;

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public String getCookiePath() {
        return cookiePath;
    }

    public void setCookiePath(String cookiePath) {
        this.cookiePath = cookiePath;
    }

    public ResponseBehaviour getResponseBehaviour() {
        return responseBehaviour;
    }

    public void setResponseBehaviour(ResponseBehaviour responseBehaviour) {
        this.responseBehaviour = responseBehaviour;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
}
