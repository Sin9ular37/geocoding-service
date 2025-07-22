package com.example.geocode.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GaodeConfig {
    @Value("${gaode.api.url}")
    private String geocodeUrl;

    @Value("${gaode.api.key}")
    private String apiKey;

    @Value("${gaode.api.citycode}")
    private String cityCode;

    public String getGeocodeUrl() {
        return geocodeUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getCityCode() {
        return cityCode;
    }
}
