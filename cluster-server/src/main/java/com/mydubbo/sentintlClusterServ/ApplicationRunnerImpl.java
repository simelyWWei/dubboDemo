package com.mydubbo.sentintlClusterServ;

import com.alibaba.csp.sentinel.cluster.server.ClusterTokenServer;
import com.alibaba.csp.sentinel.cluster.server.SentinelDefaultTokenServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


/**
 * 项目启动测试
 *
 * @author weiwei
 * @since 2020-04-27 16:00
 **/
@Slf4j
@Component
public class ApplicationRunnerImpl implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("启动tokenServer");
        // 创建一个 ClusterTokenServer 的实例，独立模式
        ClusterTokenServer tokenServer = new SentinelDefaultTokenServer();
        // 启动
        tokenServer.start();
    }

}
