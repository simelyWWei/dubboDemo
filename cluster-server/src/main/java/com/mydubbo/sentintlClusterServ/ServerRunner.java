package com.mydubbo.sentintlClusterServ;

import com.alibaba.csp.sentinel.cluster.server.ClusterTokenServer;
import com.alibaba.csp.sentinel.cluster.server.SentinelDefaultTokenServer;
import com.alibaba.csp.sentinel.cluster.server.config.ClusterServerConfigManager;
import com.alibaba.csp.sentinel.cluster.server.config.ServerTransportConfig;
import lombok.SneakyThrows;

import java.util.Collections;

public class ServerRunner {
    private static final int CLUSTER_SERVER_PORT = 29090;
    private static final String APP_NAME = "appA";

    @SneakyThrows
    public static void main(String[] args) {
        ClusterTokenServer tokenServer = new SentinelDefaultTokenServer();

        ClusterServerConfigManager.loadGlobalTransportConfig(new ServerTransportConfig()
                .setIdleSeconds(600)
                .setPort(CLUSTER_SERVER_PORT));
        ClusterServerConfigManager.loadServerNamespaceSet(Collections.singleton(APP_NAME));
        // 启动
        tokenServer.start();
    }
}
