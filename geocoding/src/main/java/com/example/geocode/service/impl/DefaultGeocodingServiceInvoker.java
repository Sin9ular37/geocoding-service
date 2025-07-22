package com.example.geocode.service.impl;

import com.example.geocode.config.GeocodingPriorityConfig;
import com.example.geocode.entity.ApiResult;
import com.example.geocode.service.GeocodingService;
import com.example.geocode.service.GeocodingServiceInvoker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DefaultGeocodingServiceInvoker implements GeocodingServiceInvoker {

    private final GeocodingPriorityConfig priorityConfig;
    private final Map<String, GeocodingService> geocodingServices; // 服务名称 -> 实例

    @Autowired
    public DefaultGeocodingServiceInvoker(GeocodingPriorityConfig priorityConfig, List<GeocodingService> services) {
        this.priorityConfig = priorityConfig;
        this.geocodingServices = services.stream()
                .collect(Collectors.toMap(
                        s -> s.getClass().getSimpleName().replace("ServiceImpl", "").toLowerCase(),
                        s -> s));
    }

    @Override
    public ApiResult geocode(String name, String adName, String streetName, String address) throws Exception {
        if (geocodingServices == null || geocodingServices.isEmpty()) {
            throw new IllegalStateException("未注册任何地理编码服务");
        }

        for (String serviceName : priorityConfig.getPriority()) {
            GeocodingService service = geocodingServices.get(serviceName.toLowerCase()); // 统一转小写
            if (service == null) {
                System.err.println("警告：未找到服务 " + serviceName);
                continue;
            }

            ApiResult result;
            if ("local".equals(serviceName))
                result = service.geocode(adName, streetName, address, name);
            else
                result = service.geocode(name);

            if (result != null && result.getLng() != null && result.getLat() != null){
                result.setSource(serviceName.replace("ServiceImpl", "").toLowerCase());
                return result;
            }
        }
        return null;
    }
}
