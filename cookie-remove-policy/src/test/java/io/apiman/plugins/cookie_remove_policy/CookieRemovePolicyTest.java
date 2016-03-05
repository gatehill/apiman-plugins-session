package io.apiman.plugins.cookie_remove_policy;

import io.apiman.plugins.cookie_remove_policy.backend.FailBackEndApi;
import io.apiman.plugins.session.exception.InvalidConfigurationException;
import io.apiman.plugins.session.model.Cookie;
import io.apiman.plugins.session.model.Session;
import io.apiman.plugins.session.test.CommonTestUtil;
import io.apiman.plugins.session.util.Constants;
import io.apiman.plugins.session.util.CookieUtil;
import io.apiman.test.policies.*;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.net.HttpURLConnection;

import static org.junit.Assert.*;

/**
 * Tests for {@link CookieRemovePolicy}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@SuppressWarnings("nls")
@TestingPolicy(CookieRemovePolicy.class)
public class CookieRemovePolicyTest extends ApimanPolicyTest {

    private static final String EMPTY_CONFIG = "{}";
    private static final String RESOURCE = "/some/resource";

    /**
     * Make a request for a session that is not yet expired and expect
     * that an HTTP 200 response is returned and that the session was invalidated.
     *
     * @throws Throwable
     */
    private void callAndExpect200(boolean expectBody) throws Throwable {
        // test data - session expires in 60s
        final Session originalSession = CommonTestUtil.insertTestSession(60, false);

        // send request with cookie
        final PolicyTestRequest request = PolicyTestRequest.build(PolicyTestRequestType.GET, RESOURCE);
        request.header(Constants.HEADER_COOKIE, CommonTestUtil.buildCookieHeader(originalSession));

        final PolicyTestResponse response = send(request);
        assertEquals(HttpURLConnection.HTTP_OK, response.code());

        if (expectBody) {
            // content should be returned
            assertNotNull(response.header(Constants.HEADER_CONTENT_LENGTH));
            assertTrue(StringUtils.isNotBlank(response.body()));

        } else {
            // content should not be returned
            assertNull(response.header(Constants.HEADER_CONTENT_LENGTH));
            assertTrue(StringUtils.isBlank(response.body()));
        }

        // verify the session was invalidated
        verifySessionInvalidated(originalSession, response);
    }

    /**
     * Make a request for a session that is not yet expired and expect
     * that an HTTP 302 response is returned and that the session was invalidated.
     *
     * @throws Throwable
     */
    private void callAndExpect302Redirect() throws Throwable {
        // test data - session expires in 60s
        final Session originalSession = CommonTestUtil.insertTestSession(60, false);

        // send request with cookie
        final PolicyTestRequest request = PolicyTestRequest.build(PolicyTestRequestType.GET, RESOURCE);
        request.header(Constants.HEADER_COOKIE, CommonTestUtil.buildCookieHeader(originalSession));

        final PolicyTestResponse response = send(request);
        assertEquals(HttpURLConnection.HTTP_MOVED_TEMP, response.code());

        // content should not be returned - policy is configured to redirect to another URL
        assertNull(response.header(Constants.HEADER_CONTENT_LENGTH));
        assertTrue(StringUtils.isBlank(response.body()));
        assertEquals("/another/url", response.header(Constants.HEADER_LOCATION));

        // verify the session was invalidated
        verifySessionInvalidated(originalSession, response);
    }

    /**
     * Verify the session data in the shared state was removed and the cookie was marked for
     * removal in the HTTP response.
     *
     * @param originalSession the Session state from before the request was sent
     * @param response        the policy response
     */
    private void verifySessionInvalidated(Session originalSession, PolicyTestResponse response) {
        // verify the cookie was set to be removed
        final String cookieHeader = response.header(Constants.HEADER_SET_COOKIE);
        assertNotNull(cookieHeader);

        final Cookie cookie = CookieUtil.parseResponseCookie(cookieHeader);
        assertEquals(CommonTestUtil.COOKIE_NAME, cookie.getName());
        assertNull(cookie.getValue());
        assertEquals(0, cookie.getMaxAge().intValue());

        // verify the session data in the shared state was removed
        final Session updatedSession = CommonTestUtil.fetchSession(originalSession.getSessionId());
        assertNull(updatedSession.getSessionId());
    }

    /**
     * Sends a request that does not contain a session Cookie with a valid session ID.
     *
     * @param expectSetCookieHeader whether the response contains a 'Set-Cookie' header to force removal of the cookie.
     * @throws Throwable
     */
    private void callWithoutCookie(boolean expectSetCookieHeader) throws Throwable {
        // test data - session expires in 60s
        final Session originalSession = CommonTestUtil.insertTestSession(60, false);

        // send request without cookie
        final PolicyTestRequest request = PolicyTestRequest.build(PolicyTestRequestType.GET, RESOURCE);
        request.header(Constants.HEADER_COOKIE, "");

        final PolicyTestResponse response = send(request);
        assertEquals(HttpURLConnection.HTTP_OK, response.code());

        // content should be returned
        assertNotNull(response.header(Constants.HEADER_CONTENT_LENGTH));
        assertTrue(StringUtils.isNotBlank(response.body()));

        if (expectSetCookieHeader) {
            // verify the cookie was set to be removed
            assertNotNull(response.header(Constants.HEADER_SET_COOKIE));

        } else {
            // verify the cookie was not set to be removed
            assertNull(response.header(Constants.HEADER_SET_COOKIE));
        }

        // verify the session data in the shared state was not removed
        final Session updatedSession = CommonTestUtil.fetchSession(originalSession.getSessionId());
        assertEquals(originalSession.getSessionId(), updatedSession.getSessionId());
        assertFalse(updatedSession.isTerminated());
    }

    /**
     * Expects the Session identified by the value of the cookie is removed, and that the HTTP response
     * instructs the browser to remove the cookie.
     *
     * @throws Throwable
     */
    @Test
    @Configuration(classpathConfigFile = "passthrough-config.json")
    @BackEndApi(EchoBackEndApi.class)
    public void testLogoutSuccess() throws Throwable {
        callAndExpect200(true);
    }

    /**
     * Expects the Session identified by the value of the cookie is removed, and that the HTTP response
     * instructs the browser to remove the cookie, and the call to the back-end service is skipped.
     *
     * @throws Throwable
     * @see FailBackEndApi
     */
    @Test
    @Configuration(classpathConfigFile = "skip-backend-config.json")
    @BackEndApi(FailBackEndApi.class)
    public void testLogoutSuccessSkipBackend() throws Throwable {
        callAndExpect200(false);
    }

    /**
     * Expects the Session identified by the value of the cookie is removed, and that the HTTP response
     * instructs the browser to remove the cookie, a 302 HTTP status code is returned.
     *
     * @throws Throwable
     */
    @Test
    @Configuration(classpathConfigFile = "redirect-config.json")
    @BackEndApi(EchoBackEndApi.class)
    public void testLogoutSuccessWithRedirect() throws Throwable {
        callAndExpect302Redirect();
    }

    /**
     * Expects the Session identified by the value of the cookie is removed, and that the HTTP response
     * instructs the browser to remove the cookie, a 302 HTTP status code is returned,
     * and the call to the back-end service is skipped.
     *
     * @throws Throwable
     * @see FailBackEndApi
     * @see #testLogoutSuccessWithRedirect()
     * @see #testLogoutSuccessSkipBackend()
     */
    @Test
    @Configuration(classpathConfigFile = "redirect-skip-backend-config.json")
    @BackEndApi(FailBackEndApi.class)
    public void testLogoutSuccessWithRedirectAndSkipBackend() throws Throwable {
        callAndExpect302Redirect();
    }

    /**
     * Expects the Session is unchanged as the request does not contain a Cookie with a valid session ID. The response
     * should not contain a 'Set-Cookie' header.
     *
     * @throws Throwable
     */
    @Test
    @Configuration(classpathConfigFile = "passthrough-config.json")
    @BackEndApi(EchoBackEndApi.class)
    public void testLogoutIgnoreMissingCookie() throws Throwable {
        callWithoutCookie(false);
    }

    /**
     * Expects the Session is unchanged as the request does not contain a Cookie with a valid session ID. The response
     * should contain a 'Set-Cookie' header to force removal of the cookie.
     *
     * @throws Throwable
     */
    @Test
    @Configuration(classpathConfigFile = "force-remove-config.json")
    @BackEndApi(EchoBackEndApi.class)
    public void testLogoutForceRemoveMissingCookie() throws Throwable {
        callWithoutCookie(true);
    }

    /**
     * Expects that a ConfigurationException is thrown as the policy is not configured correctly.
     *
     * @throws Throwable
     */
    @Test(expected = InvalidConfigurationException.class)
    @Configuration(EMPTY_CONFIG)
    @BackEndApi(EchoBackEndApi.class)
    public void testLogoutExceptionEmptyConfig() throws Throwable {
        // test data - session expires in 60s
        final Session originalSession = CommonTestUtil.insertTestSession(60, false);

        // send request with cookie
        final PolicyTestRequest request = PolicyTestRequest.build(PolicyTestRequestType.GET, RESOURCE);
        request.header(Constants.HEADER_COOKIE, CommonTestUtil.buildCookieHeader(originalSession));

        send(request);
        fail(InvalidConfigurationException.class + " expected");
    }
}
