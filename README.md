# 基于动态深度学习与大模型反馈的电池故障检测系统

本项目是一个基于动态深度学习与大模型反馈的电池故障检测系统，能够实时检测电池运行状态，预测潜在故障，并提供智能分析与建议。系统集成了前端展示、后端处理和深度学习模型，为电池健康管理提供全面解决方案。

## 🏗️ 系统架构

### 📋 架构概览

```mermaid
graph TD
    %% 前端层
    subgraph Frontend["🖥️ 前端层 (Vue.js)"]
        direction TB
        UI[用户界面]
        DashboardPage[数据仪表板]
        DetectionPage[异常检测页面]
        AnalysisPage[故障分析页面]
        UserPage[用户管理页面]
        
        UI --- DashboardPage
        UI --- DetectionPage
        UI --- AnalysisPage
        UI --- UserPage
    end

    %% 业务逻辑层
    subgraph Backend["⚙️ 业务逻辑层 (Spring Boot)"]
        direction TB
        subgraph Controllers["控制器层"]
            DetectionController[检测控制器]
            UserController[用户控制器] 
            DataController[数据控制器]
            AnalysisController[分析控制器]
        end
        
        subgraph Services["服务层"]
            DetectionService[检测服务]
            UserService[用户服务]
            DataService[数据服务]
            ReportService[报告服务]
        end
        
        subgraph Mappers["数据访问层"]
            UserMapper[用户映射器]
            DetectionMapper[检测记录映射器]
            DataMapper[数据映射器]
        end
        
        Controllers --- Services
        Services --- Mappers
    end

    %% AI推理层
    subgraph AILayer["🤖 AI推理层 (Flask)"]
        direction TB
        FlaskApp[Flask应用]
        
        subgraph ModelEngine["模型引擎"]
            DynamicVAE[动态VAE模型]
            AnomalyDetector[异常检测器]
            FeatureExtractor[特征提取器]
        end
        
        subgraph AIAssistant["AI助手服务"]
            ChatAPI[ChatAPI]
            DeepSeek[DeepSeek API]
            Qwen[Qwen API]
            LocalModels[本地模型服务]
        end
        
        FlaskApp --- ModelEngine
        FlaskApp --- AIAssistant
    end

    %% 数据存储层
    subgraph Storage["💾 数据存储层"]
        direction LR
        MySQL[(MySQL数据库)]
        FileStorage[(文件存储)]
        
        subgraph Tables["数据表"]
            UserTable[用户表]
            DetectionTable[检测记录表]
            ModelTable[模型参数表]
            AnomalyTable[异常记录表]
        end
        
        MySQL --- Tables
    end

    %% 外部服务
    subgraph External["🌐 外部服务"]
        direction TB
        OnlineAI[在线AI服务]
        LMStudio[本地LM-Studio]
        LANAI[局域网AI服务]
        DataSource[电池数据源]
    end

    %% 连接关系
    Frontend -.-> Backend
    Backend -.-> AILayer
    Backend -.-> Storage
    AILayer -.-> External
    AILayer -.-> Storage
    
    %% 样式
    classDef frontendStyle fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
    classDef backendStyle fill:#e8f5e8,stroke:#388e3c,stroke-width:2px
    classDef aiStyle fill:#fce4ec,stroke:#c2185b,stroke-width:2px
    classDef storageStyle fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef externalStyle fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    
    class Frontend,UI,DashboardPage,DetectionPage,AnalysisPage,UserPage frontendStyle
    class Backend,Controllers,Services,Mappers,DetectionController,UserController,DataController,AnalysisController,DetectionService,UserService,DataService,ReportService,UserMapper,DetectionMapper,DataMapper backendStyle
    class AILayer,FlaskApp,ModelEngine,AIAssistant,DynamicVAE,AnomalyDetector,FeatureExtractor,ChatAPI,DeepSeek,Qwen,LocalModels aiStyle
    class Storage,MySQL,FileStorage,Tables,UserTable,DetectionTable,ModelTable,AnomalyTable storageStyle
    class External,OnlineAI,LMStudio,LANAI,DataSource externalStyle
```

---

## 🗄️ 数据模型设计

### 📊 实体关系图

```mermaid
classDiagram
    %% 实体类
    class User {
        +Integer id
        +String username
        +String password
        +String name
        +String email
        +String role
        +Date createTime
    }
    
    class BatteryData {
        +Integer id
        +String batteryId
        +Double voltage
        +Double current
        +Double temperature
        +Double capacity
        +DateTime timestamp
        +String dataSource
    }
    
    class DetectionRecord {
        +Integer id
        +String recordId
        +String batteryId
        +String detectionType
        +Double anomalyScore
        +String anomalyLevel
        +String features
        +String username
        +DateTime startTime
        +String aiModel
        +String suggestion
    }
    
    class ModelConfig {
        +Integer id
        +String modelName
        +String modelPath
        +String parameters
        +String version
        +DateTime createTime
        +Boolean isActive
    }
    
    %% 控制器类
    class DetectionController {
        -DetectionService detectionService
        +detectAnomaly() Result
        +getDetectionHistory() Result
        +exportReport() Result
    }
    
    class DataController {
        -DataService dataService
        +uploadData() Result
        +getBatteryData() Result
        +getStatistics() Result
    }
    
    %% 服务类
    class FlaskAIService {
        -DynamicVAE model
        -ChatAPI chatAPI
        +detectAnomaly() Dict
        +trainModel() Boolean
        +generateSuggestion() String
        +extractFeatures() Array
    }
    
    class DynamicVAE {
        -String modelPath
        -Dict parameters
        +encode() Tensor
        +decode() Tensor
        +calculateAnomalyScore() Double
        +train() Boolean
    }
    
    class ChatAPI {
        -String apiKey
        -String modelName
        +generateAnalysis() String
        +getSuggestions() String
        +thinkModeRequest() String
    }
    
    %% 关系定义
    User "1" --> "*" DetectionRecord : creates
    BatteryData "1" --> "*" DetectionRecord : analyzed_by
    ModelConfig "1" --> "*" DetectionRecord : used_by
    
    DetectionController --> FlaskAIService : calls
    DataController --> BatteryData : manages
    
    FlaskAIService --> DynamicVAE : uses
    FlaskAIService --> ChatAPI : uses
    
    DynamicVAE --> BatteryData : processes
```

---

## 🔄 系统流程设计

### ⏱️ 异常检测流程时序图

```mermaid
sequenceDiagram
    participant U as 🧑‍🔬 用户
    participant V as 💻 Vue前端
    participant S as ⚙️ Spring Boot
    participant F as 🤖 Flask AI服务
    participant M as 🧠 动态VAE模型
    participant A as 🤖 AI助手
    participant D as 💾 数据库
    
    %% 数据上传阶段
    Note over U,D: 📤 数据上传阶段
    U->>V: 1. 上传电池数据
    V->>S: 2. POST /api/data/upload
    S->>D: 3. 保存原始数据
    D-->>S: 4. 确认保存成功
    
    %% 异常检测阶段
    Note over U,A: 🔍 异常检测阶段
    U->>V: 5. 启动异常检测
    V->>S: 6. POST /api/detection/analyze
    S->>F: 7. 转发检测请求
    
    F->>M: 8. 加载动态VAE模型
    M->>M: 9. 特征提取
    M->>M: 10. 异常分数计算
    M-->>F: 11. 返回检测结果
    
    %% AI分析阶段
    Note over F,A: 🧠 AI分析阶段
    alt 用户选择AI分析
        F->>A: 12. 发送检测结果
        A->>A: 13. 生成故障分析
        A-->>F: 14. 返回分析建议
    end
    
    %% 结果保存阶段
    Note over S,D: 💾 结果保存阶段
    F-->>S: 15. 返回完整结果
    S->>D: 16. 保存检测记录
    D-->>S: 17. 确认保存成功
    
    %% 结果展示阶段
    Note over U,V: 📊 结果展示阶段
    S-->>V: 18. 返回检测结果
    V-->>U: 19. 展示异常分析
    V-->>U: 20. 显示AI建议
    
    %% 报告生成阶段
    Note over U,V: 📄 报告生成(可选)
    U->>V: 21. 生成分析报告
    V-->>U: 22. 下载PDF报告
```

### 📊 数据流图

```mermaid
graph LR
    %% 输入层
    subgraph Input["📥 数据输入层"]
        direction TB
        A[🔋 电池数据上传]
        B[⚙️ 模型参数配置]
        C[🎯 检测类型选择]
        D[🤖 AI模型选择]
        E[🧠 思考模式开关]
    end
    
    %% 处理层
    subgraph Process["⚡ 数据处理层"]
        direction TB
        F[🔄 数据预处理]
        G[🧠 特征提取]
        H[📊 异常检测]
        I[💡 AI建议生成]
        J[📈 结果可视化]
    end
    
    %% 存储层
    subgraph Storage["💾 数据存储层"]
        direction TB
        K[🔋 原始数据存储]
        L[📊 特征数据存储]
        M[📋 检测记录存储]
        N[🤖 模型参数存储]
    end
    
    %% 输出层
    subgraph Output["📤 结果输出层"]
        direction TB
        O[📊 异常分数]
        P[⚠️ 异常等级]
        Q[📈 可视化图表]
        R[💡 AI分析建议]
        S[📄 检测报告]
    end
    
    %% 数据流连接
    A --> F
    B --> G
    C --> H
    D --> I
    E --> I
    
    F --> G
    G --> H
    H --> J
    G --> I
    
    A --> K
    G --> L
    H --> M
    B --> N
    
    H --> O
    H --> P
    J --> Q
    I --> R
    O --> S
    P --> S
    Q --> S
    R --> S
    
    %% 样式定义
    classDef inputStyle fill:#e3f2fd,stroke:#1976d2,stroke-width:2px,color:#000
    classDef processStyle fill:#e8f5e8,stroke:#388e3c,stroke-width:2px,color:#000
    classDef storageStyle fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px,color:#000
    classDef outputStyle fill:#fff3e0,stroke:#f57c00,stroke-width:2px,color:#000
    
    class A,B,C,D,E inputStyle
    class F,G,H,I,J processStyle
    class K,L,M,N storageStyle
    class O,P,Q,R,S outputStyle
```

---

## 🚀 部署架构

### 🖥️ 部署架构图

```mermaid
graph TB
    %% 客户端层
    subgraph Client["👥 客户端层"]
        direction LR
        WebBrowser[🌐 Web浏览器]
        MobileBrowser[📱 移动端浏览器]
        DataCollector[📡 数据采集器]
    end
    
    %% 负载均衡层
    subgraph LoadBalancer["⚖️ 负载均衡层"]
        Nginx[🔄 Nginx反向代理<br/>端口: 80/443]
    end
    
    %% 应用服务层
    subgraph AppServer["🖥️ 应用服务层"]
        direction TB
        
        subgraph Frontend["🎨 前端服务"]
            Vue[Vue.js应用<br/>📦 Node.js<br/>🔗 端口: 3000]
        end
        
        subgraph Backend["⚙️ 后端服务"]
            SpringBoot[Spring Boot应用<br/>☕ JDK 24<br/>🔗 端口: 9999]
        end
        
        subgraph AIService["🤖 AI推理服务"]
            Flask[Flask应用<br/>🐍 Python 3.8+<br/>🔗 端口: 5000]
            DynamicVAE[🧠 动态VAE模型<br/>🔥 PyTorch/CUDA]
        end
    end
    
    %% AI模型服务层
    subgraph ModelService["🧠 AI模型服务层"]
        direction TB
        
        subgraph Local["💻 本地服务"]
            LMStudio[🏠 LM-Studio<br/>🔗 端口: 1234]
        end
        
        subgraph Cloud["☁️ 云端服务"]
            DeepSeek[🌊 DeepSeek API<br/>🔑 API密钥认证]
            Qwen[🔮 Qwen API<br/>🔑 API密钥认证]
        end
        
        subgraph LAN["🏢 局域网服务"]
            LANModels[🌐 局域网AI服务<br/>🔗 192.168.1.108:1234]
        end
    end
    
    %% 数据存储层
    subgraph DataLayer["💾 数据存储层"]
        direction LR
        MySQL[🗄️ MySQL数据库<br/>🔗 端口: 3306<br/>📊 用户/检测数据]
        FileStorage[📁 文件存储系统<br/>📋 模型文件/数据文件]
        ModelStorage[🧠 模型存储<br/>💾 VAE模型参数]
    end
    
    %% 外部数据源
    subgraph DataSource["📡 外部数据源"]
        BatterySystem[🔋 电池管理系统<br/>⚡ 实时数据接口]
        IoTSensors[📊 IoT传感器<br/>🌡️ 温度/电压监控]
        HistoricalDB[📚 历史数据库<br/>📈 历史运行数据]
    end
    
    %% 连接关系
    Client -.->|HTTPS/HTTP| LoadBalancer
    LoadBalancer -.->|反向代理| Frontend
    LoadBalancer -.->|API转发| Backend
    
    Frontend -.->|REST API| Backend
    Backend -.->|HTTP调用| AIService
    Backend -.->|JDBC| DataLayer
    
    AIService -.->|HTTP请求| ModelService
    AIService -.->|文件操作| DataLayer
    
    Backend -.->|数据采集| DataSource
    
    %% 样式定义
    classDef clientStyle fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
    classDef proxyStyle fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef appStyle fill:#e8f5e8,stroke:#388e3c,stroke-width:2px
    classDef modelStyle fill:#fce4ec,stroke:#c2185b,stroke-width:2px
    classDef dataStyle fill:#fff8e1,stroke:#f9a825,stroke-width:2px
    classDef sourceStyle fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    
    class WebBrowser,MobileBrowser,DataCollector clientStyle
    class Nginx proxyStyle
    class Vue,SpringBoot,Flask,DynamicVAE appStyle
    class LMStudio,DeepSeek,Qwen,LANModels modelStyle
    class MySQL,FileStorage,ModelStorage dataStyle
    class BatterySystem,IoTSensors,HistoricalDB sourceStyle
```

---

本系统采用前后端分离架构，包含三个主要组件：

1. **前端(Vue)**：基于Vue3+TypeScript构建的用户界面，提供直观的数据可视化和分析工具。
2. **后端(SpringBoot)**：负责用户管理、数据存储和请求转发的Java服务。
3. **AI模型服务(Flask)**：运行动态深度学习模型和大语言模型的Python服务，提供异常检测和智能分析功能。

## 主要功能

- 时间点异常检测：实时监测电池参数，检测单一时间点的异常
- 时间段异常检测：分析电池历史数据，识别长期异常模式
- 故障预警与分析：通过动态VAE模型检测异常并计算异常分数
- 智能分析报告：生成可视化报表，并提供LLM分析建议
- 用户管理：多用户权限管理

## 技术栈

- **前端**：Vue 3、TypeScript、Element Plus
- **后端**：SpringBoot 3.5.4 (最新版)、MyBatis-Plus、MySQL
- **AI服务**：Flask、动态VAE模型、大型语言模型API
- **Java版本**：JDK 24 (最新LTS版本)
- **部署**：Docker (可选)

## 🚀 最新更新

### 版本升级 (2025年8月)
- **JDK升级**：从Java 1.8 升级到 JDK 24，带来以下改进：
  - 更好的垃圾回收性能
  - 增强的安全特性
  - 改进的JVM性能和内存管理
- **Spring Boot升级**：从2.3.7升级到3.5.4
  - 支持更多现代化特性
  - 更好的性能和稳定性
  - 增强的安全性
- **依赖库更新**：升级了所有核心依赖到最新稳定版本
- **警告修复**：解决了JDK 24下的所有编译和运行时警告
  - 添加了`--sun-misc-unsafe-memory-access=allow`参数解决FastJSON2的sun.misc.Unsafe警告
  - 升级FastJSON2到2.0.58版本（最新稳定版）
  - 配置了完整的JVM参数确保在JDK 24下正常运行

## 快速开始

### 前提条件

- **JDK 24** (已升级到最新版本，包含性能优化和安全改进)
- Node.js 16+
- Python 3.8+
- MySQL 8.0+
- CUDA支持的GPU (推荐用于模型推理)
- LM-Studio (用于本地部署大模型)

### JDK 24 兼容性说明

本项目已完全适配JDK 24，所有JVM警告均已解决：

- ✅ **sun.misc.Unsafe警告** - 通过`--sun-misc-unsafe-memory-access=allow`参数解决
- ✅ **模块系统** - 配置了必要的`--add-opens`参数
- ✅ **动态代理** - 启用了`-XX:+EnableDynamicAgentLoading`
- ✅ **参数名保留** - 编译器配置了`-parameters`标志

如需手动运行，请使用以下JVM参数：
```bash
--enable-native-access=ALL-UNNAMED
--add-opens java.base/java.lang=ALL-UNNAMED 
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/sun.misc=ALL-UNNAMED
--sun-misc-unsafe-memory-access=allow
-XX:+EnableDynamicAgentLoading
```

### 数据库配置

1. 创建名为`ai`的数据库
2. 运行`数据库文件/database.sql`脚本初始化数据库结构

### 后端服务启动

1. 进入springboot目录
2. 使用Maven构建项目：`mvn clean package`
3. 运行生成的jar文件：`java -jar target/Kcsj-0.0.1-SNAPSHOT.jar`

### AI服务启动

1. 进入flask目录
2. 安装依赖：`pip install -r requirements.txt`
3. 启动Flask服务：`python main.py`

### 前端启动

1. 进入vue目录
2. 安装依赖：`npm install`
3. 启动开发服务器：`npm run dev`
4. 构建生产版本：`npm run build`

## 系统访问

- 前端页面：http://localhost:3000
- Spring Boot 后端：http://localhost:9999
- Flask AI 服务：http://localhost:5000

### 默认登录账号

- **管理员账号**：admin
- **密码**：admin123

## 大模型部署与使用

本系统支持多种大模型部署方式，用于生成故障分析报告和建议：

### 支持的模型

- **云端API模型**
  - Deepseek-R1
  - Qwen

- **局域网部署模型**
  - Deepseek-R1-LAN
  - Qwen3-LAN
  - Qwen2.5-VL-LAN
  - Qwen2.5-Omni-LAN
  - Gemma3-LAN

- **本地部署模型**
  - Deepseek-R1-Local
  - Qwen3-Local
  - Qwen2.5-VL-Local
  - Qwen2.5-Omni-Local
  - Gemma3-Local

### 使用LM-Studio进行本地部署

1. 下载并安装 [LM-Studio](https://lmstudio.ai/)
2. 从Hugging Face或其他来源下载所需模型（如Deepseek-R1、Qwen等）
3. 在LM-Studio中加载模型
4. 启动本地API服务器（通常在http://localhost:1234）
5. 在系统设置中选择对应的"本地"模型选项

### 思考模式

系统支持开启思考模式（thinkMode），启用后大模型会提供更详细的分析过程和故障诊断依据，适合研究和深度分析使用。

## 模型信息

### 动态深度学习模型

本系统使用的动态深度学习模型基于清华大学提出的Dyad（Dynamic Anomaly Detection）框架，该框架专为电池故障检测设计，能够捕捉电池参数的时序变化特征。模型采用变分自编码器（VAE）结构，通过学习正常电池运行状态的分布，识别偏离正常范围的异常模式。

### 大语言模型

支持多种大语言模型，既可以通过API密钥访问云端模型，也可以通过LM-Studio在本地部署运行。系统会根据选定的模型自动配置请求参数。

## 许可证

MIT

## 贡献

欢迎提交问题和贡献代码，请通过创建Issue或Pull Request参与项目开发。

## 致谢

感谢清华大学提供的Dyad动态深度学习模型框架，以及所有为本项目提供支持和贡献的人员。
