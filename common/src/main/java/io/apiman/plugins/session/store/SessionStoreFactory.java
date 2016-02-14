package io.apiman.plugins.session.store;

import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.session.exception.SessionStoreNotFoundException;
import io.apiman.plugins.session.store.impl.SharedStateSessionStoreImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Returns the configured implementation of the {@link ISessionStore}.
 * Set the System property {@link #SESSION_STORE_IMPL} to configure the implementation to use.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SessionStoreFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionStoreFactory.class);
    private static final String SESSION_STORE_IMPL = "io.apiman.plugins.session.ISessionStore";
    private static final String DEFAULT_IMPL = SharedStateSessionStoreImpl.class.getCanonicalName();

    /**
     * Cached session store.
     */
    private static ISessionStore sessionStore;

    /**
     * Get the session store implementation.
     *
     * @param context
     * @return
     * @throws SessionStoreNotFoundException
     */
    public synchronized static ISessionStore getSessionStore(IPolicyContext context) throws SessionStoreNotFoundException {
        if (null == sessionStore) {
            final String sessionStoreImpl = System.getProperty(SESSION_STORE_IMPL, DEFAULT_IMPL);
            try {
                sessionStore = (ISessionStore) Class.<ISessionStore>forName(sessionStoreImpl).newInstance();
                sessionStore.init(context);
                LOGGER.debug("Using session store implementation: " + sessionStoreImpl);

            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                throw new SessionStoreNotFoundException(sessionStoreImpl, e);
            }
        }
        return sessionStore;
    }
}
