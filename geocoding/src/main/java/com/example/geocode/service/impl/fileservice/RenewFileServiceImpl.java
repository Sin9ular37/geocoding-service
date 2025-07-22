package com.example.geocode.service.impl.fileservice;

import com.example.geocode.entity.ApiResult;
import com.example.geocode.service.ExcelFileService;
import com.example.geocode.service.GeocodingServiceInvoker;
import com.example.geocode.service.impl.QuotaService;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RenewFileServiceImpl implements ExcelFileService {

    private static final Logger logger = LoggerFactory.getLogger(RenewFileServiceImpl.class);

    // 模板字段映射
    private static final Map<String, Integer> TEMPLATE_COLUMNS = new HashMap<>();
    static {
        TEMPLATE_COLUMNS.put("序号", 0);
        TEMPLATE_COLUMNS.put("区县", 1);
        TEMPLATE_COLUMNS.put("乡镇街道", 2);
        TEMPLATE_COLUMNS.put("地名地址", 3);
    }

    @Value("${geocode.api.gaode.rate:3}")
    private int maxConcurrentRequests;

    @Value("${geocode.batch.size:50}")
    private int batchSize;

    @Value("${geocode.retry.count:3}")
    private int retryLimit;

    @Value("${geocode.api.timeout:3000}")
    private long apiTimeoutMs;

    @Value("${geocode.thread.pool.size:8}")
    private int threadPoolSize;

    private String cityPrefix = "哈尔滨市";

    private final GeocodingServiceInvoker geocodingServiceInvoker;
    private final QuotaService quotaService;
    private ExecutorService executor;
    private Semaphore apiRateLimiter;

    @Autowired
    public RenewFileServiceImpl(GeocodingServiceInvoker geocodingServiceInvoker,
                                QuotaService quotaService) {
        this.geocodingServiceInvoker = geocodingServiceInvoker;
        this.quotaService = quotaService;
    }

    @PostConstruct
    public void init() {
        logger.info("初始化地理编码服务: 最大并发={}, 批处理大小={}", maxConcurrentRequests, batchSize);
        this.apiRateLimiter = new Semaphore(maxConcurrentRequests, true);
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
        logger.info("线程池创建完成，大小={}", threadPoolSize);
    }

    @Override
    public byte[] processExcel(MultipartFile file) throws Exception {
        logger.info("开始处理文件: {} ({}字节)", file.getOriginalFilename(), file.getSize());

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            logger.info("工作簿加载完成，表名: {}", sheet.getSheetName());

            // 精确计算行数
            int physicalRowCount = sheet.getPhysicalNumberOfRows();
            logger.info("物理行数: {}", physicalRowCount);

            // 确保存在有效数据
            if (physicalRowCount <= 1) {
                logger.warn("输入文件无有效数据行，创建空结果文件");
                return createEmptyResultFile();
            }

            // 分析表头结构
            XSSFRow headerRow = sheet.getRow(0);
            HeaderAnalysis headerAnalysis = analyzeHeader(headerRow);
            logger.info("表头分析完成，原始列数: {}, 模板列映射: {}", headerAnalysis.totalColumns, headerAnalysis.templateColumnIndexes);

            // 安全读取所有行索引
            List<Integer> rowIndexes = new ArrayList<>();
            for (int i = 1; i < physicalRowCount; i++) {
                XSSFRow row = sheet.getRow(i);
                if (row != null && !isRowEmpty(row)) {
                    rowIndexes.add(i);
                }
            }
            logger.info("找到有效数据行: {}", rowIndexes.size());

            Map<String, ApiResult> addressCache = new ConcurrentHashMap<>();
            BlockingQueue<String[]> resultsQueue = new LinkedBlockingQueue<>();
            AtomicInteger countGaodeNum = new AtomicInteger(0);
            AtomicInteger countTiandituNum = new AtomicInteger(0);

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            int totalIndexes = rowIndexes.size();

            // 分批处理逻辑
            for (int i = 0; i < totalIndexes; i += batchSize) {
                final int startIdx = i;
                final int endIdx = Math.min(i + batchSize, totalIndexes);

                futures.add(CompletableFuture.runAsync(() -> {
                    processBatch(sheet, rowIndexes.subList(startIdx, endIdx), headerAnalysis, addressCache,
                            resultsQueue, countGaodeNum, countTiandituNum);
                }, executor));
            }

            logger.info("提交了{}个批处理任务", futures.size());

            // 带超时的等待
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(10, TimeUnit.MINUTES);
            logger.info("所有批处理任务已完成");

            // 更新配额
            int gaodeCount = countGaodeNum.get();
            int tiandituCount = countTiandituNum.get();
            logger.info("更新配额 - 高德: {}, 天地图: {}", gaodeCount, tiandituCount);
            quotaService.useCounter(gaodeCount);
            quotaService.useCounterDaily(tiandituCount);

            // 创建结果文件
            List<String[]> resultList = new ArrayList<>(resultsQueue.size());
            resultsQueue.drainTo(resultList);
            logger.info("结果队列大小: {}", resultList.size());

            return createResultFile(resultList, headerAnalysis);
        } catch (TimeoutException e) {
            logger.error("处理超时: {}", e.getMessage(), e);
            throw new RuntimeException("处理超时，请减少批量大小或增加超时时间", e);
        } catch (Exception e) {
            logger.error("处理文件时发生错误: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 表头分析结果类
     */
    private static class HeaderAnalysis {
        public final int totalColumns;
        public final Map<String, Integer> templateColumnIndexes;
        public final String[] originalHeaders;
        public final String[] resultHeaders;

        public HeaderAnalysis(int totalColumns, Map<String, Integer> templateColumnIndexes, 
                             String[] originalHeaders, String[] resultHeaders) {
            this.totalColumns = totalColumns;
            this.templateColumnIndexes = templateColumnIndexes;
            this.originalHeaders = originalHeaders;
            this.resultHeaders = resultHeaders;
        }
    }

    /**
     * 分析表头结构，确定原始字段和模板字段的位置
     */
    private HeaderAnalysis analyzeHeader(XSSFRow headerRow) {
        if (headerRow == null) {
            throw new RuntimeException("表头行不能为空");
        }

        int totalColumns = headerRow.getLastCellNum();
        String[] originalHeaders = new String[totalColumns];
        Map<String, Integer> templateColumnIndexes = new HashMap<>();

        // 读取原始表头
        for (int i = 0; i < totalColumns; i++) {
            XSSFCell cell = headerRow.getCell(i);
            String headerValue = safeGetCellValue(cell);
            originalHeaders[i] = headerValue;
            
            // 检查是否是模板字段
            if (TEMPLATE_COLUMNS.containsKey(headerValue)) {
                templateColumnIndexes.put(headerValue, i);
            }
        }

        // 验证必需的模板字段是否存在
        List<String> missingFields = new ArrayList<>();
        for (String requiredField : TEMPLATE_COLUMNS.keySet()) {
            if (!templateColumnIndexes.containsKey(requiredField)) {
                missingFields.add(requiredField);
                logger.warn("未找到必需的模板字段: {}", requiredField);
            }
        }
        
        // 如果缺少关键字段，抛出异常
        if (missingFields.contains("地名地址")) {
            throw new RuntimeException("缺少必需的模板字段：地名地址。请确保Excel文件包含此字段。");
        }

        // 构建结果表头：原始字段 + 地理编码结果字段
        List<String> resultHeaderList = new ArrayList<>();
        
        // 添加原始字段
        for (String header : originalHeaders) {
            resultHeaderList.add(header);
        }
        
        // 添加地理编码结果字段
        resultHeaderList.add("结果地址");
        resultHeaderList.add("匹配等级");
        resultHeaderList.add("来源");
        resultHeaderList.add("经度");
        resultHeaderList.add("纬度");

        String[] resultHeaders = resultHeaderList.toArray(new String[0]);

        return new HeaderAnalysis(totalColumns, templateColumnIndexes, originalHeaders, resultHeaders);
    }

    private void processBatch(XSSFSheet sheet, List<Integer> rowIndexes, HeaderAnalysis headerAnalysis,
                              Map<String, ApiResult> addressCache,
                              BlockingQueue<String[]> resultsQueue,
                              AtomicInteger countGaodeNum,
                              AtomicInteger countTiandituNum) {
        logger.debug("开始处理批次，包含{}行", rowIndexes.size());

        for (int rowNum : rowIndexes) {
            XSSFRow row = sheet.getRow(rowNum);
            if (row == null) {
                logger.warn("行{}为空，跳过处理", rowNum);
                continue;
            }

            try {
                String[] rowData = processSingleRow(row, headerAnalysis, addressCache, countGaodeNum, countTiandituNum);
                if (rowData != null) {
                    resultsQueue.put(rowData);
                }
            } catch (Exception e) {
                logger.error("处理行{}时出错: {}", rowNum, e.getMessage(), e);
                resultsQueue.offer(createErrorRowData(row, headerAnalysis));
            }
        }
        logger.debug("批次处理完成");
    }

    private String[] processSingleRow(XSSFRow row, HeaderAnalysis headerAnalysis,
                                      Map<String, ApiResult> addressCache,
                                      AtomicInteger countGaodeNum,
                                      AtomicInteger countTiandituNum) {
        try {
            logger.debug("处理行: {}", row.getRowNum());

            // 创建结果数组：原始字段数 + 5个地理编码结果字段
            String[] data = new String[headerAnalysis.totalColumns + 5];

            // 复制原始字段数据
            for (int i = 0; i < headerAnalysis.totalColumns; i++) {
                data[i] = safeGetCellValue(row.getCell(i));
            }

            // 从模板字段中提取地理编码所需的信息
            String adName = getTemplateFieldValue(row, headerAnalysis, "区县");
            String streetName = getTemplateFieldValue(row, headerAnalysis, "乡镇街道");
            String address = getTemplateFieldValue(row, headerAnalysis, "地名地址");

            logger.debug("提取地址数据: 区县={}, 街道={}, 地址={}", adName, streetName, address);

            if (address == null || address.trim().isEmpty()) {
                logger.debug("行{}地址为空", row.getRowNum());
                data[headerAnalysis.totalColumns + 2] = "空地址";
                return data;
            }

            String fullAddress = cityPrefix + adName + streetName + address;
            String cacheKey = adName + "|" + streetName + "|" + address;
            logger.debug("完整地址: {}, 缓存键: {}", fullAddress, cacheKey);

            // 优先检查缓存
            ApiResult cachedResult = addressCache.get(cacheKey);
            if (cachedResult != null) {
                logger.debug("从缓存获取结果: {}", cacheKey);
                fillResultData(data, headerAnalysis.totalColumns, cachedResult, countGaodeNum, countTiandituNum);
                return data;
            }

            // API调用
            logger.debug("获取API许可进行查询...");
            apiRateLimiter.acquire();
            try {
                logger.debug("调用地理编码API: {}", fullAddress);
                ApiResult result = withRetry(() ->
                                geocodingServiceInvoker.geocode(
                                        fullAddress,
                                        adName,
                                        streetName,
                                        address
                                ),
                        retryLimit,
                        apiTimeoutMs
                );

                if (result != null && result.getLng() != null && result.getLat() != null) {
                    logger.debug("获取API结果: {},{}, 来源={}", result.getLng(), result.getLat(), result.getSource());
                    addressCache.put(cacheKey, result);
                    fillResultData(data, headerAnalysis.totalColumns, result, countGaodeNum, countTiandituNum);
                } else {
                    logger.warn("无地理编码结果");
                    data[headerAnalysis.totalColumns + 2] = "无结果";
                }
            } finally {
                apiRateLimiter.release();
            }

            return data;
        } catch (Exception e) {
            logger.error("处理单行数据时出错", e);
            return createErrorRowData(row, headerAnalysis);
        }
    }

    /**
     * 从行中获取指定模板字段的值
     */
    private String getTemplateFieldValue(XSSFRow row, HeaderAnalysis headerAnalysis, String fieldName) {
        Integer columnIndex = headerAnalysis.templateColumnIndexes.get(fieldName);
        if (columnIndex == null) {
            logger.warn("未找到模板字段: {}", fieldName);
            return "";
        }
        return safeGetCellValue(row.getCell(columnIndex));
    }

    // 精确的速率控制类
    private static class RateLimiter {
        private final int maxRate;
        private final long minInterval;
        private long lastCallTime = 0;

        public RateLimiter(int maxRate) {
            this.maxRate = maxRate;
            this.minInterval = 1000 / maxRate; // 每次调用至少间隔的时间(毫秒)
        }

        public synchronized void acquire() throws InterruptedException {
            long now = System.currentTimeMillis();
            long elapsed = now - lastCallTime;

            if (elapsed < minInterval) {
                long sleepTime = minInterval - elapsed;
                logger.trace("速率控制-等待 {} 毫秒", sleepTime);
                Thread.sleep(sleepTime);
            }

            lastCallTime = System.currentTimeMillis();
        }
    }

    // 安全的单元格读取方法
    private String safeGetCellValue(XSSFCell cell) {
        if (cell == null) return "";

        try {
            if (cell.getCellType() == CellType.STRING) {
                return cell.getStringCellValue();
            } else if (cell.getCellType() == CellType.NUMERIC) {
                return String.valueOf((int) cell.getNumericCellValue());
            } else {
                return "";
            }
        } catch (Exception e) {
            logger.error("读取单元格值时出错", e);
            return "";
        }
    }

    // 创建空结果文件（当输入无效时）
    private byte[] createEmptyResultFile() {
        logger.info("创建空结果文件");
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("结果");
            XSSFRow headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("输入文件无有效数据行");

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            logger.error("创建空结果文件失败", e);
            // 返回最小空文件
            return new byte[0];
        }
    }

    private void fillResultData(String[] data, int originalColumnsCount, ApiResult result,
                                AtomicInteger countGaodeNum, AtomicInteger countTiandituNum) {
        if (result == null) {
            data[originalColumnsCount] = "";
            data[originalColumnsCount + 1] = "";
            data[originalColumnsCount + 2] = "";
            data[originalColumnsCount + 3] = "";
            data[originalColumnsCount + 4] = "无结果";
            return;
        }

        if (result.getLng() != null && result.getLat() != null) {
            data[originalColumnsCount] = result.getFormatted_address() != null ? result.getFormatted_address() : "未知地名";
            data[originalColumnsCount + 1] = result.getLevel() != null ? result.getLevel() : "未知等级";
            data[originalColumnsCount + 2] = result.getSource() != null ? result.getSource() : "未知来源";
            data[originalColumnsCount + 3] = result.getLng();
            data[originalColumnsCount + 4] = result.getLat();

            if ("gaode".equals(result.getSource())) {
                countGaodeNum.incrementAndGet();
            } else if ("tianditu".equals(result.getSource())) {
                countTiandituNum.incrementAndGet();
            }
        } else {
            data[originalColumnsCount] = "";
            data[originalColumnsCount + 1] = "";
            data[originalColumnsCount + 2] = "";
            data[originalColumnsCount + 3] = "";
            data[originalColumnsCount + 4] = "无结果";
        }
    }

    private ApiResult withRetry(ThrowingSupplier<ApiResult> supplier, int retries, long timeout)
            throws Exception {
        int attempt = 0;
        while (attempt <= retries) {
            try {
                return supplier.get();
            } catch (Exception e) {
                logger.warn("API调用失败，尝试 {}/{}", attempt + 1, retries);
                if (attempt++ >= retries) {
                    throw e;
                }
                Thread.sleep(timeout);
            }
        }
        return null;
    }

    private String[] createErrorRowData(XSSFRow row, HeaderAnalysis headerAnalysis) {
        String[] data = new String[headerAnalysis.totalColumns + 5];
        try {
            // 复制原始数据
            for (int i = 0; i < headerAnalysis.totalColumns; i++) {
                data[i] = row.getCell(i) != null ? getCellValue(row.getCell(i)) : "";
            }
        } catch (Exception e) {
            logger.error("创建错误行数据时出错", e);
        }
        
        // 设置地理编码结果字段为空
        data[headerAnalysis.totalColumns] = "";
        data[headerAnalysis.totalColumns + 1] = "";
        data[headerAnalysis.totalColumns + 2] = "";
        data[headerAnalysis.totalColumns + 3] = "";
        data[headerAnalysis.totalColumns + 4] = "处理错误";
        return data;
    }

    private String getCellValue(XSSFCell cell) {
        if (cell == null) return "";
        try {
            if (cell.getCellType() == CellType.STRING) {
                return cell.getStringCellValue();
            } else if (cell.getCellType() == CellType.NUMERIC) {
                return String.valueOf((int) cell.getNumericCellValue());
            } else {
                return "";
            }
        } catch (Exception e) {
            logger.error("获取单元格值时出错", e);
            return "";
        }
    }

    private boolean isRowEmpty(XSSFRow row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            XSSFCell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellValue(cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private byte[] createResultFile(List<String[]> data, HeaderAnalysis headerAnalysis) throws IOException {
        logger.info("创建结果文件，包含{}行数据", data.size());

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("结果");

            // 创建标题行
            XSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < headerAnalysis.resultHeaders.length; i++) {
                headerRow.createCell(i).setCellValue(headerAnalysis.resultHeaders[i]);
            }

            // 填充数据
            if (data != null && !data.isEmpty()) {
                for (int i = 0; i < data.size(); i++) {
                    XSSFRow row = sheet.createRow(i + 1);
                    String[] values = data.get(i);
                    for (int j = 0; j < values.length; j++) {
                        try {
                            String cellValue = values[j] != null ? values[j] : "";
                            row.createCell(j).setCellValue(cellValue);
                        } catch (Exception e) {
                            logger.error("创建单元格时出错(行:{},列:{})", i + 1, j, e);
                            row.createCell(j).setCellValue("");
                        }
                    }
                }
            } else {
                logger.warn("结果数据集为空，仅创建标题行");
            }

            workbook.write(out);
            byte[] result = out.toByteArray();
            logger.info("生成结果文件大小: {} 字节", result.length);
            return result;
        } catch (IOException e) {
            logger.error("创建结果文件失败", e);
            throw e;
        }
    }

    // 创建错误结果文件
    private byte[] createErrorResultFile(String message) {
        logger.warn("创建错误结果文件: {}", message);
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("错误");
            XSSFRow row = sheet.createRow(0);
            row.createCell(0).setCellValue("处理失败: " + message);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            logger.error("创建错误文件失败", e);
            return new byte[0];
        }
    }


    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}