package com.mydubbo.demo.gatewayii.entity;

import lombok.Data;

@Data
public class ClusterClientConfigEntity {
    private Integer requestTimeout;
    private String hostIp;
    private Integer hostPort;
}
