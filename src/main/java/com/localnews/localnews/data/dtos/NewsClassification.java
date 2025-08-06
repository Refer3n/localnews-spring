package com.localnews.localnews.data.dtos;

import lombok.Data;

@Data
public class NewsClassification {
    private Long id;
    private boolean isLocal;
    private String city;
}

