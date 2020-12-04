package com.mydubbo.sentintlClusterServ;

import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.server.ClusterTokenServer;
import com.alibaba.csp.sentinel.cluster.server.SentinelDefaultTokenServer;
import com.alibaba.csp.sentinel.cluster.server.config.ClusterServerConfigManager;
import com.alibaba.csp.sentinel.cluster.server.config.ServerTransportConfig;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.api.PropertyKeyConst;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

@Slf4j
public class ServerRunner {
    private static final String REMOTE_ADDRESS = "172.16.21.74:8848";
    private static final String NACOS_NAMESPACE_ID = "89c61f85-6223-48a3-9ff5-322e62a5e4cd";
    private static final String GROUP_ID = "SENTINEL_GROUP";
    private static final String CLUSTER_NAMESPACE_DATA_ID = "cluster-server-namespace-set";
    private static final String CLUSTER_TRANSPORT_DATA_ID = "cluster-server-transport-config";
    private static final String FLOW_POSTFIX = "-flow-rules";

    private static final int CLUSTER_SERVER_PORT = 29090;
    private static final String APP_NAME = "appA";


    public static void main(String[] args) {
        ServerRunner serverRunner = new ServerRunner();
        serverRunner.serverInit();
        serverRunner.ServerStart();
    }

    private void serverInit(){
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, REMOTE_ADDRESS);
        properties.put(PropertyKeyConst.NAMESPACE, NACOS_NAMESPACE_ID);

        // 为 namespace 注册一个 SentinelProperty
        // 初始化一个配置 namespace 的 Nacos 数据源
//        log.info("加载namespace");
//        ReadableDataSource<String, Set<String>> namespaceDs =
//                new NacosDataSource<>(properties, GROUP_ID,
//                        CLUSTER_NAMESPACE_DATA_ID, source -> JSON.parseObject(source, new TypeReference<Set<String>>() {}));
//        ClusterServerConfigManager.registerNamespaceSetProperty(namespaceDs.getProperty());

        // 为 ServerTransportConfig 注册一个 SentinelProperty
        // 初始化一个配置服务端通道配置的 Nacos 数据源
//        ReadableDataSource<String, ServerTransportConfig> transportConfigDs =
//                new NacosDataSource<>(properties,
//                        GROUP_ID, CLUSTER_TRANSPORT_DATA_ID,
//                        source -> JSON.parseObject(source, new TypeReference<ServerTransportConfig>() {}));
//        log.info("加载ServerTransportConfig");
//        ClusterServerConfigManager.registerServerTransportProperty(transportConfigDs.getProperty());

        // 注册动态规则源
        /**
         * token server 抽象出了命名空间（namespace）的概念，可以支持多个应用/服务，
         * 因此我们需要通过 ClusterFlowRuleManager 注册一个可以自动根据 namespace 创建动态规则源的生成器，即 Supplier。
         *
         * Supplier 会根据 namespace 生成类型为 SentinelProperty<List<FlowRule>> 的动态规则源，
         * 不同的 namespace 对应着不同的规则源，若不指定 namespace ，则默认为为应用名：${project.name} 的值
         *
         * 当集群限流服务端 namespace set 产生变更时，Sentinel 会自动针对新加入的 namespace 生成动态规则源并进行自动监听，并删除旧的不需要的规则源。
         */
        // 为集群流控注册一个Supplier，该Supplier会根据namespace动态创建数据源
        log.info("加载Supplier");
        ClusterFlowRuleManager.setPropertySupplier(namespace -> {
            ReadableDataSource<String, List<FlowRule>> ds =
                    new NacosDataSource<>(properties,GROUP_ID,namespace+FLOW_POSTFIX, source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {}));
            return ds.getProperty();
        });
    }

    @SneakyThrows
    private void ServerStart(){
        ClusterTokenServer tokenServer = new SentinelDefaultTokenServer();

        ClusterServerConfigManager.loadServerNamespaceSet(Collections.singleton(APP_NAME));
        ClusterServerConfigManager.loadGlobalTransportConfig(new ServerTransportConfig()
                .setIdleSeconds(600)
                .setPort(CLUSTER_SERVER_PORT));
        // 启动
        tokenServer.start();
    }
}
