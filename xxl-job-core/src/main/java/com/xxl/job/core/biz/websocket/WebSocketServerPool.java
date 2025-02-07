package com.xxl.job.core.biz.websocket;

import lombok.extern.slf4j.Slf4j;

import javax.websocket.Session;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 保存receiverId和WebSocketServer对应映射,1对多关系
 */
@Slf4j
public final class WebSocketServerPool {

    /**
     * 映射: 用户id => (EndpointType => WebSocketServer)
     * 存储客户端的连接对象,每个客户端连接都会产生一个连接对象,每个用户会关联多个会话
     */
    private final Map<String, Map<EndpointType, List<WebSocketServer>>> userEndpointMap;

    private WebSocketServerPool() {
        userEndpointMap = new ConcurrentHashMap<>();
    }

    /**
     * 添加用户的WsServerEndpoint内存
     *
     * @param clientIp
     * @param appName 用户Id
     * @param endpoint   WsServerEndpoint
     */
    public void addEndpoint(String appName, String clientIp, WebSocketServer endpoint) {

        Objects.requireNonNull(appName);
        Objects.requireNonNull(endpoint);

        EndpointType endpointType = endpoint.getEndpointType();

        // 获取当前用户订阅的，当前频道的所有会话
        // 线程安全
        // endpointType => endpoint列表
        Map<EndpointType, List<WebSocketServer>> endpointServerMap
                = userEndpointMap.computeIfAbsent(
                appName
                , element -> new ConcurrentHashMap<>());

        List<WebSocketServer> endpointList = endpointServerMap.computeIfAbsent(
                endpointType, element -> {
                    log.info("type[] list init...");
                    return new CopyOnWriteArrayList<>();
                }
        );

        log.info("EndpointPool addEndpoint. receiverId:{}, endpointServerMapSize:{}," +
                        " endpointListSize: {}, newSessionId:{}", appName,
                endpointServerMap.size(), endpointList.size(), endpoint.getSession().getId());

        // 1.缓存当前用户最新的session
        endpointList.add(endpoint);
        log.info("add list success.");
        WebSocketWatchDog.onConnectionCreated(endpoint);

        log.info("EndpointPool addEndpoint success.  receiverId:{}, sessionSize:{}, newSessionId:{}",
                appName, endpointServerMap.size(), endpoint.getSession().getId());

        // 2.每个用户每种消息类型最多保留WS_MAX_CONNECTION个session
        while (endpointList.size() > WebSocketConstants.WS_MAX_CONNECTION) {

            log.info("EndpointPool addEndpoint remove connection. receiverId: {}, session size > {}", appName, WebSocketConstants.WS_MAX_CONNECTION);

            //删除缓存
            WebSocketServer removeServer = endpointList.remove(0);
            WebSocketWatchDog.onClosedNormally(removeServer);

            try {

                // 主动断开链接客户端是否能收到通知？
                removeServer.getSession().close();

            } catch (IOException e) {
                log.error("EndpointPool addEndpoint remove connection error.messageObject:{}, " +
                        "receiverId: {}, session size: {}", appName, endpointServerMap.size(), e);
            }

        }

    }

    /**
     * 通过sessionId移除Endpoint缓存
     *
     * @param appName 用户名
     */
    public void removeEndpointBySessionId(
            String appName,
            String clientIp,
            String sessionId) {

        Objects.requireNonNull(appName);
        Objects.requireNonNull(sessionId);

        Map<EndpointType, List<WebSocketServer>> endpointMap
                = userEndpointMap.get(appName);

        // removeIf线程安全, 与add()互斥
        endpointMap.values().forEach(list -> list.removeIf(endpoint -> {

            Session session = endpoint.getSession();

            log.info("remove connection, receiverId: {}, targetSessionId: {}, " +
                    "currentSessionId: {}", appName, sessionId, session.getId());
            if (!sessionId.equals(session.getId())) {
                return false;
            }

            log.info("remove connection matched,receiverId: {}, targetSessionId: {}, " +
                    "currentSessionId: {}", appName, sessionId, session.getId());

            try {
                WebSocketWatchDog.onClosedNormally(endpoint);
                session.close();
            } catch (IOException e) {
                log.error("EndpointPool remove connection error.receiverId: {}," +
                        " sessionId: {}", appName, session, e);
            } catch (Exception e) {
                // 新增try-catch + log print测试是否这里出现问题
                log.error("", e);
                throw e;
            }

            return true;

        }));

    }

    public Optional<List<WebSocketServer>> getBySystemAndReceiverId(String appName,
                                                                    EndpointType endpointType) {
        Map<EndpointType, List<WebSocketServer>> endpointTypeListMap
                = userEndpointMap.get(appName);
        if (Objects.isNull(endpointTypeListMap)) {
            return Optional.empty();
        }
        return Optional.ofNullable(endpointTypeListMap.get(endpointType));
    }

    public static WebSocketServerPool getInstance() {
        return SingletonInstance.getInstance();
    }

    private static class SingletonInstance {
        private static final WebSocketServerPool INSTANCE = new WebSocketServerPool();

        public static WebSocketServerPool getInstance() {
            return INSTANCE;
        }
    }

}
