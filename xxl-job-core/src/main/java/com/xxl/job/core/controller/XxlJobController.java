package com.xxl.job.core.controller;

import com.xxl.job.core.biz.impl.ExecutorBizImpl;
import com.xxl.job.core.biz.model.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author kdyzm
 * @date 2021/5/7
 */
@RestController
public class XxlJobController {

    @PostMapping("/beat")
    public ReturnT<String> beat() {
        return new ExecutorBizImpl().beat();
    }

    @PostMapping("/idleBeat")
    public ReturnT<String> idleBeat(@RequestBody IdleBeatParam param) {
        return new ExecutorBizImpl().idleBeat(param);
    }

    @PostMapping("/run")
    public ReturnT<String> run(@RequestBody TriggerParam param) {
        return new ExecutorBizImpl().run(param);
    }

    @PostMapping("/kill")
    public ReturnT<String> kill(@RequestBody KillParam param) {
        return new ExecutorBizImpl().kill(param);
    }

    @PostMapping("/log")
    public ReturnT<LogResult> log(@RequestBody LogParam param) {
        return new ExecutorBizImpl().log(param);
    }
}
