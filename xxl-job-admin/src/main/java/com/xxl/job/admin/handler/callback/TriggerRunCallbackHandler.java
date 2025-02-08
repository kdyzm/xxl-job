package com.xxl.job.admin.handler.callback;

import cn.kdyzm.json.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.xxl.job.core.queue.MessageQueueManager;
import com.xxl.job.core.biz.model.ReturnT;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author kdyzm
 * @date 2025/2/8
 */
@Component
@Slf4j
@AllArgsConstructor
public class TriggerRunCallbackHandler implements CallbackHandler<JsonNode> {

    @Override
    public void handler(JsonNode callbackData) {
        ReturnT<Integer> result = JsonUtils.read(callbackData.toString(), new TypeReference<ReturnT<Integer>>() {
        });
        //发送消息队列
        MessageQueueManager.getInstance().put("triggerRunCallback", result.getContent(), result);
    }
}
