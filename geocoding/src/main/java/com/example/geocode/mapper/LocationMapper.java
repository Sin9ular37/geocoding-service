package com.example.geocode.mapper;

import com.example.geocode.entity.Location;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LocationMapper {
    Location findByAllConcatAddress(String address);

    Location findByAddress(String address);

    Location findByAllAddress(@Param("adName")String adName, @Param("streetName")String streetName, @Param("Name")String Name);

    Location findacAddress(@Param("adName")String adName, @Param("streetName")String streetName, @Param("Name")String Name);

    Location findMoteAddress(@Param("Name")String Name);
}
