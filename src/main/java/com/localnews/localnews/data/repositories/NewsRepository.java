package com.localnews.localnews.data.repositories;

import com.localnews.localnews.models.City;
import com.localnews.localnews.models.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsRepository extends JpaRepository<News, Long> {
    Page<News> findByCity(City city, Pageable pageable);
    Page<News> findByIsLocalFalse(Pageable pageable);
}
