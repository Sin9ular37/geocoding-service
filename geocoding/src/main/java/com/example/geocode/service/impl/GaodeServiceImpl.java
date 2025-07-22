package com.example.geocode.service.impl;

import com.example.geocode.config.GaodeConfig;
import com.example.geocode.entity.ApiResult;
import com.example.geocode.service.GeocodingService;
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
public class GaodeServiceImpl implements GeocodingService {
    @Autowired
    private GaodeConfig gaodeConfig;

    @Override
    public ApiResult geocode(String address) throws Exception {
        String url = String.format("%s?address=%s&city=%s&key=%s",
                gaodeConfig.getGeocodeUrl(),
                java.net.URLEncoder.encode(address, "UTF-8"),
                gaodeConfig.getCityCode(),
                gaodeConfig.getApiKey());

        System.out.println("安全URL: " + url); // 调试输出

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(new HttpGet(url));
             InputStream content = response.getEntity().getContent()) {

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(content);

            System.out.println("原始响应内容: " + root.toString()); // 打印完整响应

            if (root.has("status") && root.path("status").asText().equals("1")) {
                JsonNode res = root.path("geocodes").get(0);
                System.out.println("geocodes 内容: " + res.toString()); //  打印geocodes

                if(res.isMissingNode()){
                    System.err.println("返回结果中缺少 'geocodes' 字段");
                    return new ApiResult();
                }

                String locat = res.has("location") ? res.get("location").asText() : "";
                String lev = res.has("level") ? res.get("level").asText() : "";
                if(lev.equals("乡镇"))
                    lev = "乡镇街道";
                String formatted_address = res.has("formatted_address") ? res.get("formatted_address").asText() : "";

                ApiResult resultData = new ApiResult(locat, res.path("province").asText(),
                        res.path("city").asText(),
                        res.path("district").asText(), lev);

                String res_formatted_address = "";
                if(lev.equals("区县")){
                    res_formatted_address = TranAddress.tranAddressAdname(formatted_address);
                    resultData.setFormatted_address(res_formatted_address);
                }else if(lev.equals("乡镇街道")){
                    res_formatted_address = TranAddress.tranAddressAcuStreet(formatted_address);
                    resultData.setFormatted_address(res_formatted_address);
                }else {
                    resultData.setFormatted_address(formatted_address);
                }

                return resultData;
            }else{
                // 请求失败，输出详细错误信息
                String errorMsg = root.path("info").asText();
                String errorCode = root.path("status").asText();
                System.err.printf("API 请求失败: 状态码=%s, 错误信息=%s%n", errorCode, errorMsg);
                return new ApiResult();
            }
        }
    }
}
