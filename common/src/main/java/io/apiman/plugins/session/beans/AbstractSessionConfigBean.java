package io.apiman.plugins.session.beans;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Common configuration object for the session policies.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class AbstractSessionConfigBean {

    /**
     * A regular expression indicating the path(s) to which this policy should be applied.
     */
    @JsonProperty
    private String pathMatcher;

    public String getPathMatcher() {
        return pathMatcher;
    }

    public void setPathMatcher(String pathMatcher) {
        this.pathMatcher = pathMatcher;
    }
}
