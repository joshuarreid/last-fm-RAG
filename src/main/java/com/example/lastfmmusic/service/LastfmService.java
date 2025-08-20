package com.example.lastfmmusic.service;

import com.example.lastfmmusic.entity.Track;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LastfmService {

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final LastfmAuthService authService;

    @Value("${lastfm.api.url:https://ws.audioscrobbler.com/2.0/}")
    private String apiUrl;

    @Value("${app.default.fetch.limit:50}")
    private int defaultFetchLimit;

    public LastfmService(LastfmAuthService authService) {
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.authService = authService;
    }

    /** -----------------------------
     * Public API methods
     * ----------------------------- */

    public List<Track> getRecentTracks(String username, Integer limit) throws IOException {
        if (limit == null) limit = defaultFetchLimit;

        String sessionKey = requireSessionKey();

        Map<String, String> params = new HashMap<>();
        params.put("method", "user.getrecenttracks");
        params.put("user", username);
        params.put("api_key", authService.getApiKey());
        params.put("sk", sessionKey);
        params.put("format", "json");
        params.put("limit", String.valueOf(limit));
        params.put("api_sig", generateApiSig(params));

        HttpUrl url = buildLastfmUrl(params);
        JsonNode root = executeGetRequest(url);
        return parseTracks(root.path("recenttracks").path("track"));
    }

    public List<Track> getLovedTracks(String username, Integer limit) throws IOException {
        if (limit == null) limit = defaultFetchLimit;

        String sessionKey = requireSessionKey();

        Map<String, String> params = new HashMap<>();
        params.put("method", "user.getlovedtracks");
        params.put("user", username);
        params.put("api_key", authService.getApiKey());
        params.put("sk", sessionKey);
        params.put("format", "json");
        params.put("limit", String.valueOf(limit));
        params.put("api_sig", generateApiSig(params));

        HttpUrl url = buildLastfmUrl(params);
        JsonNode root = executeGetRequest(url);
        return parseTracks(root.path("lovedtracks").path("track"));
    }

    /** -----------------------------
     * Reusable private helpers
     * ----------------------------- */

    private String requireSessionKey() {
        String key = authService.getSessionKey();
        if (key == null) throw new IllegalStateException("User is not authenticated. Call authenticateDesktopUser() first.");
        return key;
    }

    private HttpUrl buildLastfmUrl(Map<String, String> params) {
        HttpUrl.Builder builder = HttpUrl.parse(apiUrl).newBuilder();
        params.forEach(builder::addQueryParameter);
        return builder.build();
    }

    private JsonNode executeGetRequest(HttpUrl url) throws IOException {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }
            return objectMapper.readTree(response.body().string());
        }
    }

    private List<Track> parseTracks(JsonNode tracksNode) {
        List<Track> tracks = new ArrayList<>();
        for (JsonNode node : tracksNode) {
            tracks.add(Track.builder()
                    .trackName(node.path("name").asText())
                    .artistName(node.path("artist").path("#text").asText())
                    .albumName(node.path("album").path("#text").asText())
                    .mbid(node.path("mbid").asText())
                    .build());
        }
        return tracks;
    }

    private String generateApiSig(Map<String, String> params) {
        // Exclude 'format' if present in signature calculation
        Map<String, String> signatureParams = new HashMap<>(params);
        signatureParams.remove("format");
        return LastfmAuthUtils.generateApiSig(signatureParams, authService.getApiSecret());
    }
}