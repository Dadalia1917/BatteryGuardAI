package com.example.Kcsj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * 时间段检测记录
 */
@Data
@ToString
@TableName("timeperiodrecords")
public class TimeperiodRecords implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 输入文件
     */
    @TableField("input_file")
    private String inputFile;

    /**
     * 结果文件
     */
    @TableField("result_file")
    private String resultFile;

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
     * 平均异常分数
     */
    @TableField("avg_score")
    private String avgScore;

    /**
     * 最大异常分数
     */
    @TableField("max_score")
    private String maxScore;

    /**
     * 使用的模型
     */
    private String model;

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