package com.xxl.job.core.biz.websocket;

/**
 * WebSocket常量定义
 */
public class WebSocketConstants {

    /**
     * 每个用户每个EndpointType最大链接数
     */
    public static final Integer WS_MAX_CONNECTION = 20;

    /**
     * 心跳检测最大超时时间(单位：毫秒)
     */
    public static final Long WS_HEARTBEAT_TIMEOUT = 1000L * 60 * 15;

    public static final Long WATCH_DOG_PERIODS = 1000L * 60 * 5;

}
