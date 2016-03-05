package io.apiman.plugins.cookie_validate_policy;

import io.apiman.gateway.engine.async.IAsyncResult;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.beans.PolicyFailure;
import io.apiman.gateway.engine.beans.PolicyFailureType;
import io.apiman.gateway.engine.beans.exceptions.ConfigurationParseException;
import io.apiman.gateway.engine.policies.AbstractMappedPolicy;
import io.apiman.gateway.engine.policy.IPolicyChain;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.cookie_validate_policy.model.ValidationResult;
import io.apiman.plugins.cookie_validate_policy.beans.CookieValidateConfigBean;
import io.apiman.plugins.session.beans.ValidationType;
import io.apiman.plugins.session.exception.InvalidConfigurationException;
import io.apiman.plugins.session.model.Cookie;
import io.apiman.plugins.session.model.Session;
import io.apiman.plugins.session.store.ISessionStore;
import io.apiman.plugins.session.store.SessionStoreFactory;
import io.apiman.plugins.session.util.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.util.regex.Pattern;

/**
 * Policy that validates a cookie in the request.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class CookieValidatePolicy extends AbstractMappedPolicy<CookieValidateConfigBean> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CookieValidatePolicy.class);
    private static final Messages MESSAGES = Messages.getMessageBundle(CookieValidatePolicy.class);
    private static final String ATTRIBUTE_SKIP = CookieValidatePolicy.class.getCanonicalName() + ".skipPolicy";

    private Pattern pathMatcher;

    /**
     * See {@link AbstractMappedPolicy#getConfigurationClass()}
     */
    @Override
    protected Class<CookieValidateConfigBean> getConfigurationClass() {
        return CookieValidateConfigBean.class;
    }

    /**
     * See {@link AbstractMappedPolicy#parseConfiguration(String)}
     */
    @Override
    public CookieValidateConfigBean parseConfiguration(String jsonConfiguration) throws ConfigurationParseException {
        final CookieValidateConfigBean config = super.parseConfiguration(jsonConfiguration);

        // validate configuration
        final ConfigValidator validator = ConfigValidator.build()
                .validate(config.getValidationType(), "Validation type")
                .validate(new ConfigValidator.Validator() {
                    @Override
                    public boolean isValid() {
                        // cookie name should be set
                        return (ValidationType.NoValidation.equals(config.getValidationType()) ||
                                StringUtils.isNotBlank(config.getCookieName()));
                    }
                }, "Cookie name");

        if (!validator.isValid()) {
            throw new InvalidConfigurationException(MESSAGES.formatEach(
                    "CookieValidatePolicy.ConfigNotSet", validator.getValidationErrors()));
        }

        // configure path matcher
        if (StringUtils.isNotBlank(config.getPathMatcher())) {
            pathMatcher = Pattern.compile(config.getPathMatcher());
        } else {
            LOGGER.warn(MESSAGES.format("CookieValidatePolicy.PathMatcherNotSet"));
        }

        return config;
    }

    /**
     * See {@link AbstractMappedPolicy#doApply(ApiResponse, IPolicyContext, Object, IPolicyChain)}
     */
    @Override
    protected void doApply(ApiRequest request, IPolicyContext context, CookieValidateConfigBean config,
                           IPolicyChain<ApiRequest> chain) {

        // skip if path matcher is not use, or request URL doesn't match
        if (null == pathMatcher || !pathMatcher.matcher(request.getDestination()).matches()) {
            LOGGER.debug(MESSAGES.format("CookieValidatePolicy.PathMatchFalse"));

            context.setAttribute(ATTRIBUTE_SKIP, true);
            chain.doApply(request);
            return;
        }

        // check validation
        final ValidationType validationType = config.getValidationType();
        if (ValidationType.NoValidation.equals(validationType)) {
            // no validation - continue request to back-end
            LOGGER.debug(MESSAGES.format("CookieValidatePolicy.NoValidation"));
            chain.doApply(request);

        } else if (ValidationType.ValidationRequired.equals(validationType) || ValidationType.ValidationOptional.equals(validationType)) {
            // validate the session
            LOGGER.debug(MESSAGES.format("CookieValidatePolicy.AttemptingValidation"));

            final Cookie cookie = CookieUtil.getCookie(request, config.getCookieName());
            if (null != cookie && !StringUtils.isEmpty(cookie.getValue())) {
                // the cookie value is the session ID
                validateSession(request, context, config, chain, cookie.getValue(), validationType);

            } else {
                if (ValidationType.ValidationOptional.equals(validationType)) {
                    // permit absent cookie - continue request to back-end
                    LOGGER.info(MESSAGES.format("CookieValidatePolicy.ValidationOptional.CookieAbsent", config.getCookieName()));
                    chain.doApply(request);

                } else {
                    // policy failure - cookie is absent or empty
                    LOGGER.warn(MESSAGES.format("CookieValidatePolicy.ValidationRequired.CookieAbsent", config.getCookieName()));
                    chain.doFailure(new PolicyFailure(PolicyFailureType.Authentication,
                            HttpURLConnection.HTTP_UNAUTHORIZED, Constants.GENERIC_AUTH_FAILURE));
                }
            }

        } else {
            chain.throwError(new UnsupportedOperationException(
                    MESSAGES.format("CookieValidatePolicy.UnsupportedValidationType", validationType)));
        }
    }

    /**
     * Validate the session with the given ID.
     *
     * @param request        the service request
     * @param context        the policy context
     * @param config         the cookie validator configuration bean
     * @param chain          the policy chain
     * @param sessionId      the session ID
     * @param validationType the type of validation required
     */
    protected void validateSession(final ApiRequest request, final IPolicyContext context,
                                   final CookieValidateConfigBean config, final IPolicyChain<ApiRequest> chain,
                                   final String sessionId, final ValidationType validationType) {

        // look up the session by its ID
        final ISessionStore sessionStore = SessionStoreFactory.getSessionStore(context);
        sessionStore.fetchSession(sessionId, new IAsyncResultHandler<Session>() {
            @Override
            public void handle(IAsyncResult<Session> result) {
                final ValidationResult validationResult = verifyResult(result, sessionId, request, context, config);

                if (validationResult.isSuccess()) {
                    // valid session - continue request to back-end
                    LOGGER.info(validationResult.getMessage());
                    chain.doApply(request);

                } else {
                    if (ValidationType.ValidationOptional.equals(validationType)) {
                        // permit invalid session - continue request to back-end
                        LOGGER.info(MESSAGES.format("CookieValidatePolicy.ValidationOptional.IgnoreInvalidSession",
                                validationResult.getMessage()));

                        chain.doApply(request);

                    } else {
                        // 401 as session invalid or not found
                        LOGGER.warn(validationResult.getMessage());

                        // return a generic error message - don't tell the client why the failure occurred
                        chain.doFailure(new PolicyFailure(PolicyFailureType.Authentication,
                                HttpURLConnection.HTTP_UNAUTHORIZED, Constants.GENERIC_AUTH_FAILURE));
                    }
                }
            }
        });
    }

    /**
     * Verify the session data.
     *
     * @param result    the result of retrieving session data
     * @param sessionId the ID of the session
     * @param request   the service request
     * @param context   the policy context
     * @param config    the policy configuration
     * @return the result of the validation
     */
    private ValidationResult verifyResult(IAsyncResult<Session> result, String sessionId, ApiRequest request,
                                          IPolicyContext context, CookieValidateConfigBean config) {

        final ValidationResult validationResult;
        final Session sessionData = result.getResult();

        if (result.isSuccess() && null != sessionData && StringUtils.isNotBlank(sessionData.getSessionId())) {
            validationResult = verifySessionData(sessionData, sessionId, request, context, config);

        } else {
            //noinspection ThrowableResultOfMethodCallIgnored
            if (null != result.getError()) {
                LOGGER.error(MESSAGES.format("CookieValidatePolicy.ErrorReadingSessionData", sessionId), result.getError());
            }

            // session not present
            validationResult = new ValidationResult(false,
                    MESSAGES.format("CookieValidatePolicy.MissingSessionData", sessionId));
        }

        return validationResult;
    }

    /**
     * Verify the session data and extend the session if it is valid.
     *
     * @param sessionData the session to validate
     * @param sessionId   the ID of the session
     * @param request     the service request
     * @param context     the policy context
     * @param config      the policy configuration
     */
    private ValidationResult verifySessionData(Session sessionData, String sessionId, ApiRequest request,
                                               IPolicyContext context, CookieValidateConfigBean config) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(MESSAGES.format("CookieValidatePolicy.ValidatingSession", sessionData));
        }

        if (sessionId.equals(sessionData.getSessionId())) {
            if (!sessionData.isTerminated()) {
                if (TimeUtil.isAfterNow(sessionData.getExpires())) {
                    if (TimeUtil.isAfterNow(sessionData.getAbsoluteExpiry())) {
                        // session is valid - update session data
                        extendSession(context, sessionData);

                        // set the authenticated principal as a header in the request passed on to the API
                        request.getHeaders().put(config.getAuthHeaderName(), sessionData.getAuthenticatedPrincipal());

                        return new ValidationResult(true,
                                MESSAGES.format("CookieValidatePolicy.CookieValidationSucceededSessionValid", sessionId));

                    } else {
                        return new ValidationResult(false,
                                MESSAGES.format("CookieValidatePolicy.SessionExceededAbsoluteExpiry", sessionId));
                    }

                } else {
                    return new ValidationResult(false,
                            MESSAGES.format("CookieValidatePolicy.SessionExpired", sessionId));
                }

            } else {
                return new ValidationResult(false,
                        MESSAGES.format("CookieValidatePolicy.SessionTerminated", sessionId));
            }

        } else {
            return new ValidationResult(false,
                    MESSAGES.format("CookieValidatePolicy.SessionIdMismatch", sessionId));
        }
    }

    /**
     * Extend the session with a new expiry time.
     *
     * @param context     the policy context
     * @param sessionData the session to extend
     */
    private void extendSession(final IPolicyContext context, final Session sessionData) {
        final long newExpiry = (TimeUtil.getNowInMillis() + sessionData.getValidityPeriod());
        sessionData.setExpires(newExpiry);

        LOGGER.debug(MESSAGES.format("CookieValidatePolicy.ExtendingSession", sessionData.getSessionId(), newExpiry));

        // store updated session data
        final ISessionStore sessionStore = SessionStoreFactory.getSessionStore(context);
        sessionStore.storeSession(sessionData.getSessionId(), sessionData, new IAsyncResultHandler<Void>() {
            @Override
            public void handle(IAsyncResult<Void> result) {
                if (result.isSuccess()) {
                    LOGGER.info(MESSAGES.format(
                            "CookieValidatePolicy.UpdatedSessionData", sessionData.getSessionId(), sessionData));
                } else {
                    LOGGER.error(MESSAGES.format("CookieValidatePolicy.ErrorUpdatingSessionData",
                            sessionData.getSessionId(), sessionData), result.getError());
                }
            }
        });
    }
}
