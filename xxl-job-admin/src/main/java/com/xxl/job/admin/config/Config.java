package com.xxl.job.admin.config;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * @author kdyzm
 * @date 2021/9/16
 */
@Configuration
public class Config {

    /**
     * 线程池
     */
    /*@Bean
    public ExecutorService threadPoolExecutor() {
        return TtlExecutors.getTtlExecutorService(new ThreadPoolExecutor(
                8,
                12,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        ));
    }*/
}
