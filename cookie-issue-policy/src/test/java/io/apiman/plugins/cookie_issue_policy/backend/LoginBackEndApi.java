package io.apiman.plugins.cookie_issue_policy.backend;

import com.auth0.jwt.JWTSigner;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.plugins.session.test.CommonTestUtil;
import io.apiman.test.policies.IPolicyTestBackEndApi;
import io.apiman.test.policies.PolicyTestBackEndApiResponse;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Simulates either a login success with an HTTP 200 response code and a JSON body, or
 * a failure with an HTTP 401 response code.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@SuppressWarnings("nls")
public class LoginBackEndApi implements IPolicyTestBackEndApi {
    public static final String SUCCESSFUL_REQUEST_BODY = "TestAuthenticationString";
    public static final String INVALID_JWT_BODY = "TestInvalidJwt";

    @SuppressWarnings("unchecked")
    @Override
    public PolicyTestBackEndApiResponse invoke(ApiRequest apiRequest, byte[] requestBody) {
        final ObjectMapper mapper = new ObjectMapper();

        try {
            final ApiResponse apiResponse = new ApiResponse();
            final String responseBody;

            if (Arrays.equals(SUCCESSFUL_REQUEST_BODY.getBytes(), requestBody)) {
                // login success
                responseBody = mapper.writeValueAsString(new HashMap() {{
                    put("access_token", generateJwt(true));
                }});

                apiResponse.setCode(HttpURLConnection.HTTP_OK);
                apiResponse.getHeaders().put("Content-Type", "application/json");

            } else if (Arrays.equals(INVALID_JWT_BODY.getBytes(), requestBody)) {
                // issue invalid token
                responseBody = mapper.writeValueAsString(new HashMap() {{
                    put("access_token", generateJwt(false));
                }});

                apiResponse.setCode(HttpURLConnection.HTTP_OK);
                apiResponse.getHeaders().put("Content-Type", "application/json");

            } else {
                // login failure
                responseBody = "";
                apiResponse.setCode(HttpURLConnection.HTTP_UNAUTHORIZED);
            }

            apiResponse.getHeaders().put("Content-Length", String.valueOf(responseBody.getBytes("UTF-8").length));
            return new PolicyTestBackEndApiResponse(apiResponse, responseBody);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String generateJwt(boolean shouldBeValid) {
        final String audience = "https://example.com/gw";
        final String issuer = "https://example.com/idp";
        final String secret = CommonTestUtil.JWT_SIGNING_SECRET;

        // issued at claim
        final long issuedAt;
        if (shouldBeValid) {
            // issued now - should not have expired
            issuedAt = System.currentTimeMillis() / 1000L;
        } else {
            // issued in the past - will have expired already
            issuedAt = (System.currentTimeMillis() / 1000L) - 120L;
        }

        // expires claim. In this case the token expires in 60 seconds
        final long expires = issuedAt + 60L;

        final JWTSigner signer = new JWTSigner(secret);

        @SuppressWarnings("unchecked")
        final Map<String, Object> claims = new HashMap() {{
            put("aud", audience);
            put("iss", issuer);
            put("iat", issuedAt);
            put("exp", expires);
            put("sub", CommonTestUtil.AUTHENTICATED_PRINICPAL);
        }};

        return signer.sign(claims);
    }
}
