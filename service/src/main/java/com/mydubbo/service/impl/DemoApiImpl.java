package com.mydubbo.service.impl;

import com.mydubbo.api.VO.DemoClassVO;
import com.mydubbo.api.api.DemoApi;
import com.mydubbo.service.config.bean.TestConfig;
import com.mydubbo.service.entity.DemoClass;
import com.mydubbo.service.service.DemoClassService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@DubboService(interfaceClass = DemoApi.class,loadbalance = "roundrobin")
public class DemoApiImpl implements DemoApi {

    @Autowired
    private DemoClassService demoClassService;

    @Autowired
    private TestConfig testConfig;

    @Override
    public DemoClassVO showDemo() {
        log.info("实现类中实现showDemo");
        DemoClass demoClass =  demoClassService.getDemoOne();
        DemoClassVO demoClassVo = new DemoClassVO();
        demoClassVo.setParamA(demoClass.getParamA());
        demoClassVo.setTestConfigStr(testConfig.getTestStr());
        return demoClassVo;
    }
}
