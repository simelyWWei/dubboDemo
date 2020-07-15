package com.mydubbo.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 *
 * @author weiwei
 * @since 2020-04-16 13:52
 **/

@Configuration
public class ThreadConfig {
    /**
     * 线程池,供异步线程使用
     */
    @Bean("selfTaskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 设置核心线程数
        executor.setCorePoolSize(4);
        // 设置最大线程数
        executor.setMaxPoolSize(8);
        // 设置队列容量
        executor.setQueueCapacity(2000);
        // 设置线程活跃时间（秒）
        executor.setKeepAliveSeconds(60*60);
        // 设置默认线程名称
        executor.setThreadNamePrefix("MALL_SERV");
        // 设置拒绝策略 可以自定义实现类写入数据库，进行保存
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }
}
