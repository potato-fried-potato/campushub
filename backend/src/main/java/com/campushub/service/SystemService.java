package com.campushub.service;

import com.campushub.mapper.SystemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SystemService {

    @Autowired
    private SystemMapper systemMapper;

    public Map<String, Object> getPlatformStats() {
        return systemMapper.getPlatformStats();
    }

}
