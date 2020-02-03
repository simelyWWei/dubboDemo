package com.mydubbo.service.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mydubbo.service.entity.DemoClass;
import com.mydubbo.service.mapper.DemoClassMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DemoClassService extends ServiceImpl<DemoClassMapper, DemoClass> {

    @Autowired
    private DemoClassService demoClassService;

    public DemoClass getDemoOne() {
        Long id = 1231231231L;
        return demoClassService.getById(id);
    }
}
