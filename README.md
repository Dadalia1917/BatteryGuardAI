# 基于动态深度学习与大模型反馈的电池故障检测系统

## 项目简介

本项目是一个基于动态深度学习与大模型反馈的电池故障检测系统，能够实时检测电池运行状态，预测潜在故障，并提供智能分析与建议。系统集成了前端展示、后端处理和深度学习模型，为电池健康管理提供全面解决方案。

## 技术架构

本系统采用前后端分离架构：

- **前端**：Vue.js + Element Plus，提供直观的用户界面和可视化分析
- **后端**：
  - Spring Boot：RESTful API服务
  - Flask：Python深度学习模型服务
- **核心算法**：动态深度学习模型（引用自清华大学的Dyad动态深度学习检测电池模型）
- **大模型反馈**：接入LLM模型提供故障分析和处理建议

## 系统功能

1. **时间点异常检测**：实时监测电池参数，检测单一时间点的异常
2. **时间段异常检测**：分析电池历史数据，识别长期异常模式
3. **故障预警与分析**：通过动态VAE模型检测异常并计算异常分数
4. **智能分析报告**：生成可视化报表，并提供LLM分析建议
5. **用户管理**：多用户权限管理

## 目录结构

- **app.py**：Flask应用主入口
- **flask/**：Flask API服务
- **model/**：深度学习模型实现
- **springboot/**：Spring Boot后端服务
- **vue/**：Vue.js前端应用
- **数据库文件/**：MySQL数据库结构

## 模型说明

### 深度学习模型
本项目使用的动态深度学习模型基于清华大学提出的Dyad（Dynamic Anomaly Detection）框架，该框架专为电池故障检测设计，能够捕捉电池参数的时序变化特征。模型采用变分自编码器（VAE）结构，通过学习正常电池运行状态的分布，识别偏离正常范围的异常模式。

### 大模型部署与配置
本系统使用LM-Studio进行大模型本地部署，支持多种开源LLM模型，为电池故障分析提供智能反馈。

#### LM-Studio部署说明
1. 下载并安装LM-Studio（https://lmstudio.ai/）
2. 在LM-Studio中下载所需的模型（支持Deepseek-R1、Qwen系列、Gemma3等）
3. 启动本地服务器，默认地址为http://127.0.0.1:1234
4. 可选：配置局域网访问以支持分布式部署

#### 支持的AI模型
系统支持以下大模型选项：
- **Deepseek-R1**：支持API、局域网和本地部署方式
  - `Deepseek-R1`: API调用版本
  - `Deepseek-R1-LAN`: 局域网部署版本
  - `Deepseek-R1-Local`: 本地部署版本
  
- **Qwen系列**：支持多种模型和部署方式
  - `Qwen`: API调用版本
  - `Qwen3-LAN`/`Qwen3-Local`: 通义千问3.0（局域网/本地）
  - `Qwen2.5-VL-LAN`/`Qwen2.5-VL-Local`: 通义千问2.5多模态版（局域网/本地）
  - `Qwen2.5-Omni-LAN`/`Qwen2.5-Omni-Local`: 通义千问2.5全能版（局域网/本地）

- **Gemma3系列**：Google开源模型
  - `Gemma3-LAN`: 局域网部署版本
  - `Gemma3-Local`: 本地部署版本

- **无AI选项**：`不使用AI`，仅使用传统分析方法

#### 思考模式
系统支持为大模型启用"思考模式"，使AI先分析问题再给出答案，提供更深入的故障分析。

## 部署指南

### 前端部署

```bash
cd vue
npm install
npm run dev
```

### Spring Boot后端部署

```bash
cd springboot
mvn clean package
java -jar target/Kcsj-0.0.1-SNAPSHOT.jar
```

### Flask服务部署

```bash
cd flask
pip install -r requirements.txt
python main.py
```

### 数据库配置

导入 `数据库文件/database.sql` 到MySQL数据库。

### 大模型配置

1. 安装LM-Studio并下载所需模型
2. 启动LM-Studio本地服务器
3. 在系统中选择对应的AI模型选项

## 许可证

本项目遵循 MIT 许可证。 
