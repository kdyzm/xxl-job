package com.xxl.job.core.biz.websocket;

import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.Session;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * 轮询检测失效ws链接
 */
@Slf4j
@Component
public class WebSocketWatchDog {

    private static final CopyOnWriteArrayList<WebSocketServer> TIME_HELPER = new CopyOnWriteArrayList<>();

    WebSocketWatchDog() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1,
                new DefaultThreadFactory("thread-WebSocketWatchDog"));
        executor.scheduleAtFixedRate(() -> TIME_HELPER.forEach(server -> {

            Long heartbeatTime = server.getHeartbeatTime();
            if (System.currentTimeMillis() - heartbeatTime > WebSocketConstants.WS_HEARTBEAT_TIMEOUT) {

                // 健康检测标记
                if (server.getCheckHealth()) {
                    String receiverId = server.getReceiverId();
                    String clientIp = server.getClientIp();
                    Session session = server.getSession();
                    WebSocketServerPool.getInstance().removeEndpointBySessionId(
                            receiverId,
                            clientIp,
                            session.getId());
                    return;
                }

                try {
                    // 向前端发送健康检测帧，并将标记标记为true，下次轮询前若未收到前端响应，则判定为链接已失效
                    server.send("ping");
                    server.setCheckHealth(true);
                } catch (IOException e) {
                    log.error("websocket watchDog send ping error. ", e);
                }

            }

        }), WebSocketConstants.WATCH_DOG_PERIODS, WebSocketConstants.WATCH_DOG_PERIODS, TimeUnit.MILLISECONDS);

    }

    public static void onConnectionCreated(WebSocketServer connection) {
        TIME_HELPER.addIfAbsent(connection);
    }

    public static void onClosedNormally(WebSocketServer connection) {
        TIME_HELPER.remove(connection);
    }
}
