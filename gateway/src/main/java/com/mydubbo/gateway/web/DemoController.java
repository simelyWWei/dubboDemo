package com.mydubbo.gateway.web;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientAssignConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfig;
import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.mydubbo.gateway.config.ThreadConfig;
import com.mydubbo.gateway.service.DemoService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

@Slf4j
@RestController
public class DemoController {


    @Autowired
    private DemoService demoService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ThreadConfig threadConfig;

    private ConfigService configService;

    private static final String RESOURCENAME = "showDemo";

    private static final String remoteAddress = "172.16.21.74:8848";

    private static final String NACOS_NAMESPACE_ID = "89c61f85-6223-48a3-9ff5-322e62a5e4cd";

    private static final String groupId = "ruleDemo";
    private static final String dataId = "sentinel";

    private static final String PARAM_A = "1";

    private final ExecutorService pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(1), new NamedThreadFactory("sentinel-nacos-ds-update"),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    @SneakyThrows
    @PostConstruct
    public void initParamRule() {
        // 添加普通规则
        /*log.info("加载普通规则源");
        FlowRule rule = new FlowRule();
        rule.setResource("hello1");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(10);
        List<FlowRule> rules = new ArrayList<>();
        rules.add(rule);
        System.out.println(JSON.toJSONString(rules));
        FlowRuleManager.loadRules(rules);*/

        // 添加参数限流规则
        ParamFlowRule paramRule = new ParamFlowRule(RESOURCENAME);
        // 热点参数的索引，必填，对应 SphU.entry(xxx, args) 中的参数索引位置
        paramRule.setParamIdx(0);
        paramRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        paramRule.setCount(10);
        // 针对 string 类型的参数 PARAM_A，单独设置限流 QPS 阈值为 8，而不是全局的阈值 5.
        ParamFlowItem item = new ParamFlowItem().setObject(PARAM_A)
                .setClassType(String.class.getName())
                .setCount(5);
        paramRule.setParamFlowItemList(Collections.singletonList(item));
        ParamFlowRuleManager.loadRules(Collections.singletonList(paramRule));

        // 配置动态规则源
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, remoteAddress);
        properties.put(PropertyKeyConst.NAMESPACE, NACOS_NAMESPACE_ID);
        configService = NacosFactory.createConfigService(properties);
        log.info("加载规则源");
        // 单节点模式添加规则源
        ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new NacosDataSource<>(properties, groupId, dataId,
                source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {
                }));
        log.info("flowRuleDataSource:{}", flowRuleDataSource.readSource());
        FlowRuleManager.register2Property(flowRuleDataSource.getProperty());

        /*
        ReadableDataSource<String, List<ParamFlowRule>> paramFlowRuleDataSource = new NacosDataSource<>(properties, param1_groupId, param1_dataId,
                source -> JSON.parseObject(source, new TypeReference<List<ParamFlowRule>>() {
                }));

        ParamFlowRuleManager.register2Property(paramFlowRuleDataSource.getProperty());*/

    }

    /**
     * 简单目标接口
     */
    @GetMapping("/hello")
    public String test() {
        return demoService.hello();
    }

    /**
     * hello并发20测试
     * @return
     */
    @GetMapping("/testConcurrency1")
    public String testConcurrency1() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        for (int i = 0; i < 20; i++) {
            TestThread2 myRunnable = new TestThread2(countDownLatch, i);
            threadConfig.taskExecutor().execute(myRunnable);
        }
        countDownLatch.countDown();
        return "success";
    }

    /**
     *
     * 埋点方式限流测试接口
     */
    @GetMapping("/hello1")
    public String test1() {
        String result = null;
        try (Entry entry = SphU.entry("hello1")) {
            result = demoService.hello2();
        } catch (BlockException ex) {
            System.out.println("blocked");
        }
        return result;
    }

    /**
     * hello1并发20测试
     * @return
     */
    @GetMapping("/testConcurrency2")
    public String testConcurrency2() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        for (int i = 0; i < 20; i++) {
            TestThread3 myRunnable = new TestThread3(countDownLatch, i);
            threadConfig.taskExecutor().execute(myRunnable);
        }
        countDownLatch.countDown();
        return "success";
    }

    /**
     * 测试参数限流接口
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/showDemo")
    public String showDemoClass(@RequestParam("id") String id, HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        String result = "";
        Entry entry = null;
        try {
            entry = SphU.entry(RESOURCENAME, EntryType.IN, 1, id);
            result = id;
        } catch (BlockException e) {
            System.out.println("超过qps，被阻塞，传入id:" + id);
            result = "block";
        } finally {
            if (entry != null) {
                entry.exit(1,url+id);
            }
        }
        return result;
    }

    /**
     * 热点参数热点限流测试
     * @return
     */
    @GetMapping("/testConcurrency")
    public String testConcurrency() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        for (int i = 0; i < 30; i++) {
            TestThread myRunnable = new TestThread(countDownLatch, i);
            threadConfig.taskExecutor().execute(myRunnable);
        }
        countDownLatch.countDown();
        return "success";
    }

    @SneakyThrows
    @GetMapping("/modifyProperty")
    public Boolean modifyProperty(@RequestParam("qps") double qps) {
        FlowRule rule = new FlowRule();
        rule.setResource("hello1");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(qps);
        List<FlowRule> rules = new ArrayList<>();
        rules.add(rule);
        String content = JSON.toJSONString(rules);
//        String content = "[ {\"clusterMode\":false, \"controlBehavior\":0, \"count\":5, \"grade\":1, \"limitApp\":\"default\", \"maxQueueingTimeMs\":500, \"resource\":\"hello\", \"strategy\":0, \"warmUpPeriodSec\":10 } ]";
//        ConfigService configService = NacosFactory.createConfigService(remoteAddress);
        return configService.publishConfig(dataId, groupId, content);
    }

    @GetMapping("/getConfig")
    public String getNacosConfigDemo() {
        String config = null;
        try {
            config = configService.getConfig(dataId, groupId, 5000);
        } catch (NacosException e) {
            e.printStackTrace();
        }
        return config;
    }

    /**
     * 风控测试
     * @return
     */
    @GetMapping("/testConcurrency4")
    public String testConcurrency4() {
        // Q1aFl52TUuYC7e1:userInnetmd5
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        for (int i = 0; i < 10; i++) {
            TestThread4 myRunnable = new TestThread4(countDownLatch, i);
            threadConfig.taskExecutor().execute(myRunnable);
        }
        countDownLatch.countDown();
        return "success";
    }

    @GetMapping("/testConcurrency3")
    public String testConcurrency3() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        for (int i = 0; i < 20; i++) {
            TestThread3 myRunnable = new TestThread3(countDownLatch, i);
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
                System.out.println("线程-" + threadid + "," + "传入id：" + threadid % 2 + "," + "返回：" + forObject);
//                log.info("thread-{}:result:{}", threadid, forObject);
            }
        }
    }

    public class TestThread2 implements Runnable {

        private final CountDownLatch startSignal2;
        private int threadid;

        public TestThread2(CountDownLatch startSignal, Integer threadid) {
            super();
            this.startSignal2 = startSignal;
            this.threadid = threadid;
        }

        @Override
        public void run() {
            try {
                startSignal2.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //实际测试操作
            doWork2();
        }

        private void doWork2() {
            String forObject = null;
            try {
                forObject = restTemplate.getForObject("http://127.0.0.1:9080/hello", String.class);
            } catch (Exception e) {
                log.error("thread-{}线程被阻塞", threadid);
            }
            if (forObject != null) {
                log.info("thread-{}:result:{}", threadid, forObject);
            }
        }
    }

    public class TestThread3 implements Runnable {

        private final CountDownLatch startSignal3;
        private int threadid;

        public TestThread3(CountDownLatch startSignal, Integer threadid) {
            super();
            this.startSignal3 = startSignal;
            this.threadid = threadid;
        }

        @Override
        public void run() {
            try {
                startSignal3.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //实际测试操作
            doWork3();
        }

        private void doWork3() {
            String forObject = null;
            String uri = "http://127.0.0.1:9080/hello1";
            try {
                forObject = restTemplate.getForObject(uri, String.class);
            } catch (Exception e) {
                log.error("thread-{}线程被阻塞", threadid);
            }
            if (forObject != null) {
                log.info("thread-{}:result:{}", threadid, forObject);
            }
        }
    }

    public class TestThread4 implements Runnable {

        private final CountDownLatch startSignal4;
        private int threadid;

        public TestThread4(CountDownLatch startSignal, Integer threadid) {
            super();
            this.startSignal4 = startSignal;
            this.threadid = threadid;
        }

        @Override
        public void run() {
            try {
                startSignal4.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //实际测试操作
            doWork4();
        }

        private void doWork4() {
            String forObject = null;
            try {
                MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
                requestBody.add("keyParamName","sendTelNo");
                requestBody.add("intfcUriLast","CustomsLocQuery");
                requestBody.add("paramValueJson","{\"sendTelNo\":\"18600197436\"}");
                HttpHeaders requestHeaders = new HttpHeaders();
                requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                requestHeaders.set("X-Forwarded-For","127.0.0.1");

                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(requestBody, requestHeaders);
                ResponseEntity<String> responseEntity = null;
                try {
                    responseEntity = restTemplate.postForEntity("http://10.244.11.117:9090/test/basicIntfc", requestEntity, String.class);
                } catch (RestClientException e) {
                    e.printStackTrace();
                }
                String body = responseEntity.getBody();
                System.out.println("thread-"+threadid+"调用结果"+body);
            } catch (Exception e) {
                log.error("thread-{}调用异常", threadid, e);
            }
        }
    }


    public static void main(String[] args) throws Exception {
        /*final String remoteAddress = "172.16.21.74:8848";
        final String groupId = "ruleDemo";
        final String dataId = "sentinel";
        final String rule = "[\n" +
                "  {\"clusterMode\":false,\n" +
                "  \"controlBehavior\":0,\n" +
                "  \"count\":10,\n" +
                "  \"grade\":1,\n" +
                "  \"limitApp\":\"default\",\n" +
                "  \"maxQueueingTimeMs\":500,\n" +
                "  \"resource\":\"hello\",\n" +
                "  \"strategy\":0,\n" +
                "  \"warmUpPeriodSec\":10\n" +
                "  }\n" +
                "]";
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, remoteAddress);
        properties.put(PropertyKeyConst.NAMESPACE, NACOS_NAMESPACE_ID);
        ConfigService configService = NacosFactory.createConfigService(properties);
        System.out.println(configService.publishConfig(dataId, groupId, rule));*/
        ClusterClientConfig clusterClientConfig = new ClusterClientConfig();
        clusterClientConfig.setRequestTimeout(0);
        System.out.println(JSON.toJSONString(clusterClientConfig));

        ClusterClientAssignConfig clusterClientAssignConfig = new ClusterClientAssignConfig();
        clusterClientAssignConfig.setServerHost("127.0.0.1");
        clusterClientAssignConfig.setServerPort(11111);
        System.out.println(JSON.toJSONString(clusterClientAssignConfig));


    }

}
