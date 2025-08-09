package com.example.Kcsj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.Kcsj.entity.TimepointRecords;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 时间点检测记录Mapper
 */
@Mapper
public interface TimepointRecordsMapper extends BaseMapper<TimepointRecords> {
    @Select("SELECT * FROM timepointrecords ORDER BY id DESC LIMIT 10")
    List<TimepointRecords> findLatest10();
} 