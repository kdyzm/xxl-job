package com.xxl.job.admin.handler.callback;

import cn.kdyzm.json.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.dao.XxlJobLogDao;
import com.xxl.job.core.queue.MessageQueueManager;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author kdyzm
 * @date 2025/2/7
 */
@Component
@Slf4j
@AllArgsConstructor
public class JobLogCallbackHandler implements CallbackHandler<JsonNode> {

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;

    @Resource
    public XxlJobInfoDao xxlJobInfoDao;

    @Resource
    public XxlJobLogDao xxlJobLogDao;

    @Override
    public void handler(JsonNode req) {
        ReturnT<LogResult> logResult = JsonUtils.read(req.toString(), new TypeReference<ReturnT<LogResult>>() {
        });
        // is end
        Long logId = logResult.getContent().getLogId();
        if (logResult.getContent() != null && logResult.getContent().getFromLineNum() > logResult.getContent().getToLineNum()) {
            XxlJobLog jobLog = xxlJobLogDao.load(logId);
            if (jobLog.getHandleCode() > 0) {
                logResult.getContent().setEnd(true);
            }
        }
        //发送消息队列
        MessageQueueManager.getInstance().put("logCallback", logId, logResult);
    }
}
