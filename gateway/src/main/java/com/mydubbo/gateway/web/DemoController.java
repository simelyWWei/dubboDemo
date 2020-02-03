package com.mydubbo.gateway.web;

import com.mydubbo.api.VO.DemoClassVO;
import com.mydubbo.gateway.service.DemoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class DemoController {

    @Autowired
    private DemoService demoService;

    @GetMapping("/showDemo")
    public DemoClassVO showDemoClass(){
        return demoService.showDemoClass();
    }
}
