package io.apiman.plugins.cookie_validate_policy.backend;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.plugins.session.test.CommonTestUtil;
import io.apiman.test.policies.IPolicyTestBackEndApi;
import io.apiman.test.policies.PolicyTestBackEndApiResponse;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;

/**
 * Simulates a back-end service that requires the authenticated principal HTTP header
 * in the request.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@SuppressWarnings("nls")
public class RequiresAuthHeaderBackEndApi implements IPolicyTestBackEndApi {
    public static final String AUTH_HEADER_NAME = "X-Authenticated-Principal";
    public static final String RESPONSE_BODY = "{\"aField\":\"aValue\", \"anotherField\":\"anotherValue\"}";

    @Override
    public PolicyTestBackEndApiResponse invoke(ApiRequest apiRequest, byte[] requestBody) {
        try {
            final ApiResponse apiResponse = new ApiResponse();
            final String responseBody;

            if (CommonTestUtil.AUTHENTICATED_PRINICPAL.equals(apiRequest.getHeaders().get(AUTH_HEADER_NAME))) {
                // success
                responseBody = RESPONSE_BODY;
                apiResponse.setCode(HttpURLConnection.HTTP_OK);
                apiResponse.getHeaders().put("Content-Type", "application/json");
                apiResponse.getHeaders().put("Content-Length", String.valueOf(responseBody.getBytes("UTF-8").length));

            } else {
                // login failure
                responseBody = "";
                apiResponse.setCode(HttpURLConnection.HTTP_UNAUTHORIZED);
                apiResponse.getHeaders().put("Content-Length", "0");
            }

            return new PolicyTestBackEndApiResponse(apiResponse, responseBody);

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
