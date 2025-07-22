package com.example.geocode.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TiandituConfig {
    @Value("${tianditu.api.url}")
    private String geocodeUrl;
    @Value("${tianditu.api.key}")
    private String apiKey;
    @Value("${tianditu.api.fgeocode.type}")
    private String fgeoType;

    public String getGeocodeUrl() {
        return geocodeUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getFgeoType() {
        return fgeoType;
    }
}
