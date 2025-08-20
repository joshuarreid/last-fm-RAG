package com.example.lastfmmusic;

import com.example.lastfmmusic.entity.Track;
import com.example.lastfmmusic.service.LastfmAuthService;
import com.example.lastfmmusic.service.LastfmService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/lastfm")
public class LastfmTestController {

    private final LastfmAuthService authService;
    private final LastfmService lastfmService;

    public LastfmTestController(LastfmAuthService authService, LastfmService lastfmService) {
        this.authService = authService;
        this.lastfmService = lastfmService;
    }

    /**
     * Trigger desktop user authentication
     */
    @GetMapping("/authenticate")
    public String authenticate() {
        try {
            authService.authenticateDesktopUser();
            return "Authentication complete. Session Key: " + authService.getSessionKey();
        } catch (IOException e) {
            e.printStackTrace();
            return "Authentication failed: " + e.getMessage();
        }
    }

    /**
     * Fetch recent tracks for a given username
     */
    @GetMapping("/recent-tracks")
    public List<Track> getRecentTracks(
            @RequestParam String username,
            @RequestParam(required = false) Integer limit
    ) {
        try {
            return lastfmService.getRecentTracks(username, limit);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch recent tracks: " + e.getMessage());
        }
    }
}
