package io.apiman.plugins.cookie_issue_policy.backend;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.plugins.session.test.CommonTestUtil;
import io.apiman.test.policies.IPolicyTestBackEndApi;
import io.apiman.test.policies.PolicyTestBackEndApiResponse;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.Arrays;

/**
 * Simulates either a login success with an HTTP 200 response code and a JSON body, or
 * a failure with an HTTP 401 response code.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@SuppressWarnings("nls")
public class LoginBackEndApi implements IPolicyTestBackEndApi {
    public static final String SUCCESSFUL_REQUEST_BODY = "TestAuthenticationString";

    @Override
    public PolicyTestBackEndApiResponse invoke(ApiRequest apiRequest, byte[] requestBody) {
        try {
            final ApiResponse apiResponse = new ApiResponse();
            final String responseBody;

            if (Arrays.equals(SUCCESSFUL_REQUEST_BODY.getBytes(), requestBody)) {
                // login success
                responseBody = "{\"username\":\"" + CommonTestUtil.AUTHENTICATED_PRINICPAL + "\", \"anotherField\":\"anotherValue\"}";
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
