package com.example.Kcsj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.Kcsj.entity.User;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper extends BaseMapper<User> {
    @Select("select * from user where username=#{username}")
    List<User> selectByName(String username);


    @Select("select * from user where username = #{username}")
    List<User> selectByUsername(String username);
}
