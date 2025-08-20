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
import java.util.List;

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

    /**
     * Fetch recent tracks for a user.
     * Uses session key from LastfmAuthService for authenticated requests.
     */
    public List<Track> getRecentTracks(String username, Integer limit) throws IOException {
        if (limit == null) limit = defaultFetchLimit;

        // Ensure user is authenticated
        String sessionKey = authService.getSessionKey();
        if (sessionKey == null) {
            throw new IllegalStateException("User is not authenticated. Call authenticateDesktopUser() first.");
        }

        HttpUrl url = HttpUrl.parse(apiUrl).newBuilder()
                .addQueryParameter("method", "user.getrecenttracks")
                .addQueryParameter("user", username)
                .addQueryParameter("api_key", authService.getApiKey())
                .addQueryParameter("sk", sessionKey)
                .addQueryParameter("format", "json")
                .addQueryParameter("limit", String.valueOf(limit))
                .addQueryParameter("api_sig", generateApiSigForRecentTracks(username, limit, sessionKey))
                .build();

        Request request = new Request.Builder().url(url).get().build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }

            JsonNode root = objectMapper.readTree(response.body().string());
            JsonNode tracksNode = root.path("recenttracks").path("track");

            List<Track> tracks = new ArrayList<>();
            for (JsonNode node : tracksNode) {
                Track track = Track.builder()
                        .trackName(node.path("name").asText())
                        .artistName(node.path("artist").path("#text").asText())
                        .albumName(node.path("album").path("#text").asText())
                        .mbid(node.path("mbid").asText())
                        .build();
                tracks.add(track);
            }
            return tracks;
        }
    }

    /**
     * Generate API signature for authenticated request.
     */
    private String generateApiSigForRecentTracks(String username, int limit, String sessionKey) {
        // Build parameter map
        var params = new java.util.HashMap<String, String>();
        params.put("api_key", authService.getApiKey());
        params.put("method", "user.getrecenttracks");
        params.put("sk", sessionKey);
        params.put("user", username);
        params.put("limit", String.valueOf(limit));
        return LastfmAuthUtils.generateApiSig(params, authService.getApiSecret());
    }
}