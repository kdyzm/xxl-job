/*
package com.xxl.job.core.biz.client;

import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.*;
import com.xxl.job.core.biz.websocket.WebSocketServer;
import com.xxl.job.core.util.XxlJobRemotingUtil;

*/
/**
 * admin api test
 *
 * @author xuxueli 2017-07-28 22:14:52
 *//*

public class ExecutorBizClient implements ExecutorBiz {

    public ExecutorBizClient() {
    }
    public ExecutorBizClient(String addressUrl, String accessToken) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;

        // valid
        if (!this.addressUrl.endsWith("/")) {
            this.addressUrl = this.addressUrl + "/";
        }
    }

    private String addressUrl ;
    private String accessToken;
    private int timeout = 3;


    @Override
    public ReturnT<String> beat() {
        return XxlJobRemotingUtil.postBody(addressUrl+"xxl-job/beat", accessToken, timeout, "", String.class);
    }

    @Override
    public ReturnT<WebSocketServer> idleBeat(IdleBeatParam idleBeatParam){
        return XxlJobRemotingUtil.postBody(addressUrl+"xxl-job/idleBeat", accessToken, timeout, idleBeatParam, String.class);
    }

    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "xxl-job/run", accessToken, timeout, triggerParam, String.class);
    }

    @Override
    public ReturnT<String> kill(KillParam killParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "xxl-job/kill", accessToken, timeout, killParam, String.class);
    }

    @Override
    public ReturnT<LogResult> log(LogParam logParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "xxl-job/log", accessToken, timeout, logParam, LogResult.class);
    }

}
*/
