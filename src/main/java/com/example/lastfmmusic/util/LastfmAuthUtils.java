package com.example.lastfmmusic.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class LastfmAuthUtils {

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * Build api_sig for Last.fm calls.
     * Parameters should be ordered alphabetically by key.
     * Concatenates <name><value> and appends secret, then MD5.
     */
    public static String generateApiSig(Map<String, String> params, String secret) {
        return md5(params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + e.getValue())
                .reduce("", String::concat) + secret);
    }
}
