package com.campushub.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface SystemMapper {

    // 平台统计数据（AC-8.1）
    Map<String, Object> getPlatformStats();

}
