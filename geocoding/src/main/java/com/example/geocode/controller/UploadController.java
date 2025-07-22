package com.example.geocode.controller;

import com.example.geocode.service.GeocodingService;
import com.example.geocode.service.impl.TripleFileServiceImpl;
import com.example.geocode.service.impl.fileservice.RenewFileServiceImpl;
import org.apache.poi.util.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/v2/api")
public class UploadController {

    private final RenewFileServiceImpl fileService;

    public UploadController(RenewFileServiceImpl fileService) {
        this.fileService = fileService;
    }

//    private final TripleFileServiceImpl fileService;
//    private final List<GeocodingService> geocodingServices; // 自动装配所有实现类
//
//    public UploadController(TripleFileServiceImpl fileService, List<GeocodingService> geocodingServices) {
//        this.fileService = fileService;
//        this.geocodingServices = geocodingServices;
//    }

    @PostMapping("/upload")
    public ResponseEntity<byte[]> uploadFile(MultipartFile file) throws Exception {

        IOUtils.setByteArrayMaxOverride(200000000);

        byte[] bytes = fileService.processExcel(file); // renewFileServiceImpl
//        byte[] bytes = fileService.processExcel(file, geocodingServices);// tripleFileServiceImpl

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=result.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

}
