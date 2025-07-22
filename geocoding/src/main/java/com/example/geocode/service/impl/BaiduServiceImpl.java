package com.example.geocode.service.impl;


import com.example.geocode.config.BaiduConfig;
import com.example.geocode.entity.ApiResult;
import com.example.geocode.service.GeocodingService;
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
public class BaiduServiceImpl implements GeocodingService {
    @Autowired
    private BaiduConfig baiduConfig;

    @Override
    public ApiResult geocode(String address) throws Exception {
        String url = String.format("%s?address=%s&city=%s&output=%s&ak=%s&ret_coordtype=%s",
                baiduConfig.getUrl(),
                java.net.URLEncoder.encode(address, "UTF-8"),
                java.net.URLEncoder.encode(baiduConfig.getCity(), "UTF-8"),
                baiduConfig.getOutput(),
                baiduConfig.getApiKey(),
                baiduConfig.getRet_coordtype());

        System.out.println("安全url" + url);

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(new HttpGet(url));
             InputStream content = response.getEntity().getContent()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(content);

            System.out.println("原始响应内容: " + root.toString());

            if (root.has("status") && "0".equals(root.get("status").asText())) {
                JsonNode res = root.path("result").get("location");
                System.out.println("location 内容: " + res.toString());

                if(res.isMissingNode()){
                    System.err.println("返回结果中缺少 'location' 字段");
                    return new ApiResult();
                }

                String lng = res.has("lng") ? res.get("lng").asText() : "";
                String lat = res.has("lat") ? res.get("lat").asText() : "";

                return new ApiResult(lng, lat);
            }else{
                // 请求失败，输出详细错误信息
                String errorMsg = root.path("msg").asText();
                String errorCode = root.path("status").asText();
                System.err.printf("API 请求失败: 状态码=%s, 错误信息=%s%n", errorCode, errorMsg);
                return new ApiResult();
            }
        }
    }
}
