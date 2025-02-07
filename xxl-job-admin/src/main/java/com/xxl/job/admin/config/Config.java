package com.xxl.job.admin.config;

import cn.kdyzm.util.spring.SpringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;


/**
 * @author kdyzm
 * @date 2021/9/16
 */
@Configuration
@Import({SpringUtils.class})
public class Config {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
