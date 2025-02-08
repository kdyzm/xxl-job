package com.xxl.job.core.biz.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.kdyzm.json.util.JsonUtils;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.*;
import com.xxl.job.core.biz.websocket.model.ExecuteWrapperResult;
import com.xxl.job.core.queue.MessageQueueManager;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author kdyzm
 * @date 2025/2/6
 */
@Slf4j
public class WebSocketExecutorBizClient implements ExecutorBiz {

    @Override
    public ReturnT<String> beat(WebSocketServer webSocketServer) {
        return this.sendMessage("beat", webSocketServer, new Object());
    }

    @Override
    public ReturnT<WebSocketServer> idleBeat(WebSocketServer webSocketServer, IdleBeatParam idleBeatParam) {
        try {
            webSocketServer.send(new ExecuteWrapperResult<>("idleBeat", idleBeatParam));
        } catch (IOException e) {
            log.error("执行任务失败，任务详情：{}", JsonUtils.toPrettyString(idleBeatParam), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "执行任务失败");
        }
        return new ReturnT<>(ReturnT.SUCCESS_CODE, "websocket触发任务执行成功");
    }

    @Override
    public ReturnT<String> run(WebSocketServer webSocketServer, TriggerParam triggerParam) {
        this.sendMessage("run", webSocketServer, triggerParam);
        //开始监听消息队列
        MessageQueueManager instance = MessageQueueManager.getInstance();
        Object callBackResult;
        while (Objects.isNull(callBackResult = instance.take("triggerRunCallback", triggerParam.getJobId()))) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                log.error("", e);
            }
        }
        ReturnT<Integer> returnResult = (ReturnT<Integer>) callBackResult;
        ReturnT<String> returnReulst1 = new ReturnT<>(
                returnResult.getCode(),
                returnResult.getMsg()
        );
        return returnReulst1;
    }

    @Override
    public ReturnT<String> kill(WebSocketServer webSocketServer, KillParam killParam) {
        return this.sendMessage("kill", webSocketServer, killParam);
    }

    @Override
    public ReturnT<LogResult> log(WebSocketServer webSocketServer, LogParam logParam) {
        try {
            webSocketServer.send(new ExecuteWrapperResult<>("log", logParam));
        } catch (IOException e) {
            log.error("执行任务失败，任务详情：{}", JsonUtils.toPrettyString(logParam), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "执行任务失败");
        }
        return new ReturnT<>(ReturnT.SUCCESS_CODE, "websocket触发任务执行成功");
    }

    public <T> ReturnT<String> sendMessage(String path, WebSocketServer webSocketServer, T obj) {
        try {
            webSocketServer.send(new ExecuteWrapperResult<>(path, obj));
        } catch (IOException e) {
            log.error("执行任务失败，任务详情：{}", JsonUtils.toPrettyString(obj), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "执行任务失败");
        }
        return new ReturnT<>(ReturnT.SUCCESS_CODE, "websocket触发任务执行成功");
    }
}
