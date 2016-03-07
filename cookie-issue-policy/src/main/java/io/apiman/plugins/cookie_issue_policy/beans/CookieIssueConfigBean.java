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
    private String apiResponseFieldName;
    @JsonProperty
    private Boolean cookieSecure;
    @JsonProperty
    private Boolean cookieHttpOnly;

    /**
     * Session validity period in seconds.
     */
    @JsonProperty
    private Integer validityPeriod;

    public Integer getApiResponseCode() {
        return apiResponseCode;
    }

    public void setApiResponseCode(Integer apiResponseCode) {
        this.apiResponseCode = apiResponseCode;
    }

    public String getApiResponseFieldName() {
        return apiResponseFieldName;
    }

    public void setApiResponseFieldName(String apiResponseFieldName) {
        this.apiResponseFieldName = apiResponseFieldName;
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
}
