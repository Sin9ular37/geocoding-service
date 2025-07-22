package com.example.geocode.service;

import com.example.geocode.service.impl.GaodeServiceImpl;
import com.example.geocode.service.impl.LocalServiceImpl;
import com.example.geocode.service.impl.TiandituServiceImpl;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ExcelFileService {
//    public default byte[] processExcel(MultipartFile file, LocationService locationService) throws IOException {
//        return new byte[0];
//    }
//
//    public default byte[] processExcel(MultipartFile file, GeocodeService geocodeService) throws IOException {
//        return new byte[0];
//    }

    public default byte[] processExcel(MultipartFile file, LocalServiceImpl locationService, GaodeServiceImpl geocodeService, TiandituServiceImpl tiandituServiceImpl) throws Exception {
        return new byte[0];
    }

    public default byte[] processExcel(MultipartFile file, List<GeocodingService> services) throws Exception {
        return new byte[0];
    }

    public default byte[] processExcel(MultipartFile file) throws Exception{
        return new byte[0];
    }
}
