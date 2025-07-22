package com.example.geocode.service.fgeo.impl;

import com.example.geocode.config.TiandituConfig;
import com.example.geocode.service.fgeo.TiandituFgeoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class TiandituFgeoServiceImpl implements TiandituFgeoService {
    @Autowired
    private TiandituConfig tiandituConfig;

    @Override
    public String Fgeocode(String lon, String lat) throws Exception {
        // 构建正确的JSON格式
        String jsonDs = String.format("{\"lon\":%s,\"lat\":%s,\"ver\":1}", lon, lat);
        // URL编码
        String encodeDs = java.net.URLEncoder.encode(jsonDs, "UTF-8");
        // 构建完整URL
        String url = String.format("%s?postStr=%s&type=%s&tk=%s",
                tiandituConfig.getGeocodeUrl(),
                encodeDs,
                tiandituConfig.getFgeoType(),
                tiandituConfig.getApiKey());

        System.out.println("逆向地理编码  url = " + url);

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(new HttpGet(url));
             InputStream content = response.getEntity().getContent()) {

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(content);

            System.out.println("原始响应内容: " + root.toString()); // 打印完整响应

            if (root.has("status") && "0".equals(root.get("status").asText())) {
                // 请求成功
                JsonNode resNode = root.path("result");
                System.out.println("formatted_address 内容: " + resNode.toString()); // 打印 locationNode

                if (resNode.isMissingNode()) {
                    System.err.println("返回结果中缺少 'formatAddress' 字段");
                    return "";
                }

                String formatted_address = resNode.has("formatted_address") ? resNode.path("formatted_address").asText() : "";

                return formatted_address;
            } else {
                // 请求失败，输出详细错误信息
                String errorMsg = root.path("msg").asText();
                String errorCode = root.path("status").asText();
                System.err.printf("API 请求失败: 状态码=%s, 错误信息=%s%n", errorCode, errorMsg);
                return "";
            }
        }
    }
}
