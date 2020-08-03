package com.mydubbo.gateway.web;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientAssignConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfigManager;
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
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.mydubbo.api.VO.DemoClassVO;
import com.mydubbo.gateway.config.ThreadConfig;
import com.mydubbo.gateway.entity.ClusterClientConfigEntity;
import com.mydubbo.gateway.service.DemoService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
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

    @Autowired
    private ConfigService myNacosConfigService;

    private static final String RESOURCENAME = "showDemo";

    private static final String remoteAddress = "172.16.21.74:8848";

    private static final String NACOS_NAMESPACE_ID = "89c61f85-6223-48a3-9ff5-322e62a5e4cd";

    private static final String groupId = "ruleDemo";
    private static final String dataId = "sentinel";

    private static final String groupId2 = "ruleDemo";
    private static final String dataId2 = "rule2";

    private static final String cluster_client_groupId = "SENTINEL_GROUP";
    private static final String cluster_client_config_dataid = "cluster-client-config";
    private static final String cluster_flow = "appA-flow-rules";


    private static final String param1_groupId = "sentinelRule";

    private static final String param1_dataId = "ParamRule1";

    private static final String PARAM_A = "1";

    private final ExecutorService pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(1), new NamedThreadFactory("sentinel-nacos-ds-update"),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    @SneakyThrows
    @PostConstruct
    public void initParamRule() {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, remoteAddress);
        properties.put(PropertyKeyConst.NAMESPACE, NACOS_NAMESPACE_ID);
        log.info("加载规则源");
        /*// 单节点模式添加规则源
        ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new NacosDataSource<>(properties, groupId2, dataId2,
                source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {
                }));
        log.info("flowRuleDataSource:{}", flowRuleDataSource.readSource());
        FlowRuleManager.register2Property(flowRuleDataSource.getProperty());

        ReadableDataSource<String, List<ParamFlowRule>> paramFlowRuleDataSource = new NacosDataSource<>(properties, param1_groupId, param1_dataId,
                source -> JSON.parseObject(source, new TypeReference<List<ParamFlowRule>>() {
                }));

        ParamFlowRuleManager.register2Property(paramFlowRuleDataSource.getProperty());*/

        // 集群模式添加规则源和客户端配置
        // 将当前模式置为客户端模式
        ClusterStateManager.applyState(ClusterStateManager.CLUSTER_CLIENT);
        // 初始化一个配置ClusterClientConfig的 Nacos 数据源
        ReadableDataSource<String, ClusterClientConfig> clientConfigPropertyDS =
                new NacosDataSource<>(properties, cluster_client_groupId, cluster_client_config_dataid, source -> {
                    List<ClusterClientConfigEntity> groupList = JSON.parseObject(source, new TypeReference<List<ClusterClientConfigEntity>>() {
                    });
                    return Optional.ofNullable(groupList)
                            .flatMap(this::extractClientConfig)
                            .orElse(null);
                });

        ClusterClientConfigManager.registerClientConfigProperty(clientConfigPropertyDS.getProperty());

        ReadableDataSource<String, ClusterClientAssignConfig> clientAssignDs =
                new NacosDataSource<>(properties, cluster_client_groupId, cluster_client_config_dataid, source -> {
                    List<ClusterClientConfigEntity> groupList = JSON.parseObject(source, new TypeReference<List<ClusterClientConfigEntity>>() {
                    });
                    return Optional.ofNullable(groupList)
                            .flatMap(this::extractClientAssignment)
                            .orElse(null);
                });
        ClusterClientConfigManager.registerServerAssignProperty(clientAssignDs.getProperty());

        ReadableDataSource<String, List<FlowRule>> flowDs =
                new NacosDataSource<>(properties, cluster_client_groupId, cluster_flow,
                        source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {}));
        // 为集群客户端注册动态规则源
        FlowRuleManager.register2Property(flowDs.getProperty());

       /* // 添加普通规则
        FlowRule rule = new FlowRule();
        rule.setResource("hello");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(10);
        List<FlowRule> rules = new ArrayList<>();
        rules.add(rule);
        System.out.println(JSON.toJSONString(rules));
        FlowRuleManager.loadRules(rules);*/

        /*
        // 添加参数限流规则
        ParamFlowRule rule = new ParamFlowRule(RESOURCENAME);
        rule.setParamIdx(0);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(5);
        // 针对 string 类型的参数 PARAM_A，单独设置限流 QPS 阈值为 8，而不是全局的阈值 5.
        ParamFlowItem item = new ParamFlowItem().setObject(PARAM_A)
                .setClassType(String.class.getName())
                .setCount(8);
        rule.setParamFlowItemList(Collections.singletonList(item));
        System.out.println(JSON.toJSONString(rule));
        ParamFlowRuleManager.loadRules(Collections.singletonList(rule));*/
        // 添加一个监听器
        /*myNacosConfigService.addListener(dataId, groupId, new Listener() {
            @Override
            public Executor getExecutor() {
                return pool;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                log.info("》》》》监听到update");
                log.info("configInfo:{}",configInfo);
            }
        });*/
    }

    private Optional<ClusterClientConfig> extractClientConfig(List<ClusterClientConfigEntity> clusterClientConfigEntities) {
        for (ClusterClientConfigEntity group : clusterClientConfigEntities) {
//            if (group.getClientSet().contains(getCurrentMachineId())) {
            Integer requestTimeout = group.getRequestTimeout();
            return Optional.of(new ClusterClientConfig().setRequestTimeout(requestTimeout));
//            }
        }
        return Optional.empty();
    }

    private Optional<ClusterClientAssignConfig> extractClientAssignment(List<ClusterClientConfigEntity> groupList) {
        // 过滤白名单
       /* if (groupList.stream().anyMatch(this::machineEqual)) {
            return Optional.empty();
        }*/
        // Build client assign config from the client set of target server group.
        for (ClusterClientConfigEntity group : groupList) {
//            if (group.getClientSet().contains(getCurrentMachineId())) {
            String ip = group.getHostIp();
            Integer port = group.getHostPort();
            return Optional.of(new ClusterClientAssignConfig(ip, port));
//            }
        }
        return Optional.empty();
    }

    /**
     * 目标接口
     */
    @GetMapping("/hello")
    public String test1() {
        String result = null;
        try (Entry entry = SphU.entry("hello")) {
            result = demoService.hello();
        } catch (BlockException ex) {
            System.out.println("blocked");
        }
        return result;
    }

    /**
     * 目标接口
     */
    @GetMapping("/hello2")
    public String test2() {
        String result = null;
        try (Entry entry = SphU.entry("cluster-resource")) {
            result = demoService.hello();
        } catch (BlockException ex) {
            System.out.println("blocked");
        }
        return result;
    }

    @SneakyThrows
    @GetMapping("/modifyProperty")
    public Boolean modifyProperty(@RequestParam("qps") double qps) {
        FlowRule rule = new FlowRule();
        rule.setResource("hello");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(qps);
        List<FlowRule> rules = new ArrayList<>();
        rules.add(rule);
        String content = JSON.toJSONString(rules);
//        String content = "[ {\"clusterMode\":false, \"controlBehavior\":0, \"count\":5, \"grade\":1, \"limitApp\":\"default\", \"maxQueueingTimeMs\":500, \"resource\":\"hello\", \"strategy\":0, \"warmUpPeriodSec\":10 } ]";
//        ConfigService configService = NacosFactory.createConfigService(remoteAddress);
        return myNacosConfigService.publishConfig(dataId, groupId, content);
    }

    @GetMapping("/getConfig")
    public String getNacosConfigDemo() {
        String config = null;
        try {
            config = myNacosConfigService.getConfig(dataId2, groupId, 5000);
        } catch (NacosException e) {
            e.printStackTrace();
        }
        return config;
    }

    @GetMapping("/showDemo")
    public String showDemoClass(@RequestParam("id") String id, HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        String result = "";
        Entry entry = null;
        try {
            entry = SphU.entry(RESOURCENAME, EntryType.IN, 1, id);
            result = demoService.showDemoClass().toString();
        } catch (BlockException e) {
            System.out.println("超过qps，被阻塞，传入id:" + id);
            result = "block";
        } finally {
            if (entry != null) {
                entry.exit(1, url + id);
            }
        }
        return result;
    }

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
                log.error("thread-{}线程被阻塞", threadid, e);
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
            try {
                forObject = restTemplate.getForObject("http://127.0.0.1:9080/hello2", String.class);
            } catch (Exception e) {
                log.error("thread-{}线程被阻塞", threadid, e);
            }
            if (forObject != null) {
                log.info("thread-{}:result:{}", threadid, forObject);
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
