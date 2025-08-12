package com.example.Kcsj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 时间点检测记录
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("timepointrecords")
public class TimepointRecords implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 电池数据
     */
    @TableField("battery_data")
    private String batteryData;

    /**
     * 异常分数
     */
    @TableField("anomaly_score")
    private String anomalyScore;

    /**
     * 特征重要性
     */
    @TableField("feature_importance")
    private String featureImportance;

    /**
     * 检测用时
     */
    @TableField("all_time")
    private String allTime;

    /**
     * 置信度/阈值
     */
    private String conf;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 用户名
     */
    private String username;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private String startTime;

    /**
     * 是否故障
     */
    @TableField("is_fault")
    private String isFault;

    /**
     * 使用的AI
     */
    private String ai;

    /**
     * AI建议
     */
    private String suggestion;

    // 手动添加 getter/setter 方法确保兼容性
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBatteryData() {
        return batteryData;
    }

    public void setBatteryData(String batteryData) {
        this.batteryData = batteryData;
    }

    public String getAnomalyScore() {
        return anomalyScore;
    }

    public void setAnomalyScore(String anomalyScore) {
        this.anomalyScore = anomalyScore;
    }

    public String getFeatureImportance() {
        return featureImportance;
    }

    public void setFeatureImportance(String featureImportance) {
        this.featureImportance = featureImportance;
    }

    public String getAllTime() {
        return allTime;
    }

    public void setAllTime(String allTime) {
        this.allTime = allTime;
    }

    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

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

    public String getIsFault() {
        return isFault;
    }

    public void setIsFault(String isFault) {
        this.isFault = isFault;
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
}