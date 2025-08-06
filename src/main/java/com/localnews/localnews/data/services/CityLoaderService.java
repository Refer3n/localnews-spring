package com.localnews.localnews.data.services;

import com.localnews.localnews.data.repositories.CityRepository;
import com.localnews.localnews.data.repositories.StateRepository;
import com.localnews.localnews.models.City;
import com.localnews.localnews.models.State;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Order(1)
public class CityLoaderService implements ApplicationRunner {
    private final CityRepository cityRepository;
    private final StateRepository stateRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        loadCites();
    }

    @Transactional
    public void loadCites() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/uscities.csv");

        List<String> lines;
        try (var inputStream = resource.getInputStream();
             var reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream))) {
            lines = reader.lines().toList();
        }

        if (!lines.isEmpty()) {
            lines = lines.subList(1, lines.size());
        }

        Map<String, State> stateCache = new HashMap<>();
        List<City> citiesToSave = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String line : lines) {
            String[] parts = line.split(",");

            String cityName = parts[0].trim().replace("\"", "");
            String stateName = parts[3].trim().replace("\"", "");

            if (!seen.add(cityName)) continue;

            State state = stateCache.get(stateName);
            if (state == null) {
                state = stateRepository.findByName(stateName).orElse(null);
                if (state == null) {
                    state = new State();
                    state.setName(stateName);
                    state = stateRepository.save(state);
                }
                stateCache.put(stateName, state);
            }

            City city = new City();
            city.setName(cityName);
            city.setState(state);
            citiesToSave.add(city);
        }

        cityRepository.saveAll(citiesToSave);
    }
}
