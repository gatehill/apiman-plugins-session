package io.apiman.plugins.cookie_remove_policy.beans;

import io.apiman.plugins.session.beans.AbstractCookieConfigBean;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Configuration object for the Cookie Remove policy.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class CookieRemoveConfigBean extends AbstractCookieConfigBean {

    @JsonProperty
    private Boolean invalidateSession;
    @JsonProperty
    private Boolean skipBackendCall;

    public Boolean getInvalidateSession() {
        return invalidateSession;
    }

    public void setInvalidateSession(Boolean invalidateSession) {
        this.invalidateSession = invalidateSession;
    }

    public Boolean getSkipBackendCall() {
        return skipBackendCall;
    }

    public void setSkipBackendCall(Boolean skipBackendCall) {
        this.skipBackendCall = skipBackendCall;
    }
}
