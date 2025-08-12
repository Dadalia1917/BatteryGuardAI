package com.example.Kcsj.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.TimepointRecords;
import com.example.Kcsj.mapper.TimepointRecordsMapper;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 时间点检测记录控制器
 */
@RestController
@RequestMapping("/timepointRecords")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TimepointRecordsController {

    @Resource
    TimepointRecordsMapper timepointRecordsMapper;

    /**
     * 新增或更新记录
     */
    @PostMapping
    public Result<?> save(@RequestBody TimepointRecords timepointRecords) {
        System.out.println("保存时间点检测记录: " + timepointRecords);
        timepointRecordsMapper.insert(timepointRecords);
        return Result.success();
    }

    /**
     * 查询全部记录
     */
    @GetMapping
    public Result<?> findAll() {
        System.out.println("查询所有时间点检测记录");
        List<TimepointRecords> list = timepointRecordsMapper.selectList(null);
        System.out.println("查询结果数量: " + list.size());
        return Result.success(list);
    }

    /**
     * 查询全部记录 - 首页使用的接口
     */
    @GetMapping("/all")
    public Result<?> findAllForHome() {
        System.out.println("【首页】查询所有时间点检测记录");
        try {
            QueryWrapper<TimepointRecords> wrapper = new QueryWrapper<>();
            wrapper.orderByDesc("id");
            wrapper.last("limit 20"); // 只返回最新的20条记录，避免数据量过大
            
            List<TimepointRecords> list = timepointRecordsMapper.selectList(wrapper);
            System.out.println("查询结果数量: " + list.size());
            return Result.success(list);
        } catch (Exception e) {
            System.err.println("【错误】查询所有时间点检测记录出错: " + e.getMessage());
            e.printStackTrace();
            return Result.error("-1", "查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID删除记录
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        System.out.println("删除时间点检测记录，ID: " + id);
        timepointRecordsMapper.deleteById(id);
        return Result.success();
    }

    /**
     * 分页查询记录
     */
    @GetMapping("/page")
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize,
                             @RequestParam(defaultValue = "") String search) {
        System.out.println("【分页查询】时间点检测记录，页码: " + pageNum + ", 每页记录数: " + pageSize + ", 搜索关键字: " + search);
        
        try {
            // 检查数据库连接
            System.out.println("数据库连接：尝试检查数据库表是否存在");
            Long count = timepointRecordsMapper.selectCount(null);
            System.out.println("数据库表中总记录数: " + count);
        
        // 构建查询条件
        QueryWrapper<TimepointRecords> wrapper = new QueryWrapper<>();
        if (!"".equals(search)) {
            wrapper.like("username", search);
        }
        wrapper.orderByDesc("id");
        
            // 执行查询
            System.out.println("执行分页查询...");
            Page<TimepointRecords> timepointRecordsPage = timepointRecordsMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
            
            System.out.println("查询结果总数: " + timepointRecordsPage.getTotal());
            System.out.println("查询结果当前页数据: " + timepointRecordsPage.getRecords().size());
            
            if (timepointRecordsPage.getRecords().size() > 0) {
                System.out.println("查询到的第一条记录: " + timepointRecordsPage.getRecords().get(0));
            }
            
            return Result.success(timepointRecordsPage);
        } catch (Exception e) {
            System.err.println("【错误】分页查询时间点检测记录出错: " + e.getMessage());
            e.printStackTrace();
            return Result.error("-1", "查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户名查询记录
     */
    @GetMapping("/username/{username}")
    public Result<?> findByUsername(@PathVariable String username) {
        System.out.println("根据用户名查询时间点检测记录，用户名: " + username);
        QueryWrapper<TimepointRecords> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        wrapper.orderByDesc("id");
        
        List<TimepointRecords> list = timepointRecordsMapper.selectList(wrapper);
        System.out.println("查询结果数量: " + list.size());
        
        return Result.success(list);
    }
} 