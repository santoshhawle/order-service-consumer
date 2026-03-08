package org.example.orderserviceconsumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.V4Pact;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "UserService", port = "8085",pactVersion = PactSpecVersion.V4)
class ConsumerPactTest {

    @Pact(provider = "UserService", consumer = "order_service_consumer")
    public V4Pact createPact(PactBuilder builder) {
        return builder
                .comment("Request for user 1")
                .given("User with id 1 exists")
                .expectsToReceiveHttpInteraction("Request for user 1", httpBuilder ->
                        httpBuilder
                                .withRequest(requestBuilder -> requestBuilder
                                        .method("GET")
                                        .path("/users/1")
                                )
                                .willRespondWith(responseBuilder -> responseBuilder
                                        .status(200)
                                        .body(new PactDslJsonBody()
                                                .numberType("id")
                                                .stringType("name")
                                                .stringType("email")
                                        )
                                )
                                .comment("Response contains id, name, email")
                )
                .comment("Finished defining V4 Pact")
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createPact")
    void testGetUserFromProvider(MockServer mockServer) throws IOException, ParseException {
        ClassicHttpResponse response = (ClassicHttpResponse) Request
                .get(mockServer.getUrl() + "/users/1")
                .execute()
                .returnResponse();

        String body = EntityUtils.toString(response.getEntity());

        // JSON-based assertions
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(body);

        assertTrue(json.has("id"));
        assertTrue(json.has("name"));
        assertTrue(json.has("email"));
    }


    @Pact(provider = "UserService", consumer = "order_service_consumer")
    public V4Pact userNotFoundPact(PactBuilder builder) {
        return builder
                .given("User with id 999 does not exist")
                .expectsToReceiveHttpInteraction("Request for non-existing user", http ->
                        http.withRequest(req -> req
                                        .method("GET")
                                        .path("/users/999")
                                )
                                .willRespondWith(res -> res
                                        .status(404)
                                )
                )
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "userNotFoundPact")
    void testUserNotFound(MockServer mockServer) throws Exception {
        ClassicHttpResponse response = (ClassicHttpResponse) Request
                .get(mockServer.getUrl() + "/users/999")
                .execute()
                .returnResponse();

        assertTrue(response.getCode() == 404);
    }

}