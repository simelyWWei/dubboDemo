package com.mydubbo.service.impl;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.mydubbo.api.VO.DemoClassVO;
import com.mydubbo.api.api.DemoApi;
import com.mydubbo.service.entity.DemoClass;
import com.mydubbo.service.service.DemoClassService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@DubboService(interfaceClass = DemoApi.class,loadbalance = "roundrobin")
public class DemoApiImpl implements DemoApi {

    @Autowired
    private DemoClassService demoClassService;

    @Override
    public DemoClassVO showDemo() {
        DemoClass demoClass =  demoClassService.getDemoOne();
        DemoClassVO demoClassVo = new DemoClassVO();
        demoClassVo.setParamA(demoClass.getParamA());
        return demoClassVo;
    }

    @Override
    public String hello() {
        return "hello!";
    }

    @Override
    public String hello2() {
        log.info("come to impl.hello2");
        String result;
        try (Entry entry = SphU.entry("cluster-resource", EntryType.IN,1)) {
            result = "hello2";
        } catch (BlockException ex) {
            System.out.println("blocked");
            result = "blocked!";
        }
        return result;
    }
}
