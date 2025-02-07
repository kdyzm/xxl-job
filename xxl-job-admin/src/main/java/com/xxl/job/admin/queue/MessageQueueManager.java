package com.xxl.job.admin.queue;

import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author kdyzm
 * @date 2025/2/7
 */
@Slf4j
public class MessageQueueManager {

    private static final MessageQueueManager MESSAGE_QUEUE_MANAGER = new MessageQueueManager();

    private Map<String, ConcurrentHashMap<Long, LinkedBlockingQueue<Object>>> map = new ConcurrentHashMap<>();

    public static MessageQueueManager getInstance() {
        return MESSAGE_QUEUE_MANAGER;
    }

    public void put(String type, Long key, Object value) {
        ConcurrentHashMap<Long, LinkedBlockingQueue<Object>> data = map.get(type);
        if (MapUtil.isEmpty(data)) {
            data = new ConcurrentHashMap<>();
            map.put(type, data);
            data = map.get(type);
        }
        LinkedBlockingQueue<Object> objects = data.get(key);
        if (Objects.isNull(objects)) {
            objects = new LinkedBlockingQueue<>();
            data.put(key, objects);
            objects = data.get(key);
        }
        try {
            objects.put(value);
            log.info("发送消息：{}", value);
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }


    public Object take(String type, Long key) {
        ConcurrentHashMap<Long, LinkedBlockingQueue<Object>> data = map.get(type);
        if (Objects.isNull(data)) {
            return null;
        }
        LinkedBlockingQueue<Object> objects = data.get(key);
        if (Objects.isNull(objects)) {
            return null;
        }
        try {
            log.info("即将接收消息：{},{}", type, key);
            Object take = objects.take();
            log.info("接收到消息：{}", take);
            return take;
        } catch (InterruptedException e) {
            log.error("", e);
        }
        return null;
    }

    public void remove(String type, Long key) {
        ConcurrentHashMap<Long, LinkedBlockingQueue<Object>> data = map.get(type);
        if (Objects.isNull(data)) {
            return;
        }
        data.remove(key);
    }
}
