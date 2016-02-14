package io.apiman.plugins.cookie_validate_policy.model;

/**
 * The result of validating the session.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ValidationResult {
    private final boolean success;
    private final String message;

    public ValidationResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
