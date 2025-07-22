package com.example.geocode.entity;

public class ApiResult {
    private String location;
    private String lng;
    private String lat;
    private String provinde;
    private String city;
    private String district;
    private String level;
    private String source;
    private String address;
    private String formatted_address;

    public ApiResult(String location, String provinde, String city, String district, String level) {
        this.location = location;
        this.provinde = provinde;
        this.city = city;
        this.district = district;
        this.level = level;
        if (location != null){
            this.lng = location.split(",")[0];
            this.lat = location.split(",")[1];
        }
    }

    public ApiResult(String lng, String lat) {
        this.lng = lng;
        this.lat = lat;
    }

    public ApiResult(String lng, String lat, String level) {
        this.level = level;
        this.lng = lng;
        this.lat = lat;
    }

    public ApiResult() {
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLocation() {
        return location;
    }

    public String getLng() {
        return lng;
    }

    public String getLat() {
        return lat;
    }

    public String getProvinde() {
        return provinde;
    }

    public String getCity() {
        return city;
    }

    public String getDistrict() {
        return district;
    }

    public String getAddress() {
        return address;
    }

    public String getLevel() {
        return level;
    }

    public String getFormatted_address() {
        return formatted_address;
    }

    public void setFormatted_address(String formatted_address) {
        this.formatted_address = formatted_address;
    }

    @Override
    public String toString() {
        return "ApiResult{" +
                "location='" + location + '\'' +
                ", lng='" + lng + '\'' +
                ", lat='" + lat + '\'' +
                ", provinde='" + provinde + '\'' +
                ", city='" + city + '\'' +
                ", district='" + district + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
