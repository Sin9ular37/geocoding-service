package com.example.geocode.entity;

import lombok.Data;

@Data
public class Location {
    private int id;              // 主键ID
    private String pname;        // 省份名称
    private String cityname;     // 城市名称
    private String adname;       // 区县名称
    private String businessA;    // 商圈（可能为空）
    private String streetname;   // 乡镇街道（可能为空）
    private String name;         // 具体地点名称
    private Double lng;          // 经度
    private Double lat;          // 纬度

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStreetname() {
        return streetname;
    }

    public void setStreetname(String streetname) {
        this.streetname = streetname;
    }

    public String getPname() {
        return pname;
    }

    public void setPname(String pname) {
        this.pname = pname;
    }

    public String getCityname() {
        return cityname;
    }

    public void setCityname(String cityname) {
        this.cityname = cityname;
    }

    public String getAdname() {
        return adname;
    }

    public void setAdname(String adname) {
        this.adname = adname;
    }

    public String getBusinessA() {
        return businessA;
    }

    public void setBusinessA(String businessA) {
        this.businessA = businessA;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    @Override
    public String toString() {
        return "Location{" +
                "id=" + id +
                ", pname='" + pname + '\'' +
                ", cityname='" + cityname + '\'' +
                ", adname='" + adname + '\'' +
                ", businessA='" + businessA + '\'' +
                ", streetname='" + streetname + '\'' +
                ", name='" + name + '\'' +
                ", lng=" + lng +
                ", lat=" + lat +
                '}';
    }
}