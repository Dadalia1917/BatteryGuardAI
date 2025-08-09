package com.example.Kcsj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * 时间点检测记录
 */
@Data
@ToString
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
} 