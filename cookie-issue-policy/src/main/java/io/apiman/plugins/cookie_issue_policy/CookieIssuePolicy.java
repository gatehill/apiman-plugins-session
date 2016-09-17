package io.apiman.plugins.cookie_issue_policy;

import com.auth0.jwt.JWTVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.beans.PolicyFailure;
import io.apiman.gateway.engine.beans.PolicyFailureType;
import io.apiman.gateway.engine.beans.exceptions.ConfigurationParseException;
import io.apiman.gateway.engine.components.IBufferFactoryComponent;
import io.apiman.gateway.engine.io.AbstractStream;
import io.apiman.gateway.engine.io.IApimanBuffer;
import io.apiman.gateway.engine.io.IReadWriteStream;
import io.apiman.gateway.engine.policies.AbstractMappedDataPolicy;
import io.apiman.gateway.engine.policies.AbstractMappedPolicy;
import io.apiman.gateway.engine.policy.IPolicyChain;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.cookie_issue_policy.beans.CookieIssueConfigBean;
import io.apiman.plugins.session.beans.ResponseBehaviour;
import io.apiman.plugins.session.exception.InvalidConfigurationException;
import io.apiman.plugins.session.model.Cookie;
import io.apiman.plugins.session.model.Session;
import io.apiman.plugins.session.store.ISessionStore;
import io.apiman.plugins.session.store.SessionStoreFactory;
import io.apiman.plugins.session.util.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Policy that issues a cookie in the response.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class CookieIssuePolicy extends AbstractMappedDataPolicy<CookieIssueConfigBean> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CookieIssuePolicy.class);
    private static final Messages MESSAGES = new Messages("io.apiman.plugins.cookie_issue_policy", "CookieIssuePolicy");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ATTRIBUTE_SESSION_ID = CookieIssuePolicy.class.getCanonicalName() + ".sessionId";
    private static final String ATTRIBUTE_SKIP = CookieIssuePolicy.class.getCanonicalName() + ".skipPolicy";

    private Pattern pathMatcher;

    /**
     * See {@link AbstractMappedPolicy#getConfigurationClass()}
     */
    @Override
    protected Class<CookieIssueConfigBean> getConfigurationClass() {
        return CookieIssueConfigBean.class;
    }

    /**
     * See {@link AbstractMappedPolicy#parseConfiguration(String)}
     */
    @Override
    public CookieIssueConfigBean parseConfiguration(String jsonConfiguration) throws ConfigurationParseException {
        final CookieIssueConfigBean config = super.parseConfiguration(jsonConfiguration);

        // validate configuration
        final ConfigValidator validator = ConfigValidator.build()
                .validate("API response code", config.getApiResponseCode())
                .validate("Cookie name", config.getCookieName())
                .validate("Session validity period", config.getValidityPeriod())
                .validate("Response behaviour", config.getResponseBehaviour())
                .validate("Path matcher", config.getPathMatcher())
                .validate("API response JWT field name", config.getJwtFieldName())
                .validate("JWT signing secret", config.getSigningSecret())
                .validate("Redirect URL", () -> {
                    // redirect URL should be set
                    return (ResponseBehaviour.PassThrough.equals(config.getResponseBehaviour()) ||
                            StringUtils.isNotBlank(config.getRedirectUrl()));
                });

        if (!validator.isValid()) {
            throw new InvalidConfigurationException(MESSAGES.formatEach(
                    "ConfigNotSet", validator.getValidationErrors()));
        }

        // precompile path matcher for performance
        pathMatcher = Pattern.compile(config.getPathMatcher());

        return config;
    }

    /**
     * See {@link AbstractMappedPolicy#doApply(ApiRequest, IPolicyContext, Object, IPolicyChain)}
     */
    @Override
    protected void doApply(ApiRequest request, IPolicyContext context, CookieIssueConfigBean config,
                           IPolicyChain<ApiRequest> chain) {

        // skip if path matcher is not use, or request URL doesn't match
        if (null == pathMatcher || !pathMatcher.matcher(request.getDestination()).matches()) {
            context.setAttribute(ATTRIBUTE_SKIP, true);
        }

        chain.doApply(request);
    }

    /**
     * See {@link AbstractMappedPolicy#doApply(ApiResponse, IPolicyContext, Object, IPolicyChain)}
     */
    @Override
    protected void doApply(ApiResponse response, IPolicyContext context, final CookieIssueConfigBean config,
                           IPolicyChain<ApiResponse> chain) {

        // short-circuit
        if (context.getAttribute(ATTRIBUTE_SKIP, false)) {
            LOGGER.debug(MESSAGES.format("PathMatchFalse"));
            chain.doApply(response);
            return;
        }

        // validate the API response code
        if (response.getCode() != config.getApiResponseCode()) {
            LOGGER.warn(MESSAGES.format("ApiResponseCodeInvalid",
                    response.getCode(), config.getApiResponseCode()));

            chain.doFailure(new PolicyFailure(PolicyFailureType.Authentication, HttpURLConnection.HTTP_UNAUTHORIZED,
                    Constants.GENERIC_AUTH_FAILURE));

            return;
        }

        final Cookie cookie = generateCookie(config);

        // remember ID
        final String sessionId = cookie.getValue();
        context.setAttribute(ATTRIBUTE_SESSION_ID, sessionId);

        // set the response cookie
        CookieUtil.addResponseCookie(response, cookie);

        LOGGER.info(MESSAGES.format("ApiResponseCodeValid",
                config.getApiResponseCode(), config.getCookieName(), sessionId));

        if (ResponseBehaviour.Redirect.equals(config.getResponseBehaviour())) {
            LOGGER.info(MESSAGES.format("Redirecting", config.getRedirectUrl()));

            // remove existing Content-Length header before continuing chain, as API response will not be returned
            response.getHeaders().remove(Constants.HEADER_CONTENT_LENGTH);

            // redirect client
            response.setCode(HttpURLConnection.HTTP_MOVED_TEMP);
            response.getHeaders().put(Constants.HEADER_LOCATION, config.getRedirectUrl());
        }

        // continue the chain
        chain.doApply(response);
    }

    /**
     * See {@link AbstractMappedDataPolicy#requestDataHandler(ApiRequest, IPolicyContext, Object)}
     */
    @Override
    protected IReadWriteStream<ApiRequest> requestDataHandler(ApiRequest request, IPolicyContext context,
                                                              CookieIssueConfigBean config) {
        return null;
    }

    /**
     * See {@link AbstractMappedDataPolicy#responseDataHandler(ApiResponse, IPolicyContext, Object)}
     */
    @Override
    protected IReadWriteStream<ApiResponse> responseDataHandler(final ApiResponse response, final IPolicyContext context,
                                                                final CookieIssueConfigBean config) {

        // short-circuit
        if (context.getAttribute(ATTRIBUTE_SKIP, false)) {
            return null;
        }

        final IBufferFactoryComponent bufferFactory = context.getComponent(IBufferFactoryComponent.class);
        final int contentLength = response.getHeaders().containsKey(Constants.HEADER_CONTENT_LENGTH)
                ? Integer.parseInt(response.getHeaders().get(Constants.HEADER_CONTENT_LENGTH))
                : 0;

        // read API response into a buffer, to be parsed later
        return new AbstractStream<ApiResponse>() {
            private IApimanBuffer readBuffer = bufferFactory.createBuffer(contentLength);

            @Override
            protected void handleHead(ApiResponse head) {
            }

            @Override
            public ApiResponse getHead() {
                return response;
            }

            /**
             * Read API response into a buffer, to be parsed in {@link #end()}.
             * @param chunk a chunk from the API response
             */
            @Override
            public void write(IApimanBuffer chunk) {
                readBuffer.append(chunk);
            }

            /**
             * Parse the {@link #readBuffer}.
             */
            @Override
            public void end() {

                final String sessionId = context.getAttribute(ATTRIBUTE_SESSION_ID, null);

                if (StringUtils.isBlank(sessionId)) {
                    LOGGER.error(MESSAGES.format("SessionIdNull"));

                } else {
                    try {
                        // fail-safe
                        boolean sessionValid = false;
                        try {
                            if (readBuffer.length() > 0) {
                                final String authenticatedPrincipal = parseApiResponseBody(config, readBuffer.getBytes());
                                if (StringUtils.isNotBlank(authenticatedPrincipal)) {
                                    storeSessionData(context, config, sessionId, authenticatedPrincipal);
                                    sessionValid = true;
                                }

                                if (ResponseBehaviour.PassThrough.equals(config.getResponseBehaviour())) {
                                    // API response will be returned unchanged
                                    super.write(readBuffer);
                                }

                            } else {
                                LOGGER.warn(MESSAGES.format("ApiResponseBodyEmpty"));
                            }

                        } finally {
                            if (!sessionValid) {
                                invalidateSession(sessionId, context);
                            }
                        }

                    } catch (Exception e) {
                        LOGGER.error(MESSAGES.format("ErrorProcessingApiResponseBody"), e);
                    }
                }

                super.end();
            }
        };
    }

    /**
     * Generate a new Cookie from the config.
     *
     * @param config the policy configuration
     * @return a Cookie
     */
    private static Cookie generateCookie(CookieIssueConfigBean config) {
        final Cookie cookie = new Cookie(config.getCookieName(), UUID.randomUUID().toString());
        cookie.setPath(config.getCookiePath());
        cookie.setSecure(config.getCookieSecure());
        cookie.setHttpOnly(config.getCookieHttpOnly());
        return cookie;
    }

    /**
     * Store session information.
     *
     * @param context                the policy context
     * @param config                 the policy configuration
     * @param sessionId              the ID of the session generated in {@link #generateCookie(CookieIssueConfigBean)}
     * @param authenticatedPrincipal the authenticated principal extracted from the API response
     */
    private static void storeSessionData(IPolicyContext context, CookieIssueConfigBean config, final String sessionId,
                                         String authenticatedPrincipal) {

        // build and store a new session
        final Session sessionData = SessionUtil.buildSession(sessionId, authenticatedPrincipal, config.getValidityPeriod());
        LOGGER.debug(MESSAGES.format("StoringSessionData", sessionId, sessionData));

        final ISessionStore sessionStore = SessionStoreFactory.getSessionStore(context);
        sessionStore.storeSession(sessionId, sessionData, result -> {
            if (result.isSuccess()) {
                LOGGER.info(MESSAGES.format(
                        "StoredSessionData", sessionId, sessionData));

            } else {
                LOGGER.error(MESSAGES.format(
                        "ErrorStoringSessionData", sessionId, sessionData), result.getError());
            }
        });
    }

    /**
     * Invalidate session data for the given session.
     *
     * @param sessionId the ID of the session
     * @param context   the policy context
     */
    private void invalidateSession(final String sessionId, final IPolicyContext context) {
        final ISessionStore sessionStore = SessionStoreFactory.getSessionStore(context);
        sessionStore.deleteSession(sessionId, result -> {
            if (result.isSuccess()) {
                // session data removed
                LOGGER.info(MESSAGES.format("SessionInvalidated", sessionId));

            } else {
                // failed to remove session data
                final String failureMessage = MESSAGES.format("SessionInvalidationFailed", sessionId);
                LOGGER.error(failureMessage, result.getError());
            }
        });
    }

    /**
     * Parse the API response body and extract the desired field.
     *
     * @param config          the policy configuration
     * @param apiResponseBody the API response body
     * @return the authenticated principal extracted from the API response
     */
    @SuppressWarnings("unchecked")
    private String parseApiResponseBody(CookieIssueConfigBean config, byte[] apiResponseBody) {
        // sanity check
        if (null == apiResponseBody || 0 == apiResponseBody.length) {
            LOGGER.warn(MESSAGES.format("ApiResponseBodyEmpty"));

        } else {
            final String body = new String(apiResponseBody);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(MESSAGES.format("ParsingApiResponseBody", body));
            }

            final Map<String, Object> bodyJson;
            try {
                // extract JWT from response
                bodyJson = MAPPER.readValue(body, HashMap.class);

            } catch (IOException e) {
                throw new RuntimeException(MESSAGES.format("ErrorParsingApiResponseBody", body), e);
            }

            final String jwt = (String) bodyJson.get(config.getJwtFieldName());
            if (StringUtils.isBlank(jwt)) {
                LOGGER.error(MESSAGES.format("JwtFieldNull", config.getJwtFieldName()));

            } else {
                final String claimValue = validateJwt(config, jwt);

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(MESSAGES.format("ExtractedClaim", config.getExtractClaim(), claimValue));
                }

                return claimValue;
            }
        }

        // default to empty authenticated principal
        return null;
    }

    /**
     * Perform validation on the JWT, and extract the authentication information.
     *
     * @param config the policy configuration
     * @param jwt    the JWT
     * @return the authentication information
     */
    private String validateJwt(CookieIssueConfigBean config, String jwt) {
        try {
            final String secret = config.getSigningSecret();

            final JWTVerifier verifier;
            if (StringUtils.isNotBlank(config.getRequiredAudience()) && StringUtils.isNotBlank(config.getRequiredIssuer())) {
                verifier = new JWTVerifier(secret, config.getRequiredAudience(), config.getRequiredIssuer());
            } else if (StringUtils.isNotBlank(config.getRequiredAudience())) {
                verifier = new JWTVerifier(secret, config.getRequiredAudience());
            } else {
                verifier = new JWTVerifier(secret);
            }

            final Map<String, Object> claims = verifier.verify(jwt);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("JWT verification successful - claims: ", claims);
            } else {
                LOGGER.debug("JWT verification successful - {} claims present", claims.size());
            }

            final String auth;
            if (StringUtils.isNotBlank(config.getExtractClaim())) {
                LOGGER.trace("Using claim '{}' from JWT for authentication information");
                auth = (String) claims.get(config.getExtractClaim());
            } else {
                LOGGER.trace("Using entire JWT for authentication information");
                auth = jwt;
            }
            return auth;

        } catch (Exception e) {
            // Invalid Token
            throw new RuntimeException("JWT verification failed", e);
        }
    }
}
