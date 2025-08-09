package com.example.Kcsj.controller;
import com.alibaba.fastjson.JSONObject;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.TimepointRecords;
import com.example.Kcsj.entity.TimeperiodRecords;
import com.example.Kcsj.mapper.TimepointRecordsMapper;
import com.example.Kcsj.mapper.TimeperiodRecordsMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.util.LinkedMultiValueMap;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;

@RestController
@RequestMapping("/flask")
public class PredictionController {
    @javax.annotation.Resource
    TimepointRecordsMapper timepointRecordsMapper;
    
    @javax.annotation.Resource
    TimeperiodRecordsMapper timeperiodRecordsMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    // 定义接收的参数类
    public static class PredictRequest {
        private String startTime;
        private String conf;
        private String username;
        private String ai;
        private String suggestion;
        private Boolean thinkMode;
        private Object batteryData;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getConf() {
            return conf;
        }

        public void setConf(String conf) {
            this.conf = conf;
        }

        public String getAi() {
            return ai;
        }

        public void setAi(String ai) {
            this.ai = ai;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }

        public Boolean getThinkMode() {
            return thinkMode;
        }

        public void setThinkMode(Boolean thinkMode) {
            this.thinkMode = thinkMode;
        }
        
        public Object getBatteryData() {
            return batteryData;
        }

        public void setBatteryData(Object batteryData) {
            this.batteryData = batteryData;
        }
    }

    @PostMapping("/predict")
    public Result<?> predict(@RequestBody PredictRequest request) {
        if (request == null || request.getBatteryData() == null) {
            return Result.error("-1", "未提供电池数据");
        }

        try {
            // 将batteryData转换为JSON字符串
            String batteryDataJson = JSONObject.toJSONString(request.getBatteryData());
            
            // 创建请求体
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 创建新的请求对象，确保数据格式正确
            PredictRequest modifiedRequest = new PredictRequest();
            modifiedRequest.setUsername(request.getUsername());
            modifiedRequest.setStartTime(request.getStartTime());
            modifiedRequest.setConf(request.getConf());
            modifiedRequest.setAi(request.getAi());
            modifiedRequest.setThinkMode(request.getThinkMode());
            modifiedRequest.setBatteryData(request.getBatteryData());
            
            HttpEntity<PredictRequest> requestEntity = new HttpEntity<>(modifiedRequest, headers);

            // 调用 Flask API
            String response = restTemplate.postForObject("http://localhost:5000/predictTimePoint", requestEntity, String.class);
            System.out.println("Received response: " + response);
            JSONObject responses = JSONObject.parseObject(response);
            if(responses.get("status").equals(400)){
                return Result.error("-1", "Error: " + responses.get("message"));
            }else {
                TimepointRecords timepointRecords = new TimepointRecords();
                timepointRecords.setConf(request.getConf());
                timepointRecords.setBatteryData(batteryDataJson);
                timepointRecords.setUsername(request.getUsername());
                timepointRecords.setStartTime(request.getStartTime());
                timepointRecords.setAi(request.getAi());
                timepointRecords.setIsFault(String.valueOf(responses.get("is_fault")));
                timepointRecords.setAnomalyScore(String.valueOf(responses.get("anomaly_score")));
                timepointRecords.setFeatureImportance(String.valueOf(responses.get("feature_importance")));
                timepointRecords.setAllTime(String.valueOf(responses.get("allTime")));
                timepointRecords.setSuggestion(String.valueOf(responses.get("suggestion")));
                timepointRecordsMapper.insert(timepointRecords); // 插入到数据库
                return Result.success(response);
            }
        } catch (Exception e) {
            return Result.error("-1", "Error: " + e.getMessage());
        }
    }

    @PostMapping("/predictTimePoint")
    public Result<?> predictTimePoint(@RequestBody PredictRequest request) {
        if (request == null || request.getBatteryData() == null) {
            return Result.error("-1", "未提供电池数据");
        }

        try {
            // 将batteryData转换为JSON字符串
            String batteryDataJson = JSONObject.toJSONString(request.getBatteryData());
            
            // 创建请求体
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 创建新的请求对象，确保数据格式正确
            PredictRequest modifiedRequest = new PredictRequest();
            modifiedRequest.setUsername(request.getUsername());
            modifiedRequest.setStartTime(request.getStartTime());
            modifiedRequest.setConf(request.getConf());
            modifiedRequest.setAi(request.getAi());
            modifiedRequest.setThinkMode(request.getThinkMode());
            modifiedRequest.setBatteryData(request.getBatteryData());
            
            HttpEntity<PredictRequest> requestEntity = new HttpEntity<>(modifiedRequest, headers);

            // 调用 Flask API
            String response = restTemplate.postForObject("http://localhost:5000/predictTimePoint", requestEntity, String.class);
            System.out.println("Received response from Flask: " + response);
            JSONObject responses = JSONObject.parseObject(response);
            if(responses.get("status").equals(400)){
                return Result.error("-1", "Error: " + responses.get("message"));
            }else {
                TimepointRecords timepointRecords = new TimepointRecords();
                timepointRecords.setConf(request.getConf());
                timepointRecords.setBatteryData(batteryDataJson);
                timepointRecords.setUsername(request.getUsername());
                timepointRecords.setStartTime(request.getStartTime());
                timepointRecords.setAi(request.getAi());
                timepointRecords.setIsFault(String.valueOf(responses.get("is_fault")));
                timepointRecords.setAnomalyScore(String.valueOf(responses.get("anomaly_score")));
                timepointRecords.setFeatureImportance(String.valueOf(responses.get("feature_importance")));
                timepointRecords.setAllTime(String.valueOf(responses.get("allTime")));
                timepointRecords.setSuggestion(String.valueOf(responses.get("suggestion")));
                
                // 输出完整记录对象以便调试
                System.out.println("准备插入的完整TimepointRecords对象: " + timepointRecords);
                
                // 尝试保存到数据库
                try {
                    System.out.println("准备保存时间点检测记录到数据库");
                    int result = timepointRecordsMapper.insert(timepointRecords);
                    System.out.println("保存时间点检测记录结果: " + result);
                    
                    // 验证记录是否保存成功
                    QueryWrapper<TimepointRecords> wrapper = new QueryWrapper<>();
                    wrapper.eq("username", request.getUsername())
                           .eq("start_time", request.getStartTime())
                           .orderByDesc("id");
                    wrapper.last("LIMIT 1");
                    TimepointRecords savedRecord = timepointRecordsMapper.selectOne(wrapper);
                    System.out.println("数据库中查询到的记录ID: " + (savedRecord != null ? savedRecord.getId() : "null"));
                    
                    if (savedRecord == null) {
                        System.err.println("警告：无法在数据库中找到刚插入的记录");
                    } else {
                        System.out.println("记录保存成功，ID: " + savedRecord.getId());
                        // 将新记录的ID添加到响应中
                        responses.put("record_id", savedRecord.getId());
                    }
                } catch (Exception e) {
                    System.err.println("保存时间点检测记录失败: " + e.getMessage());
                    e.printStackTrace();
                    // 记录失败但不影响返回结果
                }
                
                // 直接返回Flask响应对象，避免数据格式转换问题
                return Result.success(responses);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("-1", "Error: " + e.getMessage());
        }
    }

    @GetMapping("/model_names")
    public Result<?> getModelNames() {
        try {
            // 调用 Flask API
            String response = restTemplate.getForObject("http://127.0.0.1:5000/model_names", String.class);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("-1", "Error: " + e.getMessage());
        }
    }

    @PostMapping("/predictTimePeriod")
    public Result<?> predictTimePeriod(@RequestParam("username") String username,
                                     @RequestParam("startTime") String startTime,
                                     @RequestParam("ai") String ai,
                                     @RequestParam(value = "thinkMode", required = false) Boolean thinkMode,
                                     @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("-1", "未提供文件");
        }

        try {
            // 创建临时文件
            String originalFilename = file.getOriginalFilename();
            File tempFile = File.createTempFile("upload_", "_" + originalFilename);
            file.transferTo(tempFile);
            
            // 设置Form表单数据
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("username", username);
            body.add("startTime", startTime);
            body.add("ai", ai);
            body.add("thinkMode", thinkMode != null && thinkMode ? "true" : "false");
            
            // 添加文件
            org.springframework.core.io.Resource fileResource = new FileSystemResource(tempFile);
            body.add("file", fileResource);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // 调用Flask API
            String response = restTemplate.postForObject("http://localhost:5000/predictTimePeriod", requestEntity, String.class);
            System.out.println("Received response from Flask: " + response);
            JSONObject responseObj = JSONObject.parseObject(response);
            
            if ("400".equals(String.valueOf(responseObj.get("status")))) {
                return Result.error("-1", "Error: " + responseObj.get("message"));
            } else {
                // 保存到数据库
                TimeperiodRecords record = new TimeperiodRecords();
                record.setUsername(username);
                record.setStartTime(startTime);
                record.setInputFile(String.valueOf(responseObj.get("input_file")));
                record.setResultFile(String.valueOf(responseObj.get("result_file")));
                record.setAvgScore(String.valueOf(responseObj.get("avg_score")));
                record.setMaxScore(String.valueOf(responseObj.get("max_score")));
                record.setIsFault(String.valueOf(responseObj.get("is_fault")));
                record.setAi(ai);
                record.setSuggestion(String.valueOf(responseObj.get("suggestion")));
                record.setModel("all"); // 使用通用模型
                
                // 打印记录信息用于调试
                System.out.println("即将保存的时间段检测记录: " + record);
                
                // 尝试保存到数据库
                try {
                    int result = timeperiodRecordsMapper.insert(record);
                    System.out.println("保存时间段检测记录结果: " + result);
                    
                    // 验证记录是否保存成功
                    QueryWrapper<TimeperiodRecords> wrapper = new QueryWrapper<>();
                    wrapper.eq("username", username)
                           .eq("start_time", startTime)
                           .orderByDesc("id");
                    wrapper.last("LIMIT 1");
                    TimeperiodRecords savedRecord = timeperiodRecordsMapper.selectOne(wrapper);
                    System.out.println("数据库中查询到的记录: " + savedRecord);
                } catch (Exception e) {
                    System.err.println("保存时间段检测记录失败: " + e.getMessage());
                    e.printStackTrace();
                }
                
                // 直接返回Flask响应对象，避免数据格式转换问题
                return Result.success(responseObj);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("-1", "Error: " + e.getMessage());
        }
    }

    @GetMapping("/predict")
    public Result<?> getPredictions() {
        try {
            // 获取最新的10条时间点检测记录
            return Result.success(timepointRecordsMapper.findLatest10());
        } catch (Exception e) {
            return Result.error("-1", "Error: " + e.getMessage());
        }
    }
    
    @RequestMapping("/")
    @GetMapping("")
    public Result<?> index() {
        try {
            return Result.success("电池故障检测系统API");
        } catch (Exception e) {
            return Result.error("-1", "Error: " + e.getMessage());
        }
    }
}
