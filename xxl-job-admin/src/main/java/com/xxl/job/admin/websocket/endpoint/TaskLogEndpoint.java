package com.xxl.job.admin.websocket.endpoint;

import com.xxl.job.core.biz.websocket.EndpointType;
import com.xxl.job.core.biz.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.server.ServerEndpoint;

/**
 * 通过@ServerEndpoint暴露出去的 ws 应用的路径，有点类似我们经常用的@RequestMapping。
 * 比如你的启动端口是8080，而这个注解的值是ws，那我们就可以通过 ws://127.0.0.1:8080/ws 来连接你的应用
 *
 * @author xull
 */
@Slf4j
@Component
@ServerEndpoint("/taskLog/{receiverId}")
public class TaskLogEndpoint extends WebSocketServer {

    public TaskLogEndpoint() {
        log.info("注册taskLog websocket endpoint");
    }

    @Override
    public EndpointType getEndpointType() {
        return EndpointType.TASK_LOG;
    }

    @Override
    public void onMessage(String message) {
        System.out.println("钩子方法执行：" + message);
    }

}
