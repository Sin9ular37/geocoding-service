package com.example.geocode.service.impl;

import com.example.geocode.config.TiandituConfig;
import com.example.geocode.entity.ApiResult;
import com.example.geocode.entity.Location;
import com.example.geocode.service.GeocodingService;
import com.example.geocode.service.fgeo.TiandituFgeoService;
import com.example.geocode.utils.TranAddress;
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
public class TiandituServiceImpl implements GeocodingService {
    @Autowired
    private TiandituConfig tiandituConfig;

    @Autowired
    private TiandituFgeoService tiandituFgeoService;

    @Override
    public ApiResult geocode(String address) throws Exception {
        // 1. 构建完整的 JSON 字符串
        String jsonDs = String.format("{\"keyWord\":\"%s\"}", address);
        // 2. 对整个 JSON 进行 URL 编码（UTF-8）
        String encodedDs = java.net.URLEncoder.encode(jsonDs, "UTF-8");
        // 3. 构造安全的 URL
        String url = String.format("%s?tk=%s&ds=%s",
                tiandituConfig.getGeocodeUrl(),
                tiandituConfig.getApiKey(),
                encodedDs); // 编码后的 ds 参数

        System.out.println("安全URL: " + url); // 调试输出

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(new HttpGet(url));
             InputStream content = response.getEntity().getContent()) {

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(content);

            System.out.println("原始响应内容: " + root.toString()); // 打印完整响应

            if (root.has("status") && "0".equals(root.get("status").asText())) {
                // 请求成功
                JsonNode locationNode = root.path("location");
                System.out.println("locationNode 内容: " + locationNode.toString()); // 打印 locationNode

                if (locationNode.isMissingNode()) {
                    System.err.println("返回结果中缺少 'location' 字段");
                    return new ApiResult();
                }

                String lon = locationNode.has("lon") ? locationNode.get("lon").asText() : "";
                String lat = locationNode.has("lat") ? locationNode.get("lat").asText() : "";
                String lev = locationNode.has("level") ? locationNode.get("level").asText() : "";
                if(lev.equals("区县及以上级行政区划"))
                    lev = "区县";
                String formatted_address = tiandituFgeoService.Fgeocode(lon, lat);

                ApiResult res = new ApiResult(lon, lat, lev);

                String res_formatted_address = "";
                if(lev.equals("区县")){
                    res_formatted_address = TranAddress.tranAddressAdname(formatted_address);
                    res.setFormatted_address(res_formatted_address);
                }else if(lev.equals("乡镇街道")){
                    res_formatted_address = TranAddress.tranAddressAcuStreet(formatted_address);
                    res.setFormatted_address(res_formatted_address);
                }else {
                    res.setFormatted_address(formatted_address);
                }

                return res;
            } else {
                // 请求失败，输出详细错误信息
                String errorMsg = root.path("msg").asText();
                String errorCode = root.path("status").asText();
                System.err.printf("API 请求失败: 状态码=%s, 错误信息=%s%n", errorCode, errorMsg);
                return new ApiResult();
            }
        }
    }
}
