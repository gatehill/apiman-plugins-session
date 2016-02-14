package io.apiman.plugins.cookie_remove_policy;

import io.apiman.gateway.engine.IApiConnection;
import io.apiman.gateway.engine.IApiConnectionResponse;
import io.apiman.gateway.engine.IApiConnector;
import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.HeaderHashMap;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.beans.exceptions.ConnectorException;
import io.apiman.gateway.engine.io.IApimanBuffer;
import io.apiman.gateway.engine.policy.IConnectorInterceptor;

import java.net.HttpURLConnection;

/**
 * A connector interceptor that short-circuits chain processing to skip invocation of the back-end service.
 *
 * This is required because calling {@link io.apiman.gateway.engine.policy.Chain#doSkip(Object)} still seems
 * to invoke the back-end service.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class ShortcircuitConnectorInterceptor implements IConnectorInterceptor {
    @Override
    public IApiConnector createConnector() {
        return new IApiConnector() {
            @Override
            public IApiConnection connect(ApiRequest request, IAsyncResultHandler<IApiConnectionResponse> handler) throws ConnectorException {
                return new ShortcircuitServiceConnection(handler);
            }
        };
    }

    /**
     * A connection consisting predominantly dummy methods as we're not contacting a real service.
     *
     * @author Marc Savy {@literal <msavy@redhat.com>}
     */
    private static class ShortcircuitServiceConnection implements IApiConnection, IApiConnectionResponse {
        private boolean finished = false;
        private IAsyncHandler<Void> endHandler;
        private IAsyncResultHandler<IApiConnectionResponse> responseHandler;
        private ApiResponse response;

        public ShortcircuitServiceConnection(IAsyncResultHandler<IApiConnectionResponse> handler) {
            responseHandler = handler;

            response = new ApiResponse();
            response.setCode(HttpURLConnection.HTTP_OK);
            response.setHeaders(new HeaderHashMap());
        }

        @Override
        public void abort() {
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        /**
         * @see IApiConnection#isConnected()
         */
        @Override
        public boolean isConnected() {
            return !finished;
        }

        @Override
        public void write(IApimanBuffer chunk) {
        }

        @Override
        public void end() {
            responseHandler.handle(AsyncResultImpl.<IApiConnectionResponse>create(this));
        }

        @Override
        public void transmit() {
            endHandler.handle((Void) null);
            finished = true;
        }

        @Override
        public void bodyHandler(IAsyncHandler<IApimanBuffer> bodyHandler) {
        }

        @Override
        public void endHandler(IAsyncHandler<Void> endHandler) {
            this.endHandler = endHandler;
        }

        @Override
        public ApiResponse getHead() {
            return response;
        }
    }
}
