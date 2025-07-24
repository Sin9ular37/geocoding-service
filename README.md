# 地理编码服务系统 (GeoCode)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.6.13-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-8-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0.28-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## 📋 项目简介

地理编码服务系统是一个基于Spring Boot的Web应用，提供批量地理编码功能。支持Excel文件批量处理，将地址信息转换为经纬度坐标。系统集成了多个主流地图API服务，具备智能降级和本地缓存功能。

## ✨ 主要特性

- **多服务支持**: 集成高德地图、天地图、百度地图API
- **智能降级**: 按优先级调用服务，失败时自动切换备用服务
- **批量处理**: 支持Excel文件批量地理编码
- **本地缓存**: 内置MySQL数据库缓存，提升查询效率
- **并发控制**: 支持多线程处理和API速率限制
- **Web界面**: 提供友好的文件上传和下载界面
- **配额管理**: 实时监控API调用配额使用情况

## 🏗️ 系统架构

```
├── Controller Layer     # 控制器层
│   ├── UploadController    # 文件上传处理
│   ├── DownloadController  # 模板文件下载
│   └── ViewPageController  # 页面路由
├── Service Layer        # 服务层
│   ├── GeocodingService    # 地理编码服务接口
│   ├── ExcelFileService    # Excel文件处理服务
│   └── impl/               # 具体实现
│       ├── GaodeServiceImpl     # 高德地图服务
│       ├── TiandituServiceImpl  # 天地图服务
│       ├── BaiduServiceImpl     # 百度地图服务
│       └── LocalServiceImpl     # 本地缓存服务
├── Configuration        # 配置层
│   ├── GaodeConfig         # 高德API配置
│   ├── TiandituConfig      # 天地图API配置
│   └── BaiduConfig         # 百度API配置
└── Data Layer           # 数据层
    ├── LocationMapper      # 地理位置数据映射
    └── CounterMapper       # 配额计数器映射
```

## 🚀 快速开始

### 环境要求

- Java 8+
- Maven 3.6+
- MySQL 8.0+
- Node.js (可选，用于前端开发)

### 安装步骤

1. **克隆项目**
```bash
git clone https://github.com/your-username/geocode.git
cd geocode
```

2. **数据库配置**
```sql
-- 创建数据库
CREATE DATABASE geocode CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建位置信息表
CREATE TABLE location (
    id INT PRIMARY KEY AUTO_INCREMENT,
    pname VARCHAR(100) COMMENT '省份名称',
    cityname VARCHAR(100) COMMENT '城市名称',
    adname VARCHAR(100) COMMENT '区县名称',
    business_a VARCHAR(200) COMMENT '商圈',
    streetname VARCHAR(200) COMMENT '乡镇街道',
    name VARCHAR(500) COMMENT '具体地点名称',
    lng DECIMAL(10,7) COMMENT '经度',
    lat DECIMAL(10,7) COMMENT '纬度'
);

-- 创建配额计数表
CREATE TABLE counter (
    id INT PRIMARY KEY AUTO_INCREMENT,
    count_today INT DEFAULT 0 COMMENT '今日调用次数',
    count_total INT DEFAULT 0 COMMENT '总调用次数',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

3. **配置文件修改**

编辑 `src/main/resources/application.properties`:

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/geocode?useSSL=false&characterEncoding=utf8
spring.datasource.username=your_username
spring.datasource.password=your_password

# API密钥配置
gaode.api.key=your_gaode_api_key
baidu.api.key=your_baidu_api_key
tianditu.api.key=your_tianditu_api_key
```

4. **编译运行**
```bash
# 编译项目
mvn clean compile

# 运行项目
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar target/geocode-1.0.0.jar
```

5. **访问应用**

打开浏览器访问: `http://localhost:80/geocode`

## 📖 使用说明

### Excel模板格式

系统需要特定格式的Excel文件，包含以下列：

| 列名 | 说明 | 示例 |
|------|------|------|
| 序号 | 数据序号 | 1, 2, 3... |
| 区县 | 区县名称 | 南岗区, 道里区 |
| 乡镇街道 | 街道名称 | 和兴路街道, 新阳路街道 |
| 地名地址 | 具体地址 | 哈尔滨工业大学, 中央大街 |

### 处理流程

1. **下载模板**: 点击"下载模板文件"获取标准Excel模板
2. **填写数据**: 按照模板格式填写地址信息
3. **上传文件**: 选择填写好的Excel文件进行上传
4. **自动处理**: 系统自动进行地理编码处理
5. **下载结果**: 处理完成后下载包含经纬度的结果文件

### 结果文件格式

处理后的Excel文件将包含原始数据和以下新增列：

| 列名 | 说明 |
|------|------|
| 结果地址 | 标准化地址 |
| 匹配等级 | 地址匹配精度 |
| 来源 | 数据来源(gaode/tianditu/local) |
| 经度 | WGS84坐标经度 |
| 纬度 | WGS84坐标纬度 |

## ⚙️ 配置参数

### 核心配置

```properties
# 地理编码服务优先级
geocoding.priority=gaode,tianditu,local

# 并发控制
geocode.api.gaode.rate=3                # 高德API并发数
geocode.batch.size=50                   # 批处理大小
geocode.thread.pool.size=8             # 线程池大小

# 重试设置
geocode.retry.count=3                   # 重试次数
geocode.api.timeout=3000               # API超时时间(ms)

# 文件上传限制
spring.servlet.multipart.max-file-size=150MB
spring.servlet.multipart.max-request-size=150MB
```

### API配置

```properties
# 高德地图API
gaode.api.url=https://restapi.amap.com/v3/geocode/geo
gaode.api.key=your_api_key
gaode.api.citycode=0451

# 天地图API
tianditu.api.url=https://api.tianditu.gov.cn/geocoder
tianditu.api.key=your_api_key

# 百度地图API
baidu.api.url=https://api.map.baidu.com/geocoder/v2/
baidu.api.key=your_api_key
baidu.api.city=哈尔滨市
```

## 🔧 API接口

### 文件处理接口

```http
POST /v2/api/upload
Content-Type: multipart/form-data

参数:
- file: Excel文件 (.xlsx格式)

响应:
- Content-Type: application/octet-stream
- 文件: 处理结果Excel文件
```

### 模板下载接口

```http
GET /download

响应:
- Content-Type: application/octet-stream
- 文件: Excel模板文件
```

### 配额查询接口

```http
GET /api/quota

响应:
{
  "todayUsage": 1250,
  "totalUsage": 45680,
  "remainingQuota": 2000
}
```

## 🛠️ 技术栈

### 后端技术
- **Spring Boot 2.6.13**: 主框架
- **MyBatis**: ORM框架
- **MySQL 8.0**: 数据库
- **Apache POI**: Excel文件处理
- **Jackson**: JSON处理
- **HttpClient**: HTTP客户端
- **Caffeine**: 本地缓存

### 前端技术
- **Thymeleaf**: 模板引擎
- **Bootstrap**: UI框架
- **JavaScript**: 交互逻辑
- **Font Awesome**: 图标库

## 📊 性能特性

- **并发处理**: 支持多线程并发处理，可配置线程池大小
- **智能缓存**: 三级缓存策略（内存缓存 → 数据库缓存 → API调用）
- **速率控制**: 基于信号量的API调用速率限制
- **批量优化**: 分批处理大文件，避免内存溢出
- **错误处理**: 完善的异常处理和重试机制

## 🔍 监控与日志

### 日志配置
```properties
# 日志级别
logging.level.root=INFO
logging.level.com.example.geocode=DEBUG

# 日志文件
logging.file.name=logs/geocode.log
logging.pattern.file=%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n
```

### 监控指标
- API调用成功率
- 平均响应时间
- 缓存命中率
- 处理文件数量
- 错误统计

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系我们

- 项目主页: [https://github.com/Sin9ular37/geocoding-service](https://github.com/Sin9ular37/geocoding-service)
- 问题反馈: [Issues](https://github.com/Sin9ular37/geocoding-service/issues)
- 邮箱: shiqouhz@163.com

## 🙏 致谢

感谢以下服务提供商为本项目提供的API支持：
- [高德地图开放平台](https://lbs.amap.com/)
- [天地图API](https://www.tianditu.gov.cn/)
- [百度地图开放平台](https://lbsyun.baidu.com/)

---

⭐ 如果这个项目对你有帮助，请给它一个星标！ 
