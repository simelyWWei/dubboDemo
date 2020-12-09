package com.mydubbo.service.config;

import com.alibaba.nacos.spring.context.annotation.config.EnableNacosConfig;
import com.mydubbo.service.config.bean.TestConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableNacosConfig
public class BusConfigCenterConfig {

    public static final String TEST_CONFIG = "test_config";

    @Bean
    public TestConfig testConfig() {
        return new TestConfig();
    }

}
