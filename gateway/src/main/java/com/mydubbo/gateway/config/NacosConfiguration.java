package com.mydubbo.gateway.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Slf4j
@Configuration
public class NacosConfiguration {
    private static final String remoteAddress = "172.16.21.74:8848";

    private static final String NACOS_NAMESPACE_ID = "89c61f85-6223-48a3-9ff5-322e62a5e4cd";

    @Bean("myNacosConfigService")
    public ConfigService getNacosConfigService(){
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, remoteAddress);
        properties.put(PropertyKeyConst.NAMESPACE, NACOS_NAMESPACE_ID);
        ConfigService configService = null;
        try {
            configService = NacosFactory.createConfigService(properties);
        } catch (NacosException e) {
            e.printStackTrace();
        }
        log.info("加载nacos配置完毕:{}",configService);
        return configService;
    }
}
