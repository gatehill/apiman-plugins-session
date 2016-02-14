package io.apiman.plugins.session.store.impl;

import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.components.ISharedStateComponent;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.session.model.Session;
import io.apiman.plugins.session.store.ISessionStore;

/**
 * A session store implementation using the {@link ISharedStateComponent}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SharedStateSessionStoreImpl implements ISessionStore {

    /**
     * Required as a non-<code>null</code> default value.
     */
    private static final Session DEFAULT_SESSION = new Session();

    private ISharedStateComponent sharedState;

    @Override
    public void init(IPolicyContext context) {
        sharedState = context.getComponent(ISharedStateComponent.class);
    }

    @Override
    public void storeSession(String sessionId, Session session, IAsyncResultHandler<Void> handler) {
        sharedState.setProperty(SESSION_DATA_PREFIX, sessionId, session, handler);
    }

    @Override
    public void fetchSession(String sessionId, IAsyncResultHandler<Session> handler) {
        sharedState.getProperty(SESSION_DATA_PREFIX, sessionId, DEFAULT_SESSION, handler);
    }

    @Override
    public void deleteSession(String sessionId, IAsyncResultHandler<Void> handler) {
        sharedState.clearProperty(SESSION_DATA_PREFIX, sessionId, handler);
    }
}
