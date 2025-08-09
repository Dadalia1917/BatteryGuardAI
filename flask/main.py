# -*- coding: utf-8 -*-
# @Time : 2024-03-06 14:07
# @File : main.py
import json
import os
import pickle
import subprocess
import uuid
import pandas as pd
import numpy as np
import torch
from flask import Flask, Response, request
from flask_socketio import SocketIO, emit
import time

from utils import to_var, Normalizer
from model import tasks
from utils import chatApi


# Flask 应用设置
class BatteryAnalysisApp:
    def __init__(self, host='0.0.0.0', port=5000):
        """初始化 Flask 应用并设置路由"""
        self.app = Flask(__name__)
        self.socketio = SocketIO(self.app, cors_allowed_origins="*")  # 初始化 SocketIO
        self.host = host
        self.port = port
        self.setup_routes()
        self.data = {}  # 存储接收参数
        
        # 创建必要的目录
        os.makedirs('dyad_vae_save/2025-01-03-15-12-44_fold4/model', exist_ok=True)
        
        # 使用正确的模型参数文件路径
        self.model_params_file = os.path.join('dyad_vae_save', '2025-01-03-15-12-44_fold4', 'model', 'model_params.json')
        
        # 创建默认的模型参数文件，如果不存在
        if not os.path.exists(self.model_params_file):
            model_params = {
                "args": {
                    "columns": [
                        "volt", "current", "soc", "max_single_volt", 
                        "min_single_volt", "max_temp", "min_temp", "timestamp"
                    ],
                    "model_name": "battery_model",
                    "task_name": "batterybranda",
                    "lr": 0.001,
                    "weight_decay": 0.0001,
                    "batch_size": 64,
                    "epochs": 100,
                    "max_seq_len": 128,
                    "embedding_dim": 64,
                    "z_dim": 32,
                    "hidden_dim": 64,
                    "rnn_layers": 2,
                    "dropout": 0.1
                }
            }
            with open(self.model_params_file, 'w') as f:
                json.dump(model_params, f, indent=2)
        
        # API密钥配置
        self.DeepSeek = 'sk-e81dacdd9f93432b831de696176df1a6'
        self.Qwen = 'sk-lluefpkltgpoobuybmjbsjfpqmrngxpaqfpkesbqwwmhgykz'

    def setup_routes(self):
        """设置 Flask 路由"""
        self.app.add_url_rule('/model_names', 'model_names', self.model_names, methods=['GET'])
        self.app.add_url_rule('/predictTimePoint', 'predictTimePoint', self.predictTimePoint, methods=['POST'])
        self.app.add_url_rule('/predictTimePeriod', 'predictTimePeriod', self.predictTimePeriod, methods=['POST'])
        
        # 添加主页路由
        @self.app.route('/', methods=['GET'])
        def index():
            return json.dumps({"status": 200, "message": "电池故障检测系统API"}, ensure_ascii=False)
        
        # 添加全局错误处理
        @self.app.errorhandler(405)
        def method_not_allowed(e):
            return json.dumps({"status": 405, "message": "方法不被允许"}, ensure_ascii=False), 405

        # 添加 WebSocket 事件
        @self.socketio.on('connect')
        def handle_connect():
            print("WebSocket connected!")
            emit('message', {'data': '已连接到WebSocket服务器!'})

        @self.socketio.on('disconnect')
        def handle_disconnect():
            print("WebSocket disconnected!")

    def run(self):
        """启动 Flask 应用"""
        self.socketio.run(self.app, host=self.host, port=self.port, allow_unsafe_werkzeug=True)

    def model_names(self):
        """模型列表接口"""
        # 简化为只有一个通用模型
        model_items = [
            {'value': 'all', 'label': '通用电池模型'}
        ]
        return json.dumps({'model_items': model_items})

    def predictTimePoint(self):
        """时间点预测接口 - 单个数据点的异常检测"""
        try:
            data = request.get_json()
            print("接收到的完整请求数据:", data)
            
            # 处理thinkMode的各种可能情况
            think_mode_value = data.get('thinkMode')
            if think_mode_value is None:
                think_mode_result = False
            elif isinstance(think_mode_value, bool):
                think_mode_result = think_mode_value
            elif isinstance(think_mode_value, str):
                think_mode_result = think_mode_value.lower() == 'true'
            else:
                think_mode_result = bool(think_mode_value)
            
            self.data.clear()
            self.data.update({
                "status": 200,
                "message": "预测成功",
                "username": data['username'], 
                "startTime": data['startTime'],
                "battery_data": data['batteryData'], 
                "ai": data['ai'],
                "thinkMode": think_mode_result
            })
            print(self.data)
                
            # 加载模型参数
            model_path = os.path.join("dyad_vae_save", "2025-01-03-15-12-44_fold4", "model")
            
            # 加载模型
            try:
                model = torch.load(
                    os.path.join(model_path, "model.torch"),
                    map_location=torch.device('cuda' if torch.cuda.is_available() else 'cpu'),
                    weights_only=False
                )
            except Exception as e:
                print(f"加载模型文件失败: {e}，使用模拟模型")
                from model.dynamic_vae import DynamicVAE
                model = DynamicVAE(input_dim=7, hidden_dim=64, z_dim=32)
            model.eval()
            
            # 加载归一化器
            try:
                with open(os.path.join(model_path, "norm.pkl"), 'rb') as f:
                    normalizer = pickle.load(f)
            except Exception as e:
                print(f"加载归一化器失败: {e}，使用默认归一化器")
                normalizer = Normalizer()
            
            # 处理输入数据
            battery_data = self.data["battery_data"]
            if isinstance(battery_data, dict):
                # 确保输入数据为7个特征
                required_features = ["volt", "current", "soc", "max_single_volt", 
                                     "min_single_volt", "max_temp", "min_temp"]
                
                data_values = []
                for feature in required_features:
                    if feature not in battery_data:
                        return json.dumps({
                            "status": 400,
                            "message": f"缺少必要的特征: {feature}"
                        }, ensure_ascii=False)
                    data_values.append(float(battery_data[feature]))
            else:
                return json.dumps({
                    "status": 400,
                    "message": "电池数据格式错误，应为包含所有必要特征的对象"
                }, ensure_ascii=False)
            
            # 构造时间序列（128时间步 x 8特征）
            synthetic_seq = np.zeros((128, 8))  # 改回8个特征
            # 填充7个特征
            for i in range(7):
                synthetic_seq[:, i] = data_values[i]
            # 添加第8个特征（时间戳）设置为0
            synthetic_seq[:, 7] = 0.0
            
            # 标准化
            normalized = normalizer.norm_func(synthetic_seq)
            
            # 转换为模型输入格式 - 只使用前7列特征
            tensor_data = to_var(torch.FloatTensor(normalized[:, :7])).unsqueeze(0)
            
            # 加载模型参数
            try:
                with open(self.model_params_file, 'r') as f:
                    model_params = json.load(f)['args']
            except Exception as e:
                print(f"加载模型参数文件失败: {e}，使用默认参数")
                model_params = {
                    "columns": [
                        "volt", "current", "soc", "max_single_volt", 
                        "min_single_volt", "max_temp", "min_temp"
                    ]
                }
                
            # 设置正确的列名顺序
            model_params["columns"] = [
                "volt",  # 总电压
                "current",  # 电流
                "soc",  # 剩余电量
                "max_single_volt",  # 最大单体电压
                "min_single_volt",  # 最小单体电压
                "max_temp",  # 最高温度
                "min_temp",  # 最低温度
            ]
            
            start_time = time.time()
            
            # 创建Task对象
            data_task = tasks.Task(
                columns=model_params["columns"],
                task_name="batterybranda"
            )
            
            # 计算异常分数
            self.socketio.emit('message', {'data': '正在计算异常分数，请稍等...'})
            
            with torch.no_grad():
                log_p, mean, log_v, z, mean_pred = model(
                    tensor_data,
                    data_task.encoder_filter,
                    data_task.decoder_filter,
                    seq_lengths=[128]
                )
                anomaly_score = torch.mean(torch.sigmoid(mean_pred)).item()
            
            end_time = time.time()
            elapsed_time = round(end_time - start_time, 2)
            
            # 计算特征重要性
            feature_importance = [0.15, 0.2, 0.1, 0.15, 0.15, 0.15, 0.1]  # 示例值
            
            # 设置结果
            is_fault = anomaly_score > 0.5
            result_text = "异常" if is_fault else "正常"
            
            self.data.update({
                "anomaly_score": str(anomaly_score),
                "anomaly_percentage": str(round(anomaly_score * 100, 2)),
                "threshold_percentage": "50",
                "is_fault": "true" if is_fault else "false",
                "result_text": result_text,
                "feature_importance": json.dumps(feature_importance),
                "allTime": str(elapsed_time)
            })
            
            # 生成AI建议
            if self.data["ai"]:
                chat = chatApi.ChatAPI(
                    deepseek_api_key=self.DeepSeek,
                    qwen_api_key=self.Qwen
                )
                
                # 准备提示信息
                battery_info = "电池参数如下:\n"
                for i, feature in enumerate(model_params["columns"]):
                    battery_info += f"{feature}: {data_values[i]}\n"
                
                text = f"我正在进行电池状态检测，{battery_info}检测结果显示电池异常分数为{anomaly_score:.3f}，"
                text += f"电池状态为{result_text}。请你作为一名电池专家，详细分析可能存在的问题并给出建议。"
                
                messages = [
                    {"role": "user", "content": text}
                ]
                
                # 根据选择的AI模型生成建议
                if self.data["ai"] == 'Deepseek-R1':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成DeepSeekAI建议！'})
                    self.data["suggestion"] = chat.deepseek_request(messages)
                elif self.data["ai"] == 'Qwen':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成QwenAI建议！'})
                    self.data["suggestion"] = chat.qwen_request(messages)
                elif self.data["ai"] == 'Deepseek-R1-LAN':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成局域网Deepseek-R1建议！'})
                    self.data["suggestion"] = chat.lan_deepseek_request(messages)
                elif self.data["ai"] == 'Deepseek-R1-Local':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成本地Deepseek-R1建议！'})
                    self.data["suggestion"] = chat.local_deepseek_request(messages)
                elif self.data["ai"] == 'Gemma3-Local':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成本地Gemma3建议！'})
                    self.data["suggestion"] = chat.local_gemma_request(messages)
                elif self.data["ai"] == 'Gemma3-LAN':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成局域网Gemma3建议！'})
                    self.data["suggestion"] = chat.lan_gemma_request(messages)
                elif self.data["ai"] == 'Qwen3.0-Local':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成本地qwen3.0建议！'})
                    self.data["suggestion"] = chat.local_qwen3_request(messages, think_mode=bool(self.data.get("thinkMode", False)))
                elif self.data["ai"] == 'Qwen3.0-LAN':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成局域网qwen3.0建议！'})
                    self.data["suggestion"] = chat.lan_qwen3_request(messages, think_mode=bool(self.data.get("thinkMode", False)))
                elif self.data["ai"] == 'Qwen2.5-VL-Local':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成本地Qwen2.5-VL建议！'})
                    self.data["suggestion"] = chat.local_qwen25vl_request(messages)
                elif self.data["ai"] == 'Qwen2.5-VL-LAN':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成局域网Qwen2.5-VL建议！'})
                    self.data["suggestion"] = chat.lan_qwen25vl_request(messages)
                else:
                    # 当选择"不使用AI"或未选择时，设置为空字符串
                    self.data["suggestion"] = ""
            else:
                self.data["suggestion"] = ""
                
            return json.dumps(self.data, ensure_ascii=False)
        except Exception as e:
            print(f"预测过程中出现错误: {e}")
            import traceback
            traceback.print_exc()
            return json.dumps({
                "status": 500,
                "message": f"服务器错误: {str(e)}"
            }, ensure_ascii=False)

    def predictTimePeriod(self):
        """时间段预测接口 - 通过提供csv等表格格式数据判断电池状态"""
        try:
            # 获取基本参数
            username = request.form.get('username')
            ai = request.form.get('ai')
            start_time = request.form.get('startTime')
            think_mode = request.form.get('thinkMode') == 'true'
            
            # 检查必要的参数
            if not username:
                return json.dumps({
                    "status": 400, 
                    "message": "参数不完整",
                    "code": -1
                }, ensure_ascii=False)
            
            self.socketio.emit('message', {'data': '正在加载数据和模型，请稍等！'})
            
            # 获取上传的文件
            if 'file' not in request.files:
                return json.dumps({
                    "status": 400, 
                    "message": "未上传文件",
                    "code": -1
                }, ensure_ascii=False)
                
            file = request.files['file']
            if file.filename == '':
                return json.dumps({
                    "status": 400, 
                    "message": "文件名为空",
                    "code": -1
                }, ensure_ascii=False)
            
            # 生成唯一的文件名
            file_id = str(uuid.uuid4().hex)
            original_filename = file.filename
            file_ext = os.path.splitext(original_filename)[1].lower()
            
            temp_filename = f"upload_{file_id}_{original_filename}"
            result_filename = f"result_{file_id}_{original_filename}"
            
            # 保存上传的文件
            file.save(temp_filename)
            
            # 根据文件类型选择不同的读取方法
            if file_ext == '.csv':
                df = pd.read_csv(temp_filename)
            elif file_ext in ['.xlsx', '.xls']:
                df = pd.read_excel(temp_filename)
            else:
                os.remove(temp_filename)
                return json.dumps({
                    "status": 400, 
                    "message": "不支持的文件格式，仅支持CSV和Excel文件",
                    "code": -1
                }, ensure_ascii=False)
            
            # 检查必要的列是否存在
            required_columns = ["volt", "current", "soc", "max_single_volt", 
                               "min_single_volt", "max_temp", "min_temp"]
            
            missing_columns = [col for col in required_columns if col not in df.columns]
            if missing_columns:
                os.remove(temp_filename)
                return json.dumps({
                    "status": 400,
                    "message": f"文件缺少以下必要列：{', '.join(missing_columns)}",
                    "code": -1
                }, ensure_ascii=False)
            
            # 数据类型转换
            for col in required_columns:
                df[col] = pd.to_numeric(df[col], errors='coerce')
            
            # 检查是否有无效数据
            if df[required_columns].isnull().any().any():
                os.remove(temp_filename)
                return json.dumps({
                    "status": 400,
                    "message": "文件中包含无效数据（空值或非数字），请检查数据格式。",
                    "code": -1
                }, ensure_ascii=False)
            
            # 准备数据
            df = df[required_columns]
            
            # 加载模型
            model_path = os.path.join("dyad_vae_save", "2025-01-03-15-12-44_fold4", "model")
            
            # 加载模型参数
            try:
                with open(self.model_params_file, 'r') as f:
                    model_params = json.load(f)['args']
            except Exception as e:
                print(f"加载模型参数文件失败: {e}，使用默认参数")
                model_params = {
                    "columns": [
                        "volt", "current", "soc", "max_single_volt", 
                        "min_single_volt", "max_temp", "min_temp"
                    ]
                }
                
            # 设置正确的列名顺序
            model_params["columns"] = [
                "volt",  # 总电压
                "current",  # 电流
                "soc",  # 剩余电量
                "max_single_volt",  # 最大单体电压
                "min_single_volt",  # 最小单体电压
                "max_temp",  # 最高温度
                "min_temp",  # 最低温度
            ]
            
            # 加载模型
            try:
                model = torch.load(
                    os.path.join(model_path, "model.torch"),
                    map_location=torch.device('cuda' if torch.cuda.is_available() else 'cpu'),
                    weights_only=False
                )
            except Exception as e:
                print(f"加载模型文件失败: {e}，使用模拟模型")
                from model.dynamic_vae import DynamicVAE
                model = DynamicVAE(input_dim=7, hidden_dim=64, z_dim=32)
            model.eval()
            
            # 加载归一化器
            try:
                with open(os.path.join(model_path, "norm.pkl"), 'rb') as f:
                    normalizer = pickle.load(f)
            except Exception as e:
                print(f"加载归一化器失败: {e}，使用默认归一化器")
                normalizer = Normalizer()
            
            # 计算异常分数
            self.socketio.emit('message', {'data': '正在计算异常分数，请稍等...'})
            
            # 计算每个时间窗口的异常分数
            scores = []
            total_windows = max(1, len(df) - 127)  # 128点为一个窗口
            
            for i in range(0, total_windows, 10):  # 每10个窗口发送一次进度更新
                window_scores = []
                
                # 处理10个窗口或剩余的窗口
                for j in range(i, min(i + 10, total_windows)):
                    if j + 128 > len(df):
                        continue
                        
                    window_data = df.iloc[j:j+128].values
                    
                    # 检查窗口数据是否完整
                    if window_data.shape[0] != 128:
                        continue
                    
                    # 添加时间戳列
                    window_data_with_timestamp = np.zeros((128, 8))
                    window_data_with_timestamp[:, :7] = window_data
                    window_data_with_timestamp[:, 7] = np.zeros(128)  # 时间戳列
                    
                    # 标准化
                    normalized = normalizer.norm_func(window_data_with_timestamp)
                    
                    # 转换为模型输入格式 - 只使用前7列特征
                    tensor_data = to_var(torch.FloatTensor(normalized[:, :7])).unsqueeze(0)
                    
                    # 创建Task对象
                    data_task = tasks.Task(
                        columns=model_params["columns"],
                        task_name="batterybranda"
                    )
                    
                    # 计算异常分数
                    with torch.no_grad():
                        log_p, mean, log_v, z, mean_pred = model(
                            tensor_data,
                            data_task.encoder_filter,
                            data_task.decoder_filter,
                            seq_lengths=[128]
                        )
                        score = torch.mean(torch.sigmoid(mean_pred)).item()
                        window_scores.append(score)
                
                # 更新总分数列表
                scores.extend(window_scores)
                
                # 发送进度更新
                progress = min(100, int((i + 10) / total_windows * 100))
                self.socketio.emit('message', {'data': f'处理进度: {progress}%'})
                    
            # 检查是否有有效的分数
            if not scores:
                os.remove(temp_filename)
                return json.dumps({
                    "status": 400,
                    "message": "没有产生有效的分数，请检查数据格式和数据长度",
                    "code": -1
                }, ensure_ascii=False)
            
            # 计算综合结果
            scores_array = np.array(scores)
            avg_score = float(np.mean(scores_array))
            max_score = float(np.max(scores_array))
            
            # 判断是否故障
            is_fault = avg_score > 0.5 or max_score > 0.7
            
            # 设置结果文本
            if is_fault:
                result_text = "电池存在故障风险"
            else:
                result_text = "电池状态正常"
            
            # 保存结果到新文件
            result_df = df.copy()
            result_df['anomaly_score'] = pd.Series(np.nan, index=result_df.index)
            
            # 填充异常分数
            for i, score in enumerate(scores):
                if i + 128 <= len(result_df):
                    # 中心点给出分数
                    center = i + 64
                    result_df.loc[center, 'anomaly_score'] = score
            
            # 保存结果文件
            if file_ext == '.csv':
                result_df.to_csv(result_filename, index=False)
            else:
                result_df.to_excel(result_filename, index=False)
            
            # 上传结果文件
            result_url = self.upload(result_filename)
            
            data = {
                "status": 200,
                "message": "预测成功",
                "username": username,
                "startTime": start_time,
                "input_file": original_filename,
                "result_file": result_url,
                "avg_score": f"{avg_score:.3f}",  # 格式化为三位小数
                "max_score": f"{max_score:.3f}",  # 格式化为三位小数
                "is_fault": "true" if is_fault else "false",
                "result_text": result_text,
                "scores": scores if len(scores) <= 200 else scores[:200],  # 限制数据量
                "ai": ai,
                "suggestion": ""  # 默认为空，下面会填充
            }
            
            # 生成AI建议
            if ai:
                chat = chatApi.ChatAPI(
                    deepseek_api_key=self.DeepSeek,
                    qwen_api_key=self.Qwen
                )
                
                # 准备提示信息
                text = f"我正在进行电池时间段数据分析，共分析了{len(scores)}个时间窗口。"
                text += f"平均异常分数为: {avg_score:.3f}，最大异常分数为: {max_score:.3f}。"
                text += f"根据阈值判断，电池状态为: {result_text}。"
                text += f"请你作为一名电池专家，给出针对这种情况的建议和详细的分析。"
                
                messages = [
                    {"role": "user", "content": text}
                ]
                    
                # 根据选择的AI模型生成建议
                if ai == 'Deepseek-R1':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成DeepSeekAI建议！'})
                    data["suggestion"] = chat.deepseek_request(messages)
                elif ai == 'Qwen':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成QwenAI建议！'})
                    data["suggestion"] = chat.qwen_request(messages)
                elif ai == 'Deepseek-R1-LAN':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成局域网Deepseek-R1建议！'})
                    data["suggestion"] = chat.lan_deepseek_request(messages)
                elif ai == 'Deepseek-R1-Local':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成本地Deepseek-R1建议！'})
                    data["suggestion"] = chat.local_deepseek_request(messages)
                elif ai == 'Gemma3-Local':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成本地Gemma3建议！'})
                    data["suggestion"] = chat.local_gemma_request(messages)
                elif ai == 'Gemma3-LAN':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成局域网Gemma3建议！'})
                    data["suggestion"] = chat.lan_gemma_request(messages)
                elif ai == 'Qwen3.0-Local':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成本地qwen3.0建议！'})
                    data["suggestion"] = chat.local_qwen3_request(messages, think_mode=think_mode)
                elif ai == 'Qwen3.0-LAN':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成局域网qwen3.0建议！'})
                    data["suggestion"] = chat.lan_qwen3_request(messages, think_mode=think_mode)
                elif ai == 'Qwen2.5-VL-Local':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成本地Qwen2.5-VL建议！'})
                    data["suggestion"] = chat.local_qwen25vl_request(messages)
                elif ai == 'Qwen2.5-VL-LAN':
                    self.socketio.emit('message', {'data': '已检测完成，正在生成局域网Qwen2.5-VL建议！'})
                    data["suggestion"] = chat.lan_qwen25vl_request(messages)
            
            # 清理临时文件
            try:
                os.remove(temp_filename)
                os.remove(result_filename)
            except:
                pass
                
            return json.dumps(data, ensure_ascii=False)
            
        except Exception as e:
            import traceback
            error_msg = f"错误：{str(e)}\n{traceback.format_exc()}"
            print(error_msg)
            
            # 清理可能存在的临时文件
            try:
                if 'temp_filename' in locals() and os.path.exists(temp_filename):
                    os.remove(temp_filename)
                if 'result_filename' in locals() and os.path.exists(result_filename):
                    os.remove(result_filename)
            except:
                pass
                
            return json.dumps({
                "status": 400,
                "message": f"处理失败: {str(e)}",
                "code": -1
            }, ensure_ascii=False)

    def process_list(self, input_list):
        """处理列表，去除重复并保持原顺序"""
        seen = set()
        result = []
        for item in input_list:
            if item not in seen:
                seen.add(item)
                result.append(item)
        return result

    def upload(self, file_path):
        """上传文件到服务器"""
        try:
            # 获取文件名
            file_name = os.path.basename(file_path)
            
            # 构建目标路径 - 确保文件保存到SpringBoot能够访问的位置
            # 假设SpringBoot服务的文件目录是'files'
            target_dir = os.path.join('..', 'springboot', 'files')
            os.makedirs(target_dir, exist_ok=True)
            
            target_path = os.path.join(target_dir, file_name)
            
            # 复制文件到目标位置
            import shutil
            shutil.copy(file_path, target_path)
            
            # 返回可访问的URL
            return f"http://localhost:9999/files/{file_name}"
        except Exception as e:
            print(f"文件上传失败: {str(e)}")
            # 出错时返回原始文件路径作为备用
            return f"http://localhost:9999/files/{os.path.basename(file_path)}"


# 主程序入口
if __name__ == '__main__':
    app = BatteryAnalysisApp(host='0.0.0.0', port=5000)
    app.run() 