package com.example.geocode.service.fgeo;

public interface TiandituFgeoService {
    public default String Fgeocode(String lon, String lat) throws Exception {
        return "";
    }
}
