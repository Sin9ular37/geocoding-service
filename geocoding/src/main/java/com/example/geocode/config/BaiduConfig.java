package com.example.geocode.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BaiduConfig {
    @Value("${baidu.api.url}")
    private String url;

    @Value("${baidu.api.key}")
    private String apiKey;

    @Value("${baidu.api.city}")
    private String city;

    @Value("${baidu.api.ret_coordtype}")
    private String ret_coordtype;

    @Value("${baidu.api.output}")
    private String output;

    public String getUrl() {
        return url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getCity() {
        return city;
    }

    public String getRet_coordtype() {
        return ret_coordtype;
    }

    public String getOutput() {
        return output;
    }
}
