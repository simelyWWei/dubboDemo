package com.mydubbo.service.config.bean;

import com.alibaba.nacos.api.config.annotation.NacosConfigurationProperties;
import com.mydubbo.service.config.BusConfigCenterConfig;
import lombok.Data;

@Data
@NacosConfigurationProperties(dataId = BusConfigCenterConfig.TEST_CONFIG, autoRefreshed = true)
public class TestConfig {

    private String testStr;

}
