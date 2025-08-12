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
 * 时间段检测记录
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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

    // 手动添加 getter/setter 方法确保兼容性
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public String getResultFile() {
        return resultFile;
    }

    public void setResultFile(String resultFile) {
        this.resultFile = resultFile;
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

    public String getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(String avgScore) {
        this.avgScore = avgScore;
    }

    public String getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(String maxScore) {
        this.maxScore = maxScore;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
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