package com.localnews.localnews.data.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.localnews.localnews.data.dtos.NewsClassification;
import com.localnews.localnews.data.repositories.CityRepository;
import com.localnews.localnews.data.repositories.NewsRepository;
import com.localnews.localnews.models.City;
import com.localnews.localnews.models.News;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Order(2)
public class NewsLoaderService implements ApplicationRunner {
    private final NewsRepository newsRepository;
    private final CityRepository cityRepository;
    private final OpenAIService openAIService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        loadNews();
    }

    @Transactional
    public void loadNews() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/news.json");
        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            JsonNode articles = root.get("articles").get("results");

            List<Map<String, String>> newsList = new ArrayList<>();

            for (JsonNode article : articles) {
                Map<String, String> news = new HashMap<>();
                news.put("id", article.get("uri").asText());
                news.put("title", article.get("title").asText());
                news.put("content", article.get("body").asText());
                news.put("date", article.get("date").asText());
                news.put("image", article.get("image").asText());
                news.put("location", article.get("source").get("location").get("label").get("eng").asText());

                newsList.add(news);
            }

            List<NewsClassification> classifications = openAIService.classifyBatch(newsList);

            Map<Long, NewsClassification> classificationMap = new HashMap<>();
            for (NewsClassification c : classifications) {
                classificationMap.put(c.getId(), c);
            }

            List<News> newsToSave = new ArrayList<>();
            Map<String, City> cityCache = new HashMap<>();

            for (Map<String, String> newsData : newsList) {
                Long id = Long.parseLong(newsData.get("id"));
                NewsClassification classification = classificationMap.get(id);
                if (classification == null) {
                    System.err.println("No classification found for article ID: " + id);
                    continue;
                }

                News article = new News();
                article.setTitle(newsData.get("title"));
                article.setContent(newsData.get("content"));
                article.setPublishedAt(LocalDateTime.parse(newsData.get("date") + "T00:00:00"));
                article.setImageUrl(newsData.get("image"));

                String cityName = classification.getCity();
                if (cityName == null) {
                    article.setLocal(false);
                } else {
                    article.setLocal(true);
                    City city = cityCache.computeIfAbsent(cityName, name ->
                            cityRepository.findByName(name).orElse(null)
                    );
                    article.setCity(city);
                }

                newsToSave.add(article);
            }

            newsRepository.saveAll(newsToSave);
        }
    }
}

