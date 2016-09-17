package io.apiman.plugins.cookie_issue_policy.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.apiman.plugins.session.beans.AbstractCookieConfigBean;

/**
 * Configuration object for the Cookie Issue policy.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class CookieIssueConfigBean extends AbstractCookieConfigBean {
    @JsonProperty
    private Integer apiResponseCode;
    @JsonProperty
    private Boolean cookieSecure;
    @JsonProperty
    private Boolean cookieHttpOnly;

    /**
     * Session validity period in seconds.
     */
    @JsonProperty
    private Integer validityPeriod;
    @JsonProperty
    private String jwtFieldName;
    @JsonProperty
    private String extractClaim;
    @JsonProperty
    private String signingSecret;
    @JsonProperty
    private String requiredAudience;
    @JsonProperty
    private String requiredIssuer;

    public Integer getApiResponseCode() {
        return apiResponseCode;
    }

    public void setApiResponseCode(Integer apiResponseCode) {
        this.apiResponseCode = apiResponseCode;
    }

    public Integer getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(Integer validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    public Boolean getCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(Boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public Boolean getCookieHttpOnly() {
        return cookieHttpOnly;
    }

    public void setCookieHttpOnly(Boolean cookieHttpOnly) {
        this.cookieHttpOnly = cookieHttpOnly;
    }

    public String getJwtFieldName() {
        return jwtFieldName;
    }

    public void setJwtFieldName(String jwtFieldName) {
        this.jwtFieldName = jwtFieldName;
    }

    public String getExtractClaim() {
        return extractClaim;
    }

    public void setExtractClaim(String extractClaim) {
        this.extractClaim = extractClaim;
    }

    public String getSigningSecret() {
        return signingSecret;
    }

    public void setSigningSecret(String signingSecret) {
        this.signingSecret = signingSecret;
    }

    public String getRequiredAudience() {
        return requiredAudience;
    }

    public void setRequiredAudience(String requiredAudience) {
        this.requiredAudience = requiredAudience;
    }

    public String getRequiredIssuer() {
        return requiredIssuer;
    }

    public void setRequiredIssuer(String requiredIssuer) {
        this.requiredIssuer = requiredIssuer;
    }
}
