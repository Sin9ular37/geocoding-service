package com.example.geocode.service;

import com.example.geocode.entity.ApiResult;

public interface GeocodingService {
    default ApiResult geocode(String address) throws Exception {
        return new ApiResult();
    }

    default ApiResult geocode(String adName, String streetName, String address) {
        return new ApiResult();
    }

    default ApiResult geocode(String adName, String streetName, String address, String Name) {
        return new ApiResult();
    }
}
