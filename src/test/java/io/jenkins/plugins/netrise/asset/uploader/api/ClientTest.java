package io.jenkins.plugins.netrise.asset.uploader.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ClientTest {
    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockResponse;

    static class MockClient extends Client {
        private HttpClient httpClient;
        public MockClient(URI tokenUri, String organization, String clientId, String clientSecret, String audience) {
            super(tokenUri, organization, clientId, clientSecret, audience);
        }

        @Override
        public HttpClient getHttpClient() {
            return httpClient;
        }

        public void setHttpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
        }
    }

    @InjectMocks
    private MockClient client = new MockClient(URI.create("https://example.com/auth"), "orgId", "clientId", "clientSecret", "audience");

    private HttpHeaders getJsonContentTypeHeaders() {
        return HttpHeaders.of(Map.of(Client.CONTENT_TYPE_HEADER, List.of(Client.APP_JSON_CONTENT_TYPE)), (String s1, String s2) -> true);
    }

    private HttpHeaders getEmptyHeaders() {
        return HttpHeaders.of(Collections.emptyMap(), (String s1, String s2) -> true);
    }

    private void mockAuthentication() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.headers()).thenReturn(getJsonContentTypeHeaders());
        when(mockResponse.body()).thenReturn("{\"access_token\":\"valid_token\",\"token_type\":\"Bearer\",\"expires_in\":3600}");

        Client.TokenInstance tokenInstance = client.authenticate();

        assertTrue(tokenInstance.isValid());
    }

    private void whenSuccessfulResponse(String json) {
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.headers()).thenReturn(getJsonContentTypeHeaders());
        when(mockResponse.body()).thenReturn(json);
    }

    // AUTHENTICATION

    @Test
    void testAuthenticate_Successful() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://auth.example.com")).POST(HttpRequest.BodyPublishers.ofString("{}")).build();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        whenSuccessfulResponse("{\"access_token\":\"valid_token\",\"token_type\":\"Bearer\",\"expires_in\":3600}");

        Client.TokenInstance tokenInstance = client.authenticate();

        assertTrue(tokenInstance.isValid());
        assertEquals("Bearer valid_token", tokenInstance.getAccessToken());
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testAuthenticate_InvalidToken() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://auth.example.com")).POST(HttpRequest.BodyPublishers.ofString("{}")).build();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        whenSuccessfulResponse("{\"access_token\":null,\"token_type\":\"Bearer\",\"expires_in\":3600}");

        assertThrows(AuthException.class, () -> client.authenticate());
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testAuthenticate_ExpiredToken() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://auth.example.com")).POST(HttpRequest.BodyPublishers.ofString("{}")).build();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        whenSuccessfulResponse("{\"access_token\":\"valid_token\",\"token_type\":\"Bearer\",\"expires_in\":1}");

        Client.TokenInstance tokenInstance = client.authenticate();

        // Simulate token expiration
        Thread.sleep(2000); // Wait for 2 seconds (longer than expiration time)
        assertFalse(tokenInstance.isValid(), "Token should be expired");
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testAuthenticate_ExpiredToken2() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://auth.example.com")).POST(HttpRequest.BodyPublishers.ofString("{}")).build();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        whenSuccessfulResponse("{\"access_token\":\"valid_token\",\"token_type\":\"Bearer\",\"expires_in\":5}");

        Client.TokenInstance tokenInstance = client.authenticate();

        // Simulate token expiration
        Thread.sleep(3000); // Wait for 3 seconds (less than expiration time)
        assertTrue(tokenInstance.isValid(), "Token should be valid");
        Thread.sleep(3000); // Wait for 3 seconds (longer than expiration time)
        assertFalse(tokenInstance.isValid(), "Token should be expired");
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testAuthenticate_MissingTokenType() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        whenSuccessfulResponse("{\"access_token\":\"valid_token\",\"token_type\":null,\"expires_in\":3600}");

        assertThrows(AuthException.class, () -> client.authenticate());
    }

    @Test
    void testAuthenticate_MissingTokenFields() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        whenSuccessfulResponse("{}"); // Missing all token data

        assertThrows(AuthException.class, () -> client.authenticate());
    }

    @Test
    void testAuthenticate_MalformedJsonResponse() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        whenSuccessfulResponse("{access_token:\"valid_token\" token_type:\"Bearer\" expires_in:3600}");

        assertThrows(ClientException.class, () -> client.authenticate()); // Malformed JSON
    }

    @Test
    void testAuthenticate_NullResponseBody() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        whenSuccessfulResponse(null);

        assertThrows(AuthException.class, () -> client.authenticate()); // Null response
    }

    @Test
    void testAuthenticate_EmptyJsonFields() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        whenSuccessfulResponse("{\"access_token\":\"\",\"token_type\":\"\",\"expires_in\":0}");

        assertThrows(AuthException.class, () -> client.authenticate()); // Empty fields should fail
    }

    @Test
    void testGet_SuccessfulResponse() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://example.com")).GET().build();

        mockAuthentication();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        whenSuccessfulResponse("{\"message\":\"Success\"}");

        Client.Response response = client.get(URI.create("https://example.com/get"));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Success"));
        verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testPost_SuccessfulResponse() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://example.com")).POST(HttpRequest.BodyPublishers.ofString("{}")).build();

        mockAuthentication();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(201);
        when(mockResponse.headers()).thenReturn(getJsonContentTypeHeaders());
        when(mockResponse.body()).thenReturn("{\"message\":\"Created\"}");

        Client.Response response = client.post(URI.create("https://example.com/post"), "{}");

        assertEquals(201, response.getStatusCode());
        assertTrue(response.getBody().contains("Created"));
        verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSend_ThrowsException() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://example.com")).GET().build();

        mockAuthentication();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Request failed"));

        assertThrows(ClientException.class, () -> client.get(URI.create("https://example.com")));
        verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSend_HandlesServerError() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://example.com")).GET().build();

        mockAuthentication();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockResponse.headers()).thenReturn(getJsonContentTypeHeaders());
        when(mockResponse.body()).thenReturn("{\"error\":\"Server Error\"}");

        assertThrows(ClientException.class, () -> client.get(URI.create("https://example.com")));
        verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSend_HandlesConnectionFailure() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://example.com")).GET().build();

        mockAuthentication();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection failed"));

        assertThrows(ClientException.class, () -> client.get(URI.create("https://example.com")));
        verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSend_NetworkTimeout() throws Exception {
        mockAuthentication();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Network timeout"));

        assertThrows(ClientException.class, () -> client.get(URI.create("https://example.com")));
    }

    @Test
    void testSend_UnexpectedStatusCode() throws Exception {
        mockAuthentication();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(418); // A rare status code
        when(mockResponse.headers()).thenReturn(getJsonContentTypeHeaders());
        when(mockResponse.body()).thenReturn("{\"error\":\"I’m a teapot\"}");

        assertThrows(ClientException.class, () -> client.get(URI.create("https://example.com")));
    }

    @Test
    void testSend_EmptyResponseBody() throws Exception {
        mockAuthentication();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(400);
        when(mockResponse.body()).thenReturn("");

        assertThrows(ClientException.class, () -> client.get(URI.create("https://example.com")));
    }

    @Test
    void testSend_NotFoundResponseBody() throws Exception {
        mockAuthentication();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(404);
        when(mockResponse.headers()).thenReturn(HttpHeaders.of(Map.of(Client.CONTENT_TYPE_HEADER, List.of(Client.APP_TEXT_CONTENT_TYPE)), (String s1, String s2) -> true));
        when(mockResponse.body()).thenReturn("Not Found.");

        assertThrows(ClientException.class, () -> client.get(URI.create("https://example.com")));
    }

    @Test
    void testSend_HandlesHtmlResponseInsteadOfJson() throws Exception {
        mockAuthentication();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(400);
        when(mockResponse.body()).thenReturn("<html><body>Error occurred</body></html>");

        assertThrows(ClientException.class, () -> client.get(URI.create("https://example.com")));
    }

    @Test
    void testSend_HandlesBinaryDataInsteadOfText() throws Exception {
        mockAuthentication();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        whenSuccessfulResponse("\u0001\u0002\u0003\u0004"); // Simulated binary response

        Client.Response response = client.get(URI.create("https://example.com"));

        class Test {}

        assertNotNull(response.getBody());
        assertThrows(ClientException.class, () -> response.asJson(Test.class));
    }

    @Test
    void testSend_MultipleRetriesOnFailure() throws Exception {
        mockAuthentication();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Temporary network failure"))
                .thenThrow(new IOException("Temporary network failure"))
                .thenReturn(mockResponse); // Succeeds on third attempt

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"message\":\"Recovered\"}");

        assertThrows(ClientException.class, () -> client.get(URI.create("https://example.com")));
        assertThrows(ClientException.class, () -> client.get(URI.create("https://example.com")));

        Client.Response response = client.get(URI.create("https://example.com"));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Recovered"));
        verify(mockHttpClient, times(4)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }


    // EXTRA CASES
    @Test
    void testSend_InfiniteRetriesOnTimeout() throws Exception {
        mockAuthentication();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Request timed out"))
                .thenThrow(new IOException("Request timed out"))
                .thenThrow(new IOException("Request timed out"))
                .thenThrow(new IOException("Request timed out")); // Simulate infinite timeout failure

        assertThrows(ClientException.class, () -> client.get(URI.create("https://example.com")));
        verify(mockHttpClient, atMost(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)); // Ensure retries stop at safe limit
    }

    @Test
    void testSend_LargeJsonResponse() throws Exception {
        StringBuilder largePayload = new StringBuilder("{");
        for (int i = 0; i < 100000; i++) {
            largePayload.append("\"key").append(i).append("\":\"value").append(i).append("\",");
        }
        largePayload.append("\"finalKey\":\"finalValue\"}");

        mockAuthentication();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(largePayload.toString());

        Client.Response response = client.get(URI.create("https://example.com"));

        assertTrue(response.getBody().length() > 100000); // Ensure large payloads are handled
    }

    @Test
    void testSend_InvalidContentTypeHeader() throws Exception {
        mockAuthentication();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"message\":\"Success\"}");
        when(mockResponse.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/octet-stream")), (String s1, String s2) -> true)); // Incorrect content type

        assertThrows(ClientException.class, () -> client.get(URI.create("https://example.com")));
    }

    /*@Test
    void testSend_UnstructuredJsonResponse() throws Exception {
        mockAuthentication();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        whenSuccessfulResponse("Success without JSON structure");

        assertThrows(ClientException.class, () -> client.get(URI.create("https://example.com")));
    }*/

}
