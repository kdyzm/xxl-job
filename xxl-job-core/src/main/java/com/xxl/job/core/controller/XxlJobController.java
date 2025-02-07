package com.xxl.job.core.controller;

import com.google.gson.Gson;
import com.xxl.job.core.biz.impl.ExecutorBizImpl;
import com.xxl.job.core.biz.model.*;
import com.xxl.job.core.biz.websocket.WebSocketServer;
import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author kdyzm
 * @date 2021/5/7
 */
/*@RestController
@RequestMapping("/xxl-job")
public class XxlJobController {

    private static final Logger log = LoggerFactory.getLogger(XxlJobController.class);

    @PostMapping("/beat")
    public ReturnT<String> beat() {
        log.debug("接收到beat请求");
        return new ExecutorBizImpl().beat();
    }

    @PostMapping("/idleBeat")
    public ReturnT<WebSocketServer> idleBeat(@RequestBody IdleBeatParam param) {
        log.debug("接收到idleBeat请求，{}", new Gson().toJson(param));
        return new ExecutorBizImpl().idleBeat(param);
    }

    @PostMapping("/run")
    public ReturnT<String> run(@RequestBody TriggerParam param) {
        log.debug("接收到run请求，{}", new Gson().toJson(param));
        return new ExecutorBizImpl().run(null, param);
    }

    @PostMapping("/kill")
    public ReturnT<String> kill(@RequestBody KillParam param) {
        log.debug("接收到kill请求，{}", new Gson().toJson(param));
        return new ExecutorBizImpl().kill(param);
    }

    @PostMapping("/log")
    public ReturnT<LogResult> log(@RequestBody LogParam param) {
        return new ExecutorBizImpl().log(param);
    }
}*/
