package com.example.lastfmmusic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;

@Configuration
public class AiConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Bean
    public EmbeddingModel embeddingModel() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing spring.ai.openai.api-key in properties");
        }

        OpenAiApi api = new OpenAiApi(apiKey);
        return new OpenAiEmbeddingModel(api);
    }
}
