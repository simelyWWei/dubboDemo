package com.mydubbo.service.config.bean;

import com.alibaba.nacos.api.config.ConfigType;
import com.alibaba.nacos.api.config.annotation.NacosConfigurationProperties;
import com.mydubbo.service.config.ConfigCenterConfig;
import lombok.Data;

@Data
@NacosConfigurationProperties(dataId = ConfigCenterConfig.TEST_CONFIG, autoRefreshed = true)
public class TestConfig {

    private String testStr;

}
