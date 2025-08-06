package com.localnews.localnews.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode

@Entity
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    private String title;

    @Lob
    private String content;

    private LocalDateTime publishedAt;

    private String imageUrl;

    private boolean isLocal;

    @ManyToOne
    @JoinColumn(name = "city_id")
    private City city;
}
