package com.example.geocode.controller;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
public class DownloadController {

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        // 创建Excel工作簿
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            XSSFSheet sheet = workbook.createSheet("模板");
            
            // 创建标题行
            XSSFRow headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("序号");
            headerRow.createCell(1).setCellValue("区县");
            headerRow.createCell(2).setCellValue("乡镇街道");
            headerRow.createCell(3).setCellValue("地名地址");

            workbook.write(out);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "template.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(out.toByteArray());
        }
    }
}