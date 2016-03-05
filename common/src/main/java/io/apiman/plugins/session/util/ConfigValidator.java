package io.apiman.plugins.session.util;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Chaining config validator.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ConfigValidator {
    private final List<String> validationErrors = new ArrayList<>();

    public static ConfigValidator build() {
        return new ConfigValidator();
    }

    public ConfigValidator validate(String validationErrorMessage, Object nullableObj) {
        if (null == nullableObj) {
            validationErrors.add(validationErrorMessage);
        }
        return this;
    }

    public ConfigValidator validate(String validationErrorMessage, String nullableString) {
        if (StringUtils.isBlank(nullableString)) {
            validationErrors.add(validationErrorMessage);
        }
        return this;
    }

    public ConfigValidator validate(String validationErrorMessage, Validator validator) {
        if (!validator.isValid()) {
            validationErrors.add(validationErrorMessage);
        }
        return this;
    }

    /**
     * @return <code>true</code> if the ConfigValidator contains errors, otherwise <code>false</code>
     */
    public boolean isValid() {
        return (0 == validationErrors.size());
    }

    /**
     * @return the arrays held by the ConfigValidator
     */
    public String[] getValidationErrors() {
        return validationErrors.toArray(new String[validationErrors.size()]);
    }

    /**
     * Holds custom validation logic.
     */
    public interface Validator {
        boolean isValid();
    }
}
