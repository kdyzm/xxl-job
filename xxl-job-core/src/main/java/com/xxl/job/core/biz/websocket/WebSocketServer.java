package com.xxl.job.core.biz.websocket;

import cn.hutool.core.codec.Base64;
import cn.kdyzm.json.util.JsonUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 通过@ServerEndpoint暴露出去的 ws 应用的路径，有点类似我们经常用的@RequestMapping。
 * 比如你的启动端口是8080，而这个注解的值是ws，那我们就可以通过 ws://127.0.0.1:8080/ws 来连接你的应用
 */
@Slf4j
public abstract class WebSocketServer {

    /**
     * 存储在redis中的Endpoint的类型
     *
     * @return Endpoint类型
     */
    public abstract EndpointType getEndpointType();

    /**
     * 每个连接都会有自己的会话
     */
    @Getter
    private Session session;

    @Getter
    private String receiverId;

    @Getter
    private String clientIp;

    @Getter
    @Setter
    private Boolean checkHealth = false;

    /**
     * 心跳时间<br>
     * 可以定义一个WsServerEndpoint存活最大时间，比如为30分钟。
     * 我们通过当前时间对比heartbeatTime，如果30分钟，认为WsServerEndpoint失效了，需要把它关闭，且从本地map中删除。
     */
    @Getter
    private volatile Long heartbeatTime;

    public void updateHeartBeatTime() {
        heartbeatTime = System.currentTimeMillis();
    }

    public WebSocketServer() {
        this.heartbeatTime = System.currentTimeMillis();
    }

    /**
     * 当 websocket 建立连接成功后会触发这个注解修饰的方法，它有一个 Session 参数
     *
     * @param receiverId 用户Id
     * @param session    会话
     */
    @OnOpen
    public void onOpen(@PathParam("receiverId") String receiverId,
                       @PathParam("clientIp") String clientIp,
                       Session session) {

        this.receiverId = receiverId;
        this.clientIp = Base64.decodeStr(clientIp, StandardCharsets.UTF_8);
        this.session = session;

        log.info("WsServerEndpoint 建立新连接 receiverId:{}, sessionId:{}",
                receiverId, session.getId());
        WebSocketServerPool.getInstance().addEndpoint(receiverId, clientIp, this);
        log.info("连接服务器成功，客户端ip地址：{}, receiverId:{}, session:{}", this.clientIp, receiverId, session.getId());
    }

    /**
     * 当 websocket 的连接断开后会触发这个注解修饰的方法，它有一个 Session 参数
     */
    @OnClose
    public void onClose(Session session) {
        WebSocketServerPool.getInstance().removeEndpointBySessionId(
                receiverId,
                clientIp,
                session.getId());
        log.info("websocket closed, receiverId:{}, type:{}, sessionId:{}",
                receiverId, getEndpointType(), session.getId());
    }

    /**
     * 当 websocket 的连接错误时
     */
    @OnError
    public void onError(Throwable error) {
        String sessionId = "";
        if (Objects.nonNull(session)) {
            sessionId = session.getId();
        }
        log.error("websocket connect error. receiverId:{}, type:{}, session:{}", receiverId, getEndpointType(), sessionId, error);
        // 移除session会话
        //WebSocketServerPool.getInstance().removeEndpointBySessionId(userId, session.getId());
    }

    /**
     * 接收消息
     *
     * @param message 消息
     */
    public abstract void onMessage(String message);

    /**
     * 接收到消息
     *
     * @param message 消息
     */
    @OnMessage
    public final void receiveMessage(String message) {

        // 收到消息更新一下最后收到消息的时间戳
        this.updateHeartBeatTime();

        // 心跳检测帧，用于前端判断链接是否正常
        if ("ping".equals(message)) {

            try {
                // 健康检测标记重置为false
                this.setCheckHealth(false);
                this.send("pong");
            } catch (IOException e) {
                log.error("response pong error. receiverId: {}, sessionId: {}", receiverId, session.getId(), e);
            }
            return;
        }

        this.onMessage(message);
    }

    public void send(Object message) throws IOException {
        if (session.isOpen()) {
            session.getBasicRemote()
                    .sendText(message instanceof String ? (String) message : JsonUtils.toString(message));
        }
    }

}
