package io.apiman.plugins.session.store;

import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.session.model.Session;

/**
 * Represents a session store.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ISessionStore {
    /**
     * Prefix for session data objects.
     */
    String SESSION_DATA_PREFIX = "io.apiman.plugins.session";

    /**
     * Initialise the session store, guaranteed to be called before any of the lifecycle methods.
     * Implementations should perform any necessary prerequisite setup here.
     *
     * @param context
     */
    void init(IPolicyContext context);

    void storeSession(String sessionId, Session session, IAsyncResultHandler<Void> handler);

    void fetchSession(String sessionId, IAsyncResultHandler<Session> handler);

    void deleteSession(String sessionId, IAsyncResultHandler<Void> handler);
}
