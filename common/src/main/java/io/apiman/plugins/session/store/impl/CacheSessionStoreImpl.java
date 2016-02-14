package io.apiman.plugins.session.store.impl;

import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.components.ICacheStoreComponent;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.session.model.Session;
import io.apiman.plugins.session.store.ISessionStore;
import io.apiman.plugins.session.util.Constants;

import java.io.IOException;

/**
 * A session store implementation using the {@link ICacheStoreComponent}.
 * Since some operations in the cache store are synchronous, calls are mapped to the behaviour of the
 * {@link IAsyncResultHandler}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class CacheSessionStoreImpl implements ISessionStore {
    protected ICacheStoreComponent cacheStore;

    @Override
    public void init(IPolicyContext context) {
        cacheStore = context.getComponent(ICacheStoreComponent.class);
    }

    @Override
    public void storeSession(String sessionId, Session session, IAsyncResultHandler<Void> handler) {
        try {
            cacheStore.put(buildCacheKey(sessionId), session, Constants.MAX_SESSION_TTL);
            handler.handle(AsyncResultImpl.<Void>create(null));

        } catch (IOException e) {
            handler.handle(AsyncResultImpl.<Void>create(e));
        }
    }

    @Override
    public void fetchSession(String sessionId, IAsyncResultHandler<Session> handler) {
        cacheStore.get(buildCacheKey(sessionId), Session.class, handler);
    }

    @Override
    public void deleteSession(String sessionId, IAsyncResultHandler<Void> handler) {
        try {
            // overwrite the data and set it to expire immediately
            cacheStore.put(buildCacheKey(sessionId), new Session(), 0);
            handler.handle(AsyncResultImpl.<Void>create(null));

        } catch (IOException e) {
            handler.handle(AsyncResultImpl.<Void>create(e));
        }
    }

    /**
     * @param sessionId the ID of the session
     * @return a stable cache key for the session ID
     */
    private String buildCacheKey(String sessionId) {
        return SESSION_DATA_PREFIX + "." + sessionId;
    }
}
