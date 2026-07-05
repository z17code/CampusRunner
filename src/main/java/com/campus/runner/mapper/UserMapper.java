package com.campus.runner.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.runner.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}