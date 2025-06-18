package io.jenkins.plugins.netrise.asset.uploader.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jenkins.plugins.netrise.asset.uploader.log.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authorise and make requests to the NetRise API
 * */
public class Client {
    private static final Logger log = Logger.getLogger(Client.class);

    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String CONTENT_TYPE_HEADER = "Content-Type";

    public static final String APP_JSON_CONTENT_TYPE = "application/json";

    public static final String APP_TEXT_CONTENT_TYPE = "text/";

    public static final String GRANT_TYPE = "client_credentials";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final TokenRequest tokenRequest;

    private TokenInstance tokenInstance;

    private final URI tokenUri;

    /**
     * Configure the API Client
     *
     * @param tokenUri The authentication url
     * @param organization Organization ID
     * @param clientId Client ID
     * @param clientSecret Client Secret
     * @param audience Audience
     * */
    public Client(URI tokenUri, String organization, String clientId, String clientSecret, String audience) {
        this.tokenUri = tokenUri;
        tokenRequest = new TokenRequest(
                organization,
                clientId,
                clientSecret,
                GRANT_TYPE,
                audience
        );
    }

    protected HttpRequest.Builder getRequestBuilder(URI uri) {
        return HttpRequest.newBuilder()
                .uri(uri);
    }

    private HttpRequest.Builder getAuthenticatedRequestBuilder(URI uri, Map<String, Object> headers) {
        HttpRequest.Builder builder = getRequestBuilder(uri);
        if (headers != null) {
            for (Map.Entry<String, Object> e: headers.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    builder.header(e.getKey(), e.getValue().toString());
                }
            }
        }
        try {
            TokenInstance auth = this.authenticate();

            builder.header(AUTHORIZATION_HEADER, auth.getAccessToken());
        } catch (ClientException e) {
            throw new AuthException("Authentication error.", e);
        }
        return builder;
    }

    private HttpRequest.Builder getAuthenticatedRequestBuilder(URI uri) {
        return getAuthenticatedRequestBuilder(uri, null);
    }

    /**
     * GET request
     * Can throw {@link ClientException} if there is some network error or IOException or {@link AuthException} if there is an authentication error
     *
     * @param uri The url
     *
     * @return Response wrapper
     * */
    public Response get(URI uri) {
        HttpRequest request = getAuthenticatedRequestBuilder(uri)
                .GET()
                .build();

        return send(request);
    }

    /**
     * POST request
     * Can throw {@link ClientException} if there is some network error or IOException or {@link AuthException} if there is an authentication error
     *
     * @param uri The url
     * @param data Payload
     *
     * @throws ClientException if there is an error
     * @return Response wrapper
     * */
    public Response post(URI uri, Object data) {
        HttpRequest request = getAuthenticatedRequestBuilder(uri, Map.of(CONTENT_TYPE_HEADER, APP_JSON_CONTENT_TYPE))
                .POST(HttpRequest.BodyPublishers.ofString(toJson(data)))
                .build();

        return send(request);
    }

    /**
     * PUT request to upload the file
     * Can throw {@link ClientException} if there is some network error or IOException or {@link AuthException} if there is an authentication error
     *
     * @param uri The url
     * @param path The path to the file
     *
     * @return Response wrapper
     * */
    public Response upload(URI uri, Path path) {
        HttpRequest request;
        try {
            request = getAuthenticatedRequestBuilder(uri)
                    .PUT(HttpRequest.BodyPublishers.ofFile(path))
                    .build();
        } catch (FileNotFoundException e) {
            log.error(e.getLocalizedMessage(), e);
            throw new ClientException("File processing error: " + path, e);
        }

        return send(request);
    }

    /**
     * Send configured request and process the result
     * Can throw {@link ClientException} if there is some network error or IOException or {@link AuthException} if there is an authentication error
     *
     * @param request Configured request
     *
     * @return Response wrapper
     * */
    protected Response send(HttpRequest request) {
        Response response;

        log.debug("Send ", request.method(), " request to ", request.uri());

        try {
            response = new Response(
                    getHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
            );
            if (response.getStatusCode() >= 400) {
                if (response.isJson()) {
                    Error error = response.asJson(Error.class);
                    log.error("Error", response.getStatusCode(), error.error(), error.description());
                    throw new ClientException(error.error(), error.description());
                } else if (response.isText()) {
                    log.error("Error", response.getStatusCode(), response.getBody());
                    throw new ClientException(response.getBody());
                } else {
                    log.error("Unknown Error", response.getStatusCode(), response.getBody());
                    throw new ClientException("Unknown error.");
                }
            } else if (!(response.isJson() || response.isText())) {
                throw new ClientException("Invalid content type.", response.getHeader(CONTENT_TYPE_HEADER));
            }
        } catch (IOException | InterruptedException e) {
            throw new ClientException("Request sending error.", e);
        }

        log.debug("Request to ", request.uri(), " completed.");

        return response;
    }

    protected HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Make an authentication call to the API
     *
     * @return Wrapper with token and if it is valid
     * */
    public TokenInstance authenticate() {
        return authenticate(tokenRequest);
    }

    private TokenInstance authenticate(TokenRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("'request' should be defined.");
        }

        if (tokenInstance != null && tokenInstance.isValid()) {
            return tokenInstance;
        }

        log.debug("Authentication started");

        HttpRequest req = getRequestBuilder(tokenUri)
                .header(CONTENT_TYPE_HEADER, APP_JSON_CONTENT_TYPE)
                .POST(HttpRequest.BodyPublishers.ofString(toJson(request)))
                .build();

        TokenResponse response = send(req).asJson(TokenResponse.class);

        if (response == null) {
            throw new AuthException("Access token is not valid.");
        }

        tokenInstance = new TokenInstance(response);

        if (!tokenInstance.isValid()) {
            throw new AuthException("Access token is not valid.");
        }

        log.debug("Authentication completed");

        return tokenInstance;
    }

    private <T> String toJson(T data) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new ClientException("JSON stringify error.", e.getLocalizedMessage());
        }
    }

    /**
     * Helper that wraps the HTTP response
     * */
    public static class Response {
        enum ContentType {JSON, TEXT, UNKNOWN}

        private final Map<String, List<String>> headers = new HashMap<>();
        private final int statusCode;
        private final String body;
        private ContentType contentType;

        public Response(HttpResponse<String> response) {
            response.headers().map().forEach((k, v) -> {
                headers.put(k, v);
                if (CONTENT_TYPE_HEADER.equalsIgnoreCase(k) && v != null && !v.isEmpty()) {
                    setContentType(v.get(0));
                }
            });
            if (contentType == null) {
                contentType = ContentType.UNKNOWN;
            }
            this.statusCode = response.statusCode();
            this.body = response.body();
        }

        private void setContentType(String contentType) {
            if (contentType != null) {
                contentType = contentType.toLowerCase();
                if (contentType.contains(APP_JSON_CONTENT_TYPE)) {
                    this.contentType = ContentType.JSON;
                } else if (contentType.contains(APP_TEXT_CONTENT_TYPE)) {
                    this.contentType = ContentType.TEXT;
                } else {
                    this.contentType = ContentType.UNKNOWN;
                }
            }
        }

        /**
         * Return response headers as a map
         * */
        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        /**
         * Return response status code
         * */
        public int getStatusCode() {
            return statusCode;
        }

        /**
         * Return response header by name
         * */
        public String getHeader(String name){
            String value = null;
            if(headers.containsKey(name)){
                List<String> values = headers.get(name);
                if(values != null && !values.isEmpty()) {
                    value = values.get(0);
                }
            }

            return value;
        }

        /**
         * Return response body
         * */
        public String getBody() {
            return body;
        }

        /**
         * Return true if content-type is text/plain
         * */
        public boolean isText() {
            return ContentType.TEXT.equals(this.contentType);
        }

        /**
         * Return true if content-type is application/json
         * */
        public boolean isJson() {
            return ContentType.JSON.equals(this.contentType);
        }

        /**
         * Return response body parsed as JSON and constructed as Java object of the provided class
         * */
        public <T> T asJson(Class<T> clz) {
            if (body == null) {
                return null;
            }

            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            try {
                return mapper.readValue(body, clz);
            } catch (JsonProcessingException e) {
                throw new ClientException("JSON parse error.", e.getLocalizedMessage());
            }
        }

        /**
         * Return response body parsed as JSON and constructed as Java object of the provided class
         * */
        public <T> T asJson(TypeReference<T> typeReference) {
            if (body == null) {
                return null;
            }

            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            try {
                return mapper.readValue(body, typeReference);
            } catch (JsonProcessingException e) {
                throw new ClientException("JSON parse error.", e.getLocalizedMessage());
            }
        }

        @Override
        public String toString() {
            return statusCode +
                    ":\n" + body;
        }
    }

    /**
     * Authentication data wrapper that checks token validity
     * */
    public static class TokenInstance {
        private static final long timeUnits = 1000;

        private final long time = System.currentTimeMillis();
        private final TokenResponse response;

        public TokenInstance(TokenResponse response) {
            this.response = response;
        }

        protected TokenResponse getResponse() {
            return response;
        }

        /**
         * Return true if token is defined and not expired
         * */
        public boolean isValid() {
            return response != null && response.accessToken() != null && response.tokenType() != null && response.expiresIn() != null
                    && (System.currentTimeMillis() - time < response.expiresIn() * timeUnits);
        }

        /**
         * Return token in format '{token type} {access token}' (for example 'Bearer ASDFAEWEF12DS')
         * */
        public String getAccessToken() {
            return response.tokenType() + " " + response.accessToken();
        }

        @Override
        public String toString() {
            return "token(" + getAccessToken() +
                    ") is valid - " + isValid();
        }
    }
}
