package com.example.geocode.service.impl;


import com.example.geocode.entity.ApiResult;
import com.example.geocode.service.ExcelFileService;
import com.example.geocode.service.GeocodingService;
import com.example.geocode.service.GeocodingServiceInvoker;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class TripleFileServiceImpl implements ExcelFileService {

    private final GeocodingServiceInvoker geocodingServiceInvoker;
    private final QuotaService quotaService;

    @Autowired
    public TripleFileServiceImpl(GeocodingServiceInvoker geocodingServiceInvoker, QuotaService quotaService) {
        this.geocodingServiceInvoker = geocodingServiceInvoker;
        this.quotaService = quotaService;
    }

    @Override
    public byte[] processExcel(MultipartFile file, List<GeocodingService> services) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.getSheetAt(0);
            List<String[]> results = new ArrayList<>();

            int countGaodeNum = 0, countTiandituNum = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                XSSFRow row = sheet.getRow(i);

                // 检查整行是否为空
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                String[] data = new String[8];

                //序号
                data[0] = getCellValue(row.getCell(0));
                //区县
                data[1] = getCellValue(row.getCell(1));
                String adName = data[1];
                //乡镇街道
                data[2] = getCellValue(row.getCell(2));
                String streetName = data[2];
                //地名地址
                data[3] = getCellValue(row.getCell(3));
                String address = data[3];
                String name = "哈尔滨市" + adName + streetName + address;

                // 检查地址是否有效
                if (address == null || address.trim().isEmpty()) {
                    data[4] = "";
                    data[5] = "";
                    results.add(data);
                    continue;
                }

                ApiResult result = geocodingServiceInvoker.geocode(name, adName, streetName, address);

                if (result != null && result.getLng() != null && result.getLat() != null) {
                    data[4] = result.getLng();
                    data[5] = result.getLat();
                    data[6] = result.getSource(); // 来源名称
                    data[7] = result.getLevel();
                    if("gaode".equals(result.getSource()))
                        countGaodeNum ++;
                    if ("tianditu".equals(result.getSource()))
                        countTiandituNum ++;
                } else {
                    data[4] = "";
                    data[5] = "";
                }
                results.add(data);
            }

            quotaService.useCounter(countGaodeNum);
            quotaService.useCounterDaily(countTiandituNum);

            // 生成新文件
            return createResultFile(results);
        }
    }
    private String getCellValue(XSSFCell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            default:
                return "";
        }
    }

    /**
     * 检查行是否为空（所有单元格都是空）
     */
    private boolean isRowEmpty(XSSFRow row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            XSSFCell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private byte[] createResultFile(List<String[]> data) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("结果");

            // 创建标题行
            String[] headers = {"序号", "区县", "乡镇街道", "地名地址", "经度", "纬度", "来源", "匹配等级"};
            XSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // 填充数据
            for (int i = 0; i < data.size(); i++) {
                XSSFRow row = sheet.createRow(i + 1);
                String[] values = data.get(i);
                for (int j = 0; j < values.length; j++) {
                    row.createCell(j).setCellValue(values[j]);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
