package io.apiman.plugins.cookie_issue_policy;

import io.apiman.gateway.engine.beans.PolicyFailureType;
import io.apiman.plugins.cookie_issue_policy.backend.LoginBackEndApi;
import io.apiman.plugins.session.exception.InvalidConfigurationException;
import io.apiman.plugins.session.model.Cookie;
import io.apiman.plugins.session.model.Session;
import io.apiman.plugins.session.test.CommonTestUtil;
import io.apiman.plugins.session.util.Constants;
import io.apiman.plugins.session.util.CookieUtil;
import io.apiman.plugins.session.util.TimeUtil;
import io.apiman.test.policies.*;
import org.junit.Test;

import java.net.HttpURLConnection;

import static org.junit.Assert.*;

/**
 * Tests for {@link CookieIssuePolicy}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@SuppressWarnings("nls")
@TestingPolicy(CookieIssuePolicy.class)
public class CookieIssuePolicyTest extends ApimanPolicyTest {

    private static final String EMPTY_CONFIG = "{}";
    private static final String RESOURCE = "/some/resource";

    /**
     * Expects that a Session is created in shared state and that a Cookie is set on the response upon
     * successful authentication with the back-end service.
     *
     * @throws Throwable
     */
    @Test
    @Configuration(classpathConfigFile = "standard-config.json")
    @BackEndApi(LoginBackEndApi.class)
    public void testLoginSuccess() throws Throwable {
        final PolicyTestRequest request = PolicyTestRequest.build(PolicyTestRequestType.GET, RESOURCE);
        request.body(LoginBackEndApi.SUCCESSFUL_REQUEST_BODY);

        final PolicyTestResponse response = send(request);
        assertEquals(HttpURLConnection.HTTP_MOVED_TEMP, response.code());

        // no content should be returned - instead the policy is configured to redirect to another location
        assertNull(response.header(Constants.HEADER_CONTENT_LENGTH));
        assertEquals("", response.body());
        assertEquals("/another/url", response.header(Constants.HEADER_LOCATION));

        // verify the cookie was set
        final String cookieHeader = response.header(Constants.HEADER_SET_COOKIE);
        assertNotNull(cookieHeader);

        final Cookie cookie = CookieUtil.parseResponseCookie(cookieHeader);
        assertEquals(CommonTestUtil.COOKIE_NAME, cookie.getName());

        final String sessionId = cookie.getValue();
        assertNotNull(sessionId);

        // verify the session data in the shared state
        final Session session = CommonTestUtil.fetchSession(sessionId);
        assertNotNull(session);
        assertEquals(sessionId, session.getSessionId());
        assertEquals(CommonTestUtil.AUTHENTICATED_PRINICPAL, session.getAuthenticatedPrincipal());

        // verify times
        assertTrue(session.getStarts() <= TimeUtil.getNowInMillis());
        assertTrue(TimeUtil.isAfterNow(session.getExpires()));
        assertTrue(TimeUtil.isAfterNow(session.getAbsoluteExpiry()));
        assertEquals(120000, session.getValidityPeriod());
    }

    /**
     * Expects that a Cookie is not set on the response upon unsuccessful authentication with the back-end service,
     * resulting in a PolicyFailureError.
     *
     * @throws Throwable
     */
    @Test
    @Configuration(classpathConfigFile = "standard-config.json")
    @BackEndApi(LoginBackEndApi.class)
    public void testLoginFailureInvalidResponseCode() throws Throwable {
        final PolicyTestRequest request = PolicyTestRequest.build(PolicyTestRequestType.GET, RESOURCE);
        request.body("Login failure request body");

        try {
            send(request);
            fail(PolicyFailureError.class + " expected");

        } catch (PolicyFailureError failure) {
            assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, failure.getFailure().getFailureCode());
            assertEquals(PolicyFailureType.Authentication, failure.getFailure().getType());

            // cookie should not be set
            assertNull(failure.getFailure().getHeaders().get(Constants.HEADER_SET_COOKIE));
        }
    }

    /**
     * Expects that a ConfigurationException is thrown as the policy is not configured correctly.
     *
     * @throws Throwable
     */
    @Test(expected = InvalidConfigurationException.class)
    @Configuration(EMPTY_CONFIG)
    @BackEndApi(LoginBackEndApi.class)
    public void testLoginExceptionEmptyConfig() throws Throwable {
        final PolicyTestRequest request = PolicyTestRequest.build(PolicyTestRequestType.GET, RESOURCE);
        request.body(LoginBackEndApi.SUCCESSFUL_REQUEST_BODY);

        send(request);
        fail(InvalidConfigurationException.class + " expected");
    }
}
