package com.mydubbo.service.config;

import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientAssignConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfigManager;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.mydubbo.service.entity.ClusterClientConfigEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Configuration
public class SentinelClientConfig {

    private static final String remoteAddress = "172.16.21.74:8848";
    private static final String NACOS_NAMESPACE_ID = "89c61f85-6223-48a3-9ff5-322e62a5e4cd";

    private static final String cluster_client_groupId = "SENTINEL_GROUP";
    private static final String cluster_client_config_dataid = "cluster-client-config";
    private static final String cluster_flow = "appA-flow-rules";

    @Bean
    public void initSentinelConfig(){

        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, remoteAddress);
        properties.put(PropertyKeyConst.NAMESPACE, NACOS_NAMESPACE_ID);
        // 集群模式添加规则源和客户端配置
        // 将当前模式置为客户端模式
        ClusterStateManager.applyState(ClusterStateManager.CLUSTER_CLIENT);
        // 初始化一个配置ClusterClientConfig的 Nacos 数据源
        ReadableDataSource<String, ClusterClientConfig> clientConfigPropertyDS =
                new NacosDataSource<>(properties, cluster_client_groupId, cluster_client_config_dataid, source -> {
                    List<ClusterClientConfigEntity> groupList = JSON.parseObject(source, new TypeReference<List<ClusterClientConfigEntity>>() {
                    });
                    //[{
                    //    "requestTimeout":30000,
                    //    "hostIp":"127.0.0.1",
                    //    "hostPort":11111
                    //}]
                    return Optional.ofNullable(groupList)
                            .flatMap(this::extractClientConfig)
                            .orElse(null);
                });

        ClusterClientConfigManager.registerClientConfigProperty(clientConfigPropertyDS.getProperty());

        ReadableDataSource<String, ClusterClientAssignConfig> clientAssignDs =
                new NacosDataSource<>(properties, cluster_client_groupId, cluster_client_config_dataid, source -> {
                    List<ClusterClientConfigEntity> groupList = JSON.parseObject(source, new TypeReference<List<ClusterClientConfigEntity>>() {
                    });
                    //[{
                    //    "requestTimeout":30000,
                    //    "hostIp":"127.0.0.1",
                    //    "hostPort":11111
                    //}]
                    return Optional.ofNullable(groupList)
                            .flatMap(this::extractClientAssignment)
                            .orElse(null);
                });
        ClusterClientConfigManager.registerServerAssignProperty(clientAssignDs.getProperty());

        //[
        //    {
        //        "resource" : "cluster-resource",      // 限流的资源名称
        //        "grade" : 1,                          // 限流模式为：qps
        //        "count" : 10,                         // 阈值为：10
        //        "clusterMode" :  true,                // 集群模式为：true
        //        "clusterConfig" : {
        //            "flowId" : 111,                   // 全局唯一id
        //            "thresholdType" : 1,              // 阈值模式伪：全局阈值
        //            "fallbackToLocalWhenFail" : true  // 在 client 连接失败或通信失败时，是否退化到本地的限流模式
        //        }
        //    }
        //]
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
            return Optional.of(new com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfig().setRequestTimeout(requestTimeout));
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
}
