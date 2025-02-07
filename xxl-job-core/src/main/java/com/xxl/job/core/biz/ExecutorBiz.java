package com.xxl.job.core.biz;

import com.xxl.job.core.biz.model.*;
import com.xxl.job.core.biz.websocket.WebSocketServer;

/**
 * Created by xuxueli on 17/3/1.
 */
public interface ExecutorBiz {

    /**
     * beat
     *
     * @return
     */
    public ReturnT<String> beat(WebSocketServer webSocketServer);

    /**
     * idle beat
     *
     * @param idleBeatParam
     * @return
     */
    public ReturnT<WebSocketServer> idleBeat(WebSocketServer webSocketServer,IdleBeatParam idleBeatParam);

    /**
     * run
     *
     * @param triggerParam
     * @return
     */
    public ReturnT<String> run(WebSocketServer webSocketServer, TriggerParam triggerParam);

    /**
     * kill
     *
     * @param killParam
     * @return
     */
    public ReturnT<String> kill(WebSocketServer webSocketServer,KillParam killParam);

    /**
     * log
     *
     * @param logParam
     * @return
     */
    public ReturnT<LogResult> log(WebSocketServer webSocketServer,LogParam logParam);

}
