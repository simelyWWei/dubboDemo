package com.mydubbo.service.controller;

import com.mydubbo.service.config.bean.TestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private TestConfig testConfig;

    @GetMapping("/config/get")
    public String getConfigTest(){
        return "configStr:"+testConfig.getTestStr();
    }
}
