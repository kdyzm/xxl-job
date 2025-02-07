package com.xxl.job.admin.websocket.endpoint;

import cn.kdyzm.json.util.JsonUtils;
import cn.kdyzm.util.spring.SpringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.websocket.EndpointType;
import com.xxl.job.core.biz.websocket.WebSocketServer;
import com.xxl.job.core.biz.websocket.model.ExecuteWrapperResult;
import com.xxl.job.core.util.GsonTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.server.ServerEndpoint;
import java.util.List;

/**
 * 通过@ServerEndpoint暴露出去的 ws 应用的路径，有点类似我们经常用的@RequestMapping。
 * 比如你的启动端口是8080，而这个注解的值是ws，那我们就可以通过 ws://127.0.0.1:8080/ws 来连接你的应用
 *
 * @author xull
 */
@Slf4j
@Component
@ServerEndpoint("/taskExecute/{receiverId}/{clientIp}")
public class TaskExecuteEndpoint extends WebSocketServer {
    
    public TaskExecuteEndpoint() {
        log.info("注册taskExecute websocket endpoint");
    }

    @Override
    public EndpointType getEndpointType() {
        return EndpointType.TASK_EXECUTE;
    }

    @Override
    public void onMessage(String message) {
        log.info("receive message={}", message);
        ExecuteWrapperResult<JsonNode> req = JsonUtils.read(
                message, 
                new TypeReference<ExecuteWrapperResult<JsonNode>>() {
        });
        String path = req.getPath();
        switch (path) {
            case "callback":
                List<HandleCallbackParam> callbackParamList = GsonTool.fromJson(
                        req.getData().toString(),
                        List.class,
                        HandleCallbackParam.class
                );
                AdminBiz adminBiz = SpringUtils.getBean(AdminBiz.class);
                adminBiz.callback(callbackParamList);
                break;
            default:
                log.info("无法识别的path：{}", path);
                break;
        }
    }

}
