package com.mydubbo.gateway.web;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.mydubbo.api.VO.DemoClassVO;
import com.mydubbo.gateway.config.ThreadConfig;
import com.mydubbo.gateway.service.DemoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

@Slf4j
@RestController
public class DemoController {


    @Autowired
    private DemoService demoService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ThreadConfig threadConfig;

    private static final String RESOURCENAME = "hello";

    @PostConstruct
    public void initParamRule() {
        ParamFlowRule rule = new ParamFlowRule(RESOURCENAME);
        rule.setParamIdx(0);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(5);
        ParamFlowRuleManager.loadRules(Collections.singletonList(rule));
    }

    @GetMapping("/showDemo")
    public String showDemoClass(@RequestParam("id") String id, HttpServletRequest request) {
        String url = request.getRequestURL().toString();
//        log.info("url+id:{}", url+id);
        String result = "";
        Entry entry = null;
        try {
            entry = SphU.entry(RESOURCENAME, EntryType.IN, 1, url+id);
            result = demoService.showDemoClass().toString();
        } catch (BlockException e) {
            e.printStackTrace();
            result = "block";
        }finally {
            if (entry != null){
                entry.exit(1,url+id);
            }
        }
        return result;
    }

    @GetMapping("/testConcurrency")
    public String testConcurrency() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        for (int i = 0; i < 20; i++) {
            TestThread myRunnable = new TestThread(countDownLatch, i);
            threadConfig.taskExecutor().execute(myRunnable);
        }
        countDownLatch.countDown();
        return "success";
    }

    public class TestThread implements Runnable {

        private final CountDownLatch startSignal;
        private int threadid;

        public TestThread(CountDownLatch startSignal, Integer threadid) {
            super();
            this.startSignal = startSignal;
            this.threadid = threadid;
        }

        @Override
        public void run() {
            try {
                startSignal.await();
//                log.info("thread-{},准备",threadid);
                //一直阻塞当前线程，直到计时器的值为0
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //实际测试操作
            doWork();
        }

        private void doWork() {
//            log.info("thread-{},开始执行", threadid);
            String forObject = null;
            try {
                forObject = restTemplate.getForObject("http://127.0.0.1:9080/showDemo?id=" + this.threadid % 2, String.class);
            } catch (Exception e) {
                log.info("thread-{}线程被阻塞", threadid);
            }
            if (forObject != null) {
                log.info("thread-{}:result:{}", threadid, forObject);
            }
        }
    }
}
