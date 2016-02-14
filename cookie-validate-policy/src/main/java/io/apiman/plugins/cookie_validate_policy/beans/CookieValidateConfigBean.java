package io.apiman.plugins.cookie_validate_policy.beans;

import io.apiman.plugins.session.beans.AbstractSessionConfigBean;
import io.apiman.plugins.session.beans.ValidationType;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Configuration object for the Cookie Validator policy.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class CookieValidateConfigBean extends AbstractSessionConfigBean {

    @JsonProperty
    private ValidationType validationType;
    @JsonProperty
    private String cookieName;
    @JsonProperty
    private String authHeaderName;

    /**
     * @return the type of validation to perform
     */
    public ValidationType getValidationType() {
        return validationType;
    }

    /**
     * @param validationType the type of validation to perform
     */
    public void setValidationType(ValidationType validationType) {
        this.validationType = validationType;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public String getAuthHeaderName() {
        return authHeaderName;
    }

    public void setAuthHeaderName(String authHeaderName) {
        this.authHeaderName = authHeaderName;
    }
}
