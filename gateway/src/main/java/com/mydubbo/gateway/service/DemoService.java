package com.mydubbo.gateway.service;

import com.mydubbo.api.VO.DemoClassVO;
import com.mydubbo.api.api.DemoApi;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.stereotype.Service;

@Service
public class DemoService {

    @Reference(interfaceClass = DemoApi.class,check = false)
    private DemoApi demoApi;

    public DemoClassVO showDemoClass() {
        return demoApi.showDemo();
    }
}
