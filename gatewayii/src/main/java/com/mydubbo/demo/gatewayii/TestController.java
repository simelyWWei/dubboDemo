package com.mydubbo.demo.gatewayii;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientAssignConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfigManager;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.mydubbo.demo.gatewayii.entity.ClusterClientConfigEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@RestController
public class TestController {

    private static final String remoteAddress = "172.16.21.74:8848";

    private static final String NACOS_NAMESPACE_ID = "89c61f85-6223-48a3-9ff5-322e62a5e4cd";

    private static final String cluster_client_groupId = "SENTINEL_GROUP";
    private static final String cluster_client_config_dataid = "cluster-client-config";
    private static final String cluster_assign_client_config_dataid = "cluster-assign-client-config";
    private static final String cluster_flow = "appA-flow-rules";

    private static final String CLUSTER_SERVER_HOST = "localhost";
    private static final int CLUSTER_SERVER_PORT = 29090;
    private static final int REQUEST_TIME_OUT = 200;

    private static final String RESOURCE_NAME = "cluster-resource";

    @PostConstruct
    void init(){
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, remoteAddress);
        properties.put(PropertyKeyConst.NAMESPACE, NACOS_NAMESPACE_ID);
        loadClusterClientConfig();
        registerClusterClientProperty(properties);
        registerClusterFlowRuleProperty(properties);
    }

    /**
     * 加载集群客户端配置
     * 主要是集群服务端的相关连接信息
     */
    private void loadClusterClientConfig(){
        ClusterClientAssignConfig assignConfig = new ClusterClientAssignConfig();
        assignConfig.setServerHost(CLUSTER_SERVER_HOST);
        assignConfig.setServerPort(CLUSTER_SERVER_PORT);
        ClusterClientConfigManager.applyNewAssignConfig(assignConfig);

        ClusterClientConfig clientConfig = new ClusterClientConfig();
        clientConfig.setRequestTimeout(REQUEST_TIME_OUT);
        ClusterClientConfigManager.applyNewConfig(clientConfig);
    }

    /**
     * 为ClusterClientConfig注册一个SentinelProperty
     * 这样的话可以动态的更改这些配置
     * @param properties
     */
    private void registerClusterClientProperty(Properties properties) {

        // 初始化一个配置ClusterClientConfig的 Nacos 数据源
//        ReadableDataSource<String, ClusterClientConfig> clientConfigPropertyDS =
//                new NacosDataSource<>(properties, cluster_client_groupId, cluster_client_config_dataid, source -> {
//                    source -> JSON.parseObject(source, new TypeReference<ClusterClientConfig>() {}));
//                    ClusterClientConfigManager.registerClientConfigProperty(clientConfigDS.getProperty());
//        ClusterClientConfigManager.registerClientConfigProperty(clientConfigPropertyDS.getProperty());
        ReadableDataSource<String, ClusterClientConfig> clientConfigDS = new NacosDataSource<>(properties, cluster_client_groupId, cluster_client_config_dataid,
                source -> JSON.parseObject(source, new TypeReference<ClusterClientConfig>() {}));
        ClusterClientConfigManager.registerClientConfigProperty(clientConfigDS.getProperty());

        ReadableDataSource<String, ClusterClientAssignConfig> clientAssignConfigDS = new NacosDataSource<>(properties, cluster_client_groupId, cluster_assign_client_config_dataid,
                source -> JSON.parseObject(source, new TypeReference<ClusterClientAssignConfig>() {}));
        ClusterClientConfigManager.registerServerAssignProperty(clientAssignConfigDS.getProperty());



//        ReadableDataSource<String, ClusterClientAssignConfig> clientAssignDs =
//                new NacosDataSource<>(properties, cluster_client_groupId, cluster_client_config_dataid, source -> {
//                    List<ClusterClientConfigEntity> groupList = JSON.parseObject(source, new TypeReference<List<ClusterClientConfigEntity>>() {
//                    });
//                    return Optional.ofNullable(groupList)
//                            .flatMap(this::extractClientAssignment)
//                            .orElse(null);
//                });
//        ClusterClientConfigManager.registerServerAssignProperty(clientAssignDs.getProperty());

    }

    /**
     * 注册动态规则Property
     * 当client与Server连接中断，退化为本地限流时需要用到的该规则
     */
    private void registerClusterFlowRuleProperty(Properties properties){
        // 使用 Nacos 数据源作为配置中心，需要在 REMOTE_ADDRESS 上启动一个 Nacos 的服务
        ReadableDataSource<String, List<FlowRule>> flowDs =
                new NacosDataSource<>(properties, cluster_client_groupId, cluster_flow,
                        source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {}));
        // 为集群客户端注册动态规则源
        FlowRuleManager.register2Property(flowDs.getProperty());
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

    @GetMapping("/clusterFlow")
    @ResponseBody
    public String clusterFlow() {
        Entry entry = null;
        String retVal;
        try {
            entry = SphU.entry(RESOURCE_NAME, EntryType.IN, 1);
            retVal = "passed";
        } catch (BlockException e) {
            retVal = "blocked";
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
        return retVal;
    }
}
