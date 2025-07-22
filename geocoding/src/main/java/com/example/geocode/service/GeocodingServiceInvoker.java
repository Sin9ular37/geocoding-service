package com.example.geocode.service;

import com.example.geocode.entity.ApiResult;

public interface GeocodingServiceInvoker {
    ApiResult geocode(String name, String adName, String streetName, String address) throws Exception;
}
