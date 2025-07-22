package com.example.geocode.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "geocoding")
public class GeocodingPriorityConfig {
    private List<String> priority; // 解析为 List

    // Getter/Setter
    public List<String> getPriority() {
        return priority;
    }

    public void setPriority(List<String> priority) {
        this.priority = priority;
    }
}
