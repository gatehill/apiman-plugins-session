package io.apiman.plugins.session.exception;

import io.apiman.gateway.engine.beans.exceptions.AbstractEngineException;

/**
 * Thrown by misconfigured policies.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class InvalidConfigurationException extends AbstractEngineException {
    public InvalidConfigurationException(String message) {
        super(message);
    }
}
