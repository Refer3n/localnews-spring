package com.localnews.localnews.data.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.localnews.localnews.data.dtos.NewsClassification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
public class OpenAIService {

    @Value("${openai.api.key}")
    private String apiKey;

    private WebClient webClient;

    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void initClient() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1/chat/completions")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    public List<NewsClassification> classifyBatch(List<Map<String, String>> newsList) {
        List<NewsClassification> allResults = new ArrayList<>();
        int batchSize = 10;
        int delayMs = 1000;

        for (int start = 0; start < newsList.size(); start += batchSize) {
            int end = Math.min(newsList.size(), start + batchSize);
            List<Map<String, String>> batch = newsList.subList(start, end);
            StringBuilder articlesBlock = new StringBuilder();

            StringJoiner joiner = new StringJoiner(",\n");

            for (Map<String, String> news : batch) {
                String fullTitle = news.getOrDefault("title", "");
                String title = fullTitle.length() > 100 ? fullTitle.substring(0, 100) : fullTitle;

                String fullContent = news.getOrDefault("content", "");
                String content = fullContent.length() > 100 ? fullContent.substring(0, 100) : fullContent;

                String articleJson = String.format("""
                        {
                          "id": %d,
                          "title": "%s",
                          "content": "%s",
                          "location": "%s"
                        }
                        """, Long.parseLong(news.get("id")), escape(title), escape(content), escape(news.getOrDefault("location", "")));

                joiner.add(articleJson);
            }

            articlesBlock.append(joiner);

            Map<String, Object> requestBody = getStringObjectMap(articlesBlock);

            try {
                Map response = webClient.post()
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                Map<String, Object> choice = (Map<String, Object>) ((List<?>) response.get("choices")).get(0);
                Map<String, Object> message = (Map<String, Object>) choice.get("message");
                String content = (String) message.get("content");

                String jsonOnly = extractJsonArray(content);
                JsonNode root = mapper.readTree(jsonOnly);

                for (JsonNode node : root) {
                    NewsClassification nc = new NewsClassification();
                    nc.setId(node.get("id").asLong());
                    nc.setLocal(node.get("isLocal").asBoolean());
                    nc.setCity(node.has("city") && !node.get("city").isNull() ? node.get("city").asText() : null);
                    allResults.add(nc);
                }

                Thread.sleep(delayMs);

            } catch (Exception e) {
                System.err.println("OpenAI call failed for batch [" + start + "-" + end + "]: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return allResults;
    }

    private static Map<String, Object> getStringObjectMap(StringBuilder articlesBlock) {
        String prompt = String.format("""
            Classify the following list of news articles. For each article, determine:
            - If it's local (isLocal: true/false)
            - The city it belongs to (or null if global)
        
            Articles contain a "location" field — a best guess of where the article is from, which may help classification.
        
            Input format (JSON):
            [
              { "id": 0, "title": "...", "content": "...", "location": "..." },
              ...
            ]
        
            Respond ONLY with a JSON array of this format:
            [
              { "id": 0, "isLocal": true, "city": "New York" },
              ...
            ]
        
            Articles:
            [%s]
            """, articlesBlock);

        if(prompt.contains("`")) throw new RuntimeException("code 96");

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.2
        );
        return requestBody;
    }

    private String escape(String input) {
        return input == null ? "" : input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ");
    }

    private String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start == -1 || end == -1 || end <= start) {
            throw new RuntimeException("Could not extract JSON array from response:\n" + text);
        }
        return text.substring(start, end + 1);
    }
}

