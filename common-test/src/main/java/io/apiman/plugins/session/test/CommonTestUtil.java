package io.apiman.plugins.session.test;

import io.apiman.gateway.engine.IComponentRegistry;
import io.apiman.gateway.engine.async.IAsyncResult;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.impl.EngineImpl;
import io.apiman.gateway.engine.policy.PolicyContextImpl;
import io.apiman.plugins.session.model.Session;
import io.apiman.plugins.session.store.ISessionStore;
import io.apiman.plugins.session.store.SessionStoreFactory;
import io.apiman.plugins.session.util.SessionUtil;
import io.apiman.test.policies.ApimanPolicyTest;
import org.apache.commons.lang.SerializationUtils;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility methods for common test activities.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class CommonTestUtil {
    public static final String AUTHENTICATED_PRINICPAL = "apiman";
    public static final String COOKIE_NAME = "XSESSION";

    /**
     * Performs a deep copy of an object, returning a new instance with the
     * same properties.
     *
     * @param obj the Object to copy
     * @return a new instance of the same class T with the same properties
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deepCopy(T obj) {
        return (T) SerializationUtils.deserialize(SerializationUtils.serialize(obj));
    }

    /**
     * @return the ISessionStore used by the test policy engine
     */
    private static ISessionStore getSessionStore() {
        final EngineImpl engine = (EngineImpl) ApimanPolicyTest.tester.getEngine();
        final IComponentRegistry componentRegistry = engine.getComponentRegistry();
        return SessionStoreFactory.getSessionStore(new PolicyContextImpl(componentRegistry));
    }

    /**
     * Retrieve the Session with the given ID.
     *
     * @param sessionId the ID of the Session.
     * @return the Session
     */
    public static Session fetchSession(String sessionId) {
        final AtomicReference<IAsyncResult<Session>> propertyResult = new AtomicReference<>();

        // fetch the session
        final ISessionStore sessionStore = getSessionStore();
        sessionStore.fetchSession(sessionId, new IAsyncResultHandler<Session>() {
            @Override
            public void handle(IAsyncResult<Session> result) {
                propertyResult.set(result);
            }
        });

        // wait for the result
        while (null == propertyResult.get()) {
            Thread.yield();
        }

        return propertyResult.get().getResult();
    }

    /**
     * Store the Session.
     *
     * @param session the Session to store
     */
    private static void storeSession(Session session) {
        final AtomicBoolean stored = new AtomicBoolean(false);

        // store the session
        final ISessionStore sessionStore = getSessionStore();
        sessionStore.storeSession(session.getSessionId(), session, new IAsyncResultHandler<Void>() {
            @Override
            public void handle(IAsyncResult<Void> result) {
                stored.set(true);
            }
        });

        // wait for storage
        while (!stored.get()) {
            Thread.yield();
        }
    }

    /**
     * Builds a test Session and inserts it into the session store.
     * Note: the returned object is deep copied as the Session in the store gets updated by reference.
     *
     * @param validityPeriod the validity period in seconds - negative will mean the Session has already expired
     * @param terminated     whether the Session is terminated
     * @return a test Session
     */
    public static Session insertTestSession(int validityPeriod, boolean terminated) {
        final Session session = SessionUtil.buildSession(
                UUID.randomUUID().toString(),
                AUTHENTICATED_PRINICPAL,
                validityPeriod);

        session.setTerminated(terminated);
        storeSession(session);

        // deep copy AFTER storage
        return deepCopy(session);
    }

    public static String buildCookieHeader(Session originalSession) {
        return COOKIE_NAME + "=" + originalSession.getSessionId() + ";";
    }
}
