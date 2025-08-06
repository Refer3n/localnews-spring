package com.localnews.localnews.web.controllers;

import com.localnews.localnews.data.dtos.PagedResponse;
import com.localnews.localnews.data.repositories.CityRepository;
import com.localnews.localnews.data.repositories.NewsRepository;
import com.localnews.localnews.models.City;
import com.localnews.localnews.models.News;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final CityRepository cityRepository;
    private final NewsRepository newsRepository;

    @GetMapping
    public PagedResponse<News> getNews(@RequestParam(required = false, defaultValue = "0") Integer page,
                                       @RequestParam(required = false, defaultValue = "10") Integer limit,
                                       @RequestParam(required = false) String cityName) {

        Pageable pageable = PageRequest.of(page, limit);
        Page<News> newsPage;

        if (cityName == null || cityName.isBlank()) {
            newsPage = newsRepository.findByIsLocalFalse(pageable);
        } else {
            Optional<City> city = cityRepository.findByName(cityName);
            newsPage = city.map(c -> newsRepository.findByCity(c, pageable))
                    .orElse(Page.empty(pageable));
        }

        return new PagedResponse<>(
                newsPage.getContent(),
                newsPage.getNumber(),
                newsPage.getSize(),
                newsPage.getTotalElements(),
                newsPage.getTotalPages()
        );
    }
}
