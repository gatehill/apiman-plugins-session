package io.apiman.plugins.cookie_validate_policy;

import io.apiman.gateway.engine.beans.PolicyFailureType;
import io.apiman.plugins.cookie_validate_policy.backend.RequiresAuthHeaderBackEndApi;
import io.apiman.plugins.session.exception.InvalidConfigurationException;
import io.apiman.plugins.session.model.Session;
import io.apiman.plugins.session.test.CommonTestUtil;
import io.apiman.plugins.session.util.Constants;
import io.apiman.plugins.session.util.TimeUtil;
import io.apiman.test.policies.*;
import org.junit.Test;

import java.net.HttpURLConnection;

import static org.junit.Assert.*;

/**
 * Tests for {@link CookieValidatePolicy}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@SuppressWarnings("nls")
@TestingPolicy(CookieValidatePolicy.class)
public class CookieValidatePolicyTest extends ApimanPolicyTest {
    private static final String EMPTY_CONFIG = "{}";
    private static final String RESOURCE = "/some/resource";

    /**
     * Send the request and expect a 401 Unauthorized response, and for session data to remain unchanged.
     *
     * @param request         the service request
     * @param originalSession the Session state before the request is made
     * @throws Throwable
     */
    private void sendAndExpect401(PolicyTestRequest request, Session originalSession) throws Throwable {
        try {
            send(request);
            fail(PolicyFailureError.class + " expected");

        } catch (PolicyFailureError failure) {
            assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, failure.getFailure().getFailureCode());
            assertEquals(PolicyFailureType.Authentication, failure.getFailure().getType());

            // verify the session data in the shared state has not changed
            final Session updatedSession = CommonTestUtil.fetchSession(originalSession.getSessionId());
            assertNotNull(updatedSession);
            assertEquals(originalSession.getSessionId(), updatedSession.getSessionId());

            // verify expiry not updated
            assertEquals(originalSession.getExpires(), updatedSession.getExpires());
        }
    }

    /**
     * This utility method:
     * <ul>
     * <li>sets up a valid session</li>
     * <li>sends a request with a cookie containing the session ID</li>
     * <li>expects a 200 OK status code</li>
     * <li>fetches the (potentially updated) session</li>
     * </ul>
     *
     * @param originalSession the session before the request is made
     * @param addCookie       whether to add the session cookie to the request
     * @return the updated session
     * @throws Throwable
     */
    private Session makeSessionRequest(Session originalSession, boolean addCookie) throws Throwable {
        // wait for the clock to tick to ensure we can test the updated expiration time
        Thread.sleep(100);

        final PolicyTestRequest request = PolicyTestRequest.build(PolicyTestRequestType.GET, RESOURCE);

        if (addCookie) {
            // send request with cookie
            request.header(Constants.HEADER_COOKIE, CommonTestUtil.buildCookieHeader(originalSession));
        }

        final PolicyTestResponse response = send(request);
        assertEquals(HttpURLConnection.HTTP_OK, response.code());

        // content should be returned
        assertNotNull(response.header(Constants.HEADER_CONTENT_LENGTH));

        // verify the session data in the shared state has changed
        final Session updatedSession = CommonTestUtil.fetchSession(originalSession.getSessionId());
        assertNotNull(updatedSession);
        assertEquals(originalSession.getSessionId(), updatedSession.getSessionId());

        // these should not change from request to request
        assertEquals(originalSession.getAbsoluteExpiry(), updatedSession.getAbsoluteExpiry());
        assertEquals(originalSession.getValidityPeriod(), updatedSession.getValidityPeriod());

        return updatedSession;
    }

    /**
     * Expect that a policy with the configuration of 'ValidationRequired' permits the request
     * to the back-end service if the cookie is present and the session is valid.
     *
     * @throws Throwable
     */
    @Test
    @Configuration(classpathConfigFile = "standard-config.json")
    @BackEndApi(RequiresAuthHeaderBackEndApi.class)
    public void testAuthenticatedRequestSuccessValidationRequired() throws Throwable {
        // test data - session expires in 60s
        final Session originalSession = CommonTestUtil.insertTestSession(60, false);

        // make request
        final Session updatedSession = makeSessionRequest(originalSession, true);

        // verify expiry updated
        assertTrue(TimeUtil.isAfterNow(updatedSession.getExpires()));
        assertTrue(updatedSession.getExpires() > originalSession.getExpires());
    }

    /**
     * Expect that a policy with the configuration of 'ValidationOptional' permits the request
     * to the back-end service if the cookie is present and the session is valid.
     *
     * @throws Throwable
     */
    @Test
    @Configuration(classpathConfigFile = "validation-optional-config.json")
    @BackEndApi(RequiresAuthHeaderBackEndApi.class)
    public void testAuthenticatedRequestSuccessValidationOptional() throws Throwable {
        // test data - session expires in 60s
        final Session originalSession = CommonTestUtil.insertTestSession(60, false);

        // make request
        final Session updatedSession = makeSessionRequest(originalSession, true);

        // verify expiry updated
        assertTrue(TimeUtil.isAfterNow(updatedSession.getExpires()));
        assertTrue(updatedSession.getExpires() > originalSession.getExpires());
    }

    /**
     * Expect that a policy with the configuration of 'ValidationOptional' still permits the request
     * to the back-end service even if the session cookie is not present.
     *
     * @throws Throwable
     */
    @Test
    @Configuration(classpathConfigFile = "validation-optional-config.json")
    @BackEndApi(EchoBackEndApi.class)
    public void testUnauthenticatedRequestSuccessValidationOptional() throws Throwable {
        // test data - session expires in 60s
        final Session originalSession = CommonTestUtil.insertTestSession(60, false);

        // make request
        final Session updatedSession = makeSessionRequest(originalSession, false);

        // verify expiry not updated
        assertTrue(updatedSession.getExpires() == originalSession.getExpires());
    }

    /**
     * Expect that a policy with the configuration of 'ValidationOptional' still permits the request
     * to the back-end service even if the session cookie is present but for an expired session.
     *
     * @throws Throwable
     */
    @Test
    @Configuration(classpathConfigFile = "validation-optional-config.json")
    @BackEndApi(EchoBackEndApi.class)
    public void testExpiredSessionRequestSuccessValidationOptional() throws Throwable {
        // test data - session has already expired
        final Session originalSession = CommonTestUtil.insertTestSession(-60, false);

        // make request
        final Session updatedSession = makeSessionRequest(originalSession, false);

        // verify expiry not updated
        assertTrue(updatedSession.getExpires() == originalSession.getExpires());
    }

    /**
     * Expect that a policy with the configuration of 'ValidationRequired' rejects the request with a 401 status
     * if the session has expired.
     *
     * @throws Throwable
     */
    @Test
    @Configuration(classpathConfigFile = "standard-config.json")
    @BackEndApi(RequiresAuthHeaderBackEndApi.class)
    public void testAuthenticatedRequestFailureExpiredSession() throws Throwable {
        // test data - session has already expired
        final Session originalSession = CommonTestUtil.insertTestSession(-60, false);

        // wait for the clock to tick to ensure we can test the updated expiration time
        Thread.sleep(100);

        // send request with cookie
        final PolicyTestRequest request = PolicyTestRequest.build(PolicyTestRequestType.GET, RESOURCE);
        request.header(Constants.HEADER_COOKIE, CommonTestUtil.buildCookieHeader(originalSession));

        sendAndExpect401(request, originalSession);
    }

    /**
     * Expect that a policy with the configuration of 'ValidationRequired' rejects the request with a 401 status
     * if the session is terminated.
     *
     * @throws Throwable
     */
    @Test
    @Configuration(classpathConfigFile = "standard-config.json")
    @BackEndApi(RequiresAuthHeaderBackEndApi.class)
    public void testAuthenticatedRequestFailureTerminatedSession() throws Throwable {
        // test data - session is terminated
        final Session originalSession = CommonTestUtil.insertTestSession(60, true);

        // wait for the clock to tick to ensure we can test the updated expiration time
        Thread.sleep(100);

        // send request with cookie
        final PolicyTestRequest request = PolicyTestRequest.build(PolicyTestRequestType.GET, RESOURCE);
        request.header(Constants.HEADER_COOKIE, CommonTestUtil.buildCookieHeader(originalSession));

        sendAndExpect401(request, originalSession);
    }

    /**
     * Expect that a policy with the configuration of 'ValidationRequired' rejects the request with a 401 code
     * if the session cookie is absent.
     *
     * @throws Throwable
     */
    @Test
    @Configuration(classpathConfigFile = "standard-config.json")
    @BackEndApi(RequiresAuthHeaderBackEndApi.class)
    public void testAuthenticatedRequestFailureNoCookie() throws Throwable {
        // test data - session expires in 60s
        final Session originalSession = CommonTestUtil.insertTestSession(60, false);

        // wait for the clock to tick to ensure we can test the updated expiration time
        Thread.sleep(100);

        // send request without cookie
        final PolicyTestRequest request = PolicyTestRequest.build(PolicyTestRequestType.GET, RESOURCE);
        request.header(Constants.HEADER_COOKIE, "");

        sendAndExpect401(request, originalSession);
    }

    /**
     * Expects that a ConfigurationException is thrown as the policy is not configured correctly.
     *
     * @throws Throwable
     */
    @Test(expected = InvalidConfigurationException.class)
    @Configuration(EMPTY_CONFIG)
    @BackEndApi(RequiresAuthHeaderBackEndApi.class)
    public void testAuthenticatedRequestExceptionEmptyConfig() throws Throwable {
        // test data - session expires in 60s
        final Session originalSession = CommonTestUtil.insertTestSession(60, false);

        // send request with cookie
        final PolicyTestRequest request = PolicyTestRequest.build(PolicyTestRequestType.GET, RESOURCE);
        request.header(Constants.HEADER_COOKIE, CommonTestUtil.buildCookieHeader(originalSession));

        send(request);
        fail(InvalidConfigurationException.class + " expected");
    }
}
