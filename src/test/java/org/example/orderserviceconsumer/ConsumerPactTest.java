package org.example.orderserviceconsumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Consumer Pact test class for order-service to verify contract with UserService provider.
 * This test ensures that the order-service consumer can successfully communicate with the UserService
 * according to the defined contract, validating both happy-path and error scenarios.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(
        providerName = "UserService",
        port = "8085",
        pactVersion = PactSpecVersion.V4)
class ConsumerPactTest {

    // ============ Constants: HTTP Configuration ============
    private static final int HTTP_OK = 200;
    private static final int HTTP_NOT_FOUND = 404;

    // ============ Constants: Test Data ============
    private static final String EXISTING_USER_ID = "1";
    private static final String NON_EXISTING_USER_ID = "999";

    // ============ Constants: API Endpoints ============
    private static final String USERS_ENDPOINT = "/users/";

    // ============ Constants: Pact Configuration ============
    private static final String USER_SERVICE_PROVIDER = "UserService";
    private static final String ORDER_SERVICE_CONSUMER = "order_service_consumer";

    // ============ Constants: JSON Response Fields ============
    private static final String USER_ID_FIELD = "id";
    private static final String USER_NAME_FIELD = "name";
    private static final String USER_EMAIL_FIELD = "email";

    /**
     * Defines the Pact contract for successfully retrieving an existing user.
     * This pact specifies that the consumer (order-service) expects:
     * - HTTP Method: GET
     * - Path: /users/1
     * - Response Status: 200 OK
     * - Response Body: JSON with id, name, and email fields
     *
     * @param builder PactBuilder instance used to construct the pact contract
     * @return V4Pact representing the successful user retrieval contract
     */
    @Pact(provider = USER_SERVICE_PROVIDER, consumer = ORDER_SERVICE_CONSUMER)
    public au.com.dius.pact.core.model.V4Pact createPact(PactBuilder builder) {
        return builder
                .comment("Pact contract for successful user retrieval")
                .given("User with id 1 exists")
                .expectsToReceiveHttpInteraction("Request for user 1", httpBuilder ->
                        httpBuilder
                                .withRequest(requestBuilder -> requestBuilder
                                        .method("GET")
                                        .path(USERS_ENDPOINT + EXISTING_USER_ID)
                                )
                                .willRespondWith(responseBuilder -> responseBuilder
                                        .status(HTTP_OK)
                                        .body(new PactDslJsonBody()
                                                .numberType(USER_ID_FIELD)
                                                .stringType(USER_NAME_FIELD)
                                                .stringType(USER_EMAIL_FIELD)
                                        )
                                )
                                .comment("Response body contains user id, name, and email fields")
                )
                .toPact();
    }

    /**
     * Tests the successful retrieval of user data from the provider.
     * This test verifies that:
     * 1. The HTTP request completes successfully
     * 2. The response contains all expected user fields
     * 3. The contract between consumer and provider is satisfied
     *
     * @param mockServer MockServer instance configured by the Pact framework
     * @throws IOException if response body cannot be read or parsed
     * @throws ParseException if HTTP response parsing fails
     */
    @Test
    @PactTestFor(pactMethod = "createPact")
    void testGetUserFromProvider(MockServer mockServer) throws IOException, ParseException {
        // Execute GET request to retrieve user data from the mock provider
        ClassicHttpResponse response = (ClassicHttpResponse) Request
                .get(mockServer.getUrl() + USERS_ENDPOINT + EXISTING_USER_ID)
                .execute()
                .returnResponse();

        // Extract and parse response body into a JSON object for field validation
        String responseBody = EntityUtils.toString(response.getEntity());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode userJsonNode = objectMapper.readTree(responseBody);

        // Verify that all required user fields are present in the response.
        // This ensures the provider complies with the contract specification.
        verifyUserResponseFields(userJsonNode);
    }

    /**
     * Helper method to validate the presence of all required user fields in the response.
     * Improves code readability and reduces duplication across test methods.
     *
     * @param userJsonNode the parsed JSON response containing user data
     */
    private void verifyUserResponseFields(JsonNode userJsonNode) {
        assertTrue(userJsonNode.has(USER_ID_FIELD),
                "Response must contain '" + USER_ID_FIELD + "' field");
        assertTrue(userJsonNode.has(USER_NAME_FIELD),
                "Response must contain '" + USER_NAME_FIELD + "' field");
        assertTrue(userJsonNode.has(USER_EMAIL_FIELD),
                "Response must contain '" + USER_EMAIL_FIELD + "' field");
    }

    /**
     * Defines the Pact contract for handling requests to non-existent users.
     * This pact specifies that the consumer expects:
     * - HTTP Method: GET
     * - Path: /users/999 (non-existent user)
     * - Response Status: 404 Not Found
     * This validates error handling behavior between consumer and provider.
     *
     * @param builder PactBuilder instance used to construct the pact contract
     * @return V4Pact representing the user not found error contract
     */
    @Pact(provider = USER_SERVICE_PROVIDER, consumer = ORDER_SERVICE_CONSUMER)
    public au.com.dius.pact.core.model.V4Pact userNotFoundPact(PactBuilder builder) {
        return builder
                .comment("Pact contract for non-existent user error handling")
                .given("User with id 999 does not exist")
                .expectsToReceiveHttpInteraction("Request for non-existing user", http ->
                        http.withRequest(req -> req
                                        .method("GET")
                                        .path(USERS_ENDPOINT + NON_EXISTING_USER_ID)
                                )
                                .willRespondWith(res -> res
                                        .status(HTTP_NOT_FOUND)
                                )
                )
                .toPact();
    }

    /**
     * Tests that the provider correctly returns a 404 status code for non-existent users.
     * This test verifies error handling behavior and ensures the consumer can gracefully
     * handle scenarios where the requested resource is not available.
     *
     * @param mockServer MockServer instance configured by the Pact framework
     * @throws Exception if HTTP request execution fails
     */
    @Test
    @PactTestFor(pactMethod = "userNotFoundPact")
    void testUserNotFound(MockServer mockServer) throws Exception {
        // Execute GET request for a non-existent user ID
        ClassicHttpResponse response = (ClassicHttpResponse) Request
                .get(mockServer.getUrl() + USERS_ENDPOINT + NON_EXISTING_USER_ID)
                .execute()
                .returnResponse();

        // Verify that the response status code is 404 (Not Found).
        // This confirms the provider correctly handles requests for non-existent resources.
        assertEquals(HTTP_NOT_FOUND, response.getCode(),
                "Expected 404 status code when user does not exist");
    }

}