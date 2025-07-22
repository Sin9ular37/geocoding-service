package com.example.geocode.service.impl;

import com.example.geocode.entity.ApiResult;
import com.example.geocode.entity.Location;
import com.example.geocode.mapper.LocationMapper;
import com.example.geocode.service.GeocodingService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class LocalServiceImpl implements GeocodingService {
    private final LocationMapper locationMapper;

    public LocalServiceImpl(LocationMapper locationMapper) {
        this.locationMapper = locationMapper;
    }

    public Location searchAllConcatAddress(String address) {
        return locationMapper.findByAllConcatAddress(address);
    }

    public Location searchActuAddress(String adName, String streetName, String Name) {
        return locationMapper.findByAllAddress(adName, streetName, Name);
    }

    public Location searchlocalAddress(String adName, String streetName, String Name){
        return locationMapper.findacAddress(adName, streetName, Name);
    }

    public Location searchMoteAddress(String Name) {
        return locationMapper.findMoteAddress(Name);
    }

    @Override
    @Cacheable(value = "locationCache", key = "#adName + #streetName + #name")
    public ApiResult geocode(String adName, String streetName, String address, String Name) {
        System.out.println("-----------------------------------------------------------------------------------------------------------------");
        System.out.println("localservice starting......");
        System.out.println("    adName = " + adName);
        System.out.println("    streetName = " + streetName);
        System.out.println("    address = " + address);
        System.out.println("    Name = " + Name);
        System.out.println("-----------------------------------------------------------------------------------------------------------------");

        Location  location;
        location = searchlocalAddress(adName, streetName, address);
        if(location == null)
            location = searchActuAddress(adName, streetName, address);
        if(location == null)
            location = searchMoteAddress(address);
        if(location == null)
            return new ApiResult();
        else {
            ApiResult res = new ApiResult(String.valueOf(location.getLng()), String.valueOf(location.getLat()), "");
            res.setFormatted_address(location.getPname() + location.getCityname() +
                    location.getAdname() + location.getStreetname() + location.getName());
            return res;
        }
    }
}
