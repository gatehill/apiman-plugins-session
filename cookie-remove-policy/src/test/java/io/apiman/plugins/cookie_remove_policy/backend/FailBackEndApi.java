package io.apiman.plugins.cookie_remove_policy.backend;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.test.policies.IPolicyTestBackEndApi;
import io.apiman.test.policies.PolicyTestBackEndApiResponse;

/**
 * This back-end service always throws a RuntimeException as it should not be invoked (i.e. the policy should skip).
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class FailBackEndApi implements IPolicyTestBackEndApi {
    @Override
    public PolicyTestBackEndApiResponse invoke(ApiRequest request, byte[] requestBody) {
        throw new RuntimeException("Back-end service should not be called.");
    }
}
