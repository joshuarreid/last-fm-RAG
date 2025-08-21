package com.example.lastfmmusic.auth;

import com.example.lastfmmusic.util.LastfmAuthUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class LastfmAuthService {

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    @Value("${lastfm.api.key}")
    private String apiKey;

    @Value("${lastfm.api.secret}")
    private String apiSecret;

    @Value("${lastfm.api.url}")
    private String apiUrl;

    @Value("${lastfm.auth.url}")
    private String authUrl;

    @Value("${lastfm.session.key}")
    private String sessionKey;

    public LastfmAuthService() {
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /** Step 2: Fetch request token */
    public String getRequestToken() throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("method", "auth.getToken");
        params.put("api_key", apiKey);
        params.put("format", "json");
        params.put("api_sig", LastfmAuthUtils.generateApiSig(params, apiSecret));

        HttpUrl url = buildUrl(params);
        Request request = new Request.Builder().url(url).get().build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }
            JsonNode json = objectMapper.readTree(response.body().string());
            return json.path("token").asText();
        }
    }

    /** Step 3: Generate user authorization URL */
    public String getAuthorizationUrl(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        if (authUrl == null || authUrl.isBlank()) {
            throw new IllegalStateException("Authorization URL is not configured");
        }
        try {
            return authUrl + "?api_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                    + "&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build authorization URL", e);
        }
    }

    /** Step 4: Exchange authorized token for session key */
    public void fetchSessionKey(String token) throws IOException {
        // 1. Build params for signature (exclude 'format')
        Map<String, String> paramsForSig = new HashMap<>();
        paramsForSig.put("method", "auth.getSession");
        paramsForSig.put("api_key", apiKey);
        paramsForSig.put("token", token);

        // 2. Generate the API signature
        String apiSig = LastfmAuthUtils.generateApiSig(paramsForSig, apiSecret);

        // 3. Build URL including 'format' (format is NOT part of signature)
        HttpUrl url = HttpUrl.parse(apiUrl).newBuilder()
                .addQueryParameter("method", "auth.getSession")
                .addQueryParameter("api_key", apiKey)
                .addQueryParameter("token", token)
                .addQueryParameter("format", "json")
                .addQueryParameter("api_sig", apiSig)
                .build();

        // 4. Make request
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch session key, code: " + response.code());
            }
            JsonNode json = objectMapper.readTree(response.body().string());
            this.sessionKey = json.path("session").path("key").asText();
            System.out.println("Session Key obtained: " + sessionKey);
        }
    }

    /** Complete authentication workflow */
    public void authenticateDesktopUser() throws IOException {
        String token = getRequestToken();
        System.out.println("Please visit this URL in your browser to authorize:");
        System.out.println(getAuthorizationUrl(token));
        System.out.println("Press Enter once authorization is complete...");
        System.in.read();
        fetchSessionKey(token);
        System.out.println("Session Key: " + getSessionKey());
    }

    /** Get the session key for authenticated API calls */
    public String getSessionKey() {
        return sessionKey;
    }

    private HttpUrl buildUrl(Map<String, String> params) {
        HttpUrl.Builder builder = HttpUrl.parse(apiUrl).newBuilder();
        params.forEach(builder::addQueryParameter);
        return builder.build();
    }


    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }
}
