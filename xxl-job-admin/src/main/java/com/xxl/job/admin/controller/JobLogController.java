package com.xxl.job.admin.controller;

import cn.hutool.core.collection.CollUtil;
import com.xxl.job.admin.core.exception.XxlJobException;
import com.xxl.job.admin.core.complete.XxlJobCompleter;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.scheduler.XxlJobScheduler;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.dao.XxlJobLogDao;
import com.xxl.job.core.queue.MessageQueueManager;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.KillParam;
import com.xxl.job.core.biz.model.LogParam;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.websocket.EndpointType;
import com.xxl.job.core.biz.websocket.WebSocketServer;
import com.xxl.job.core.biz.websocket.WebSocketServerPool;
import com.xxl.job.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * index controller
 *
 * @author xuxueli 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/joblog")
public class JobLogController {
    private static Logger logger = LoggerFactory.getLogger(JobLogController.class);

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;
    @Resource
    public XxlJobInfoDao xxlJobInfoDao;
    @Resource
    public XxlJobLogDao xxlJobLogDao;

    @RequestMapping
    public String index(HttpServletRequest request, Model model, @RequestParam(required = false, defaultValue = "0") Integer jobId) {

        // 执行器列表
        List<XxlJobGroup> jobGroupList_all = xxlJobGroupDao.findAll();

        // filter group
        List<XxlJobGroup> jobGroupList = JobInfoController.filterJobGroupByRole(request, jobGroupList_all);
        if (jobGroupList == null || jobGroupList.size() == 0) {
            throw new XxlJobException(I18nUtil.getString("jobgroup_empty"));
        }

        model.addAttribute("JobGroupList", jobGroupList);

        // 任务
        if (jobId > 0) {
            XxlJobInfo jobInfo = xxlJobInfoDao.loadById(jobId);
            if (jobInfo == null) {
                throw new RuntimeException(I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_unvalid"));
            }

            model.addAttribute("jobInfo", jobInfo);

            // valid permission
            JobInfoController.validPermission(request, jobInfo.getJobGroup());
        }

        return "joblog/joblog.index";
    }

    @RequestMapping("/getJobsByGroup")
    @ResponseBody
    public ReturnT<List<XxlJobInfo>> getJobsByGroup(int jobGroup) {
        List<XxlJobInfo> list = xxlJobInfoDao.getJobsByGroup(jobGroup);
        return new ReturnT<List<XxlJobInfo>>(list);
    }

    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(HttpServletRequest request,
                                        @RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        int jobGroup, int jobId, int logStatus, String filterTime) {

        logger.info("开始执行");
        // valid permission
        JobInfoController.validPermission(request, jobGroup);    // 仅管理员支持查询全部；普通用户仅支持查询有权限的 jobGroup

        // parse param
        Date triggerTimeStart = null;
        Date triggerTimeEnd = null;
        if (filterTime != null && filterTime.trim().length() > 0) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                triggerTimeStart = DateUtil.parseDateTime(temp[0]);
                triggerTimeEnd = DateUtil.parseDateTime(temp[1]);
            }
        }

        // page query
        List<XxlJobLog> list = xxlJobLogDao.pageList(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
        List<Integer> jobGroupIds = list.stream().map(XxlJobLog::getJobGroup).collect(Collectors.toList());
        List<Integer> jobIds = list.stream().map(XxlJobLog::getJobId).collect(Collectors.toList());
        List<XxlJobGroup> jobGroups = new ArrayList<>();
        if (!CollectionUtils.isEmpty(jobGroupIds)) {
            jobGroups = xxlJobGroupDao.selectByIds(jobGroupIds);
        }
        List<XxlJobInfo> xxlJobs = new ArrayList<>();
        if (!CollectionUtils.isEmpty(jobIds)) {
            xxlJobs = xxlJobInfoDao.selectByIds(jobIds);
        }

        Map<Integer, XxlJobInfo> xxlJobInfoMap = new HashMap<>();
        Map<Integer, XxlJobGroup> xxlJobGroupMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(jobGroups)) {
            xxlJobGroupMap = jobGroups.stream().collect(Collectors.toMap(XxlJobGroup::getId, i -> i, (v1, v2) -> v2));
        }
        if (!CollectionUtils.isEmpty(xxlJobs)) {
            xxlJobInfoMap = xxlJobs.stream().collect(Collectors.toMap(XxlJobInfo::getId, i -> i, (v1, v2) -> v2));
        }
        Map<Integer, XxlJobGroup> finalXxlJobGroupMap = xxlJobGroupMap;
        Map<Integer, XxlJobInfo> finalXxlJobInfoMap = xxlJobInfoMap;
        list.forEach(item -> {
            int jobGroup1 = item.getJobGroup();
            int jobId1 = item.getJobId();
            XxlJobGroup xxlJobGroup = finalXxlJobGroupMap.get(jobGroup1);
            XxlJobInfo xxlJobInfo = finalXxlJobInfoMap.get(jobId1);
            if (Objects.nonNull(xxlJobGroup)) {
                item.setJobGroupName(xxlJobGroup.getTitle());
            }
            if (Objects.nonNull(xxlJobInfo)) {
                item.setJobName(xxlJobInfo.getJobDesc());
            }
        });
        int list_count = xxlJobLogDao.pageListCount(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);

        // package result
        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("recordsTotal", list_count);        // 总记录数
        maps.put("recordsFiltered", list_count);    // 过滤后的总记录数
        maps.put("data", list);                    // 分页列表
        return maps;
    }

    @RequestMapping("/logDetailPage")
    public String logDetailPage(int id, Model model) {

        // base check
        ReturnT<String> logStatue = ReturnT.SUCCESS;
        XxlJobLog jobLog = xxlJobLogDao.load(id);
        if (jobLog == null) {
            throw new RuntimeException(I18nUtil.getString("joblog_logid_unvalid"));
        }

        model.addAttribute("triggerCode", jobLog.getTriggerCode());
        model.addAttribute("handleCode", jobLog.getHandleCode());
        model.addAttribute("executorAddress", jobLog.getExecutorAddress());
        model.addAttribute("triggerTime", jobLog.getTriggerTime().getTime());
        model.addAttribute("logId", jobLog.getId());
        return "joblog/joblog.detail";
    }

    @RequestMapping("/logDetailCat")
    @ResponseBody
    public ReturnT<LogResult> logDetailCat(
            String executorAddress,
            long triggerTime,
            long logId,
            int fromLineNum) {
        try {
            //查询对应的websocket连接
            XxlJobLog jobLog = xxlJobLogDao.load(logId);
            XxlJobGroup jobGroup = xxlJobGroupDao.load(jobLog.getJobGroup());
            List<WebSocketServer> webSocketServers = WebSocketServerPool.getInstance().getBySystemAndReceiverId(
                    jobGroup.getAppname(),
                    EndpointType.TASK_EXECUTE)
                    .orElse(null);
            WebSocketServer webSocketServer = null;
            if (CollUtil.isNotEmpty(webSocketServers)) {
                webSocketServer = webSocketServers.stream()
                        .filter(item -> item.getClientIp().equals(executorAddress))
                        .findAny().orElse(null);
            }
            if (Objects.isNull(webSocketServer)) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "未找到有效的websocket连接");
            }

            ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(executorAddress);
            executorBiz.log(
                    webSocketServer,
                    new LogParam(triggerTime, logId, fromLineNum)
            );
            //开始监听消息队列
            MessageQueueManager instance = MessageQueueManager.getInstance();
            Object callBackResult;
            while (Objects.isNull(callBackResult = instance.take("logCallback", logId))) {
                TimeUnit.MILLISECONDS.sleep(100);
            }
            logger.info("接收成功消息，即将返回");
            ReturnT<LogResult> returnResult = (ReturnT<LogResult>)callBackResult;
            return returnResult;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ReturnT<LogResult>(ReturnT.FAIL_CODE, e.getMessage());
        }
    }

    @RequestMapping("/logKill")
    @ResponseBody
    public ReturnT<String> logKill(int id) {
        // base check
        XxlJobLog log = xxlJobLogDao.load(id);
        XxlJobInfo jobInfo = xxlJobInfoDao.loadById(log.getJobId());
        if (jobInfo == null) {
            return new ReturnT<String>(500, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        if (ReturnT.SUCCESS_CODE != log.getTriggerCode()) {
            return new ReturnT<String>(500, I18nUtil.getString("joblog_kill_log_limit"));
        }
        XxlJobGroup jobGroup = xxlJobGroupDao.load(jobInfo.getJobGroup());
        // request of kill
        ReturnT<String> runResult = null;
        try {
            List<WebSocketServer> webSocketServers = WebSocketServerPool.getInstance().getBySystemAndReceiverId(
                    jobGroup.getAppname(),
                    EndpointType.TASK_EXECUTE)
                    .orElse(null);
            WebSocketServer webSocketServer = null;
            if (CollUtil.isNotEmpty(webSocketServers)) {
                webSocketServer = webSocketServers.stream().filter(
                        item -> item.getClientIp().equals(log.getExecutorAddress()))
                        .findAny().orElse(null);
            }
            if (Objects.isNull(webSocketServer)) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "未找到有效的websocket连接");
            }
            ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(log.getExecutorAddress());
            runResult = executorBiz.kill(webSocketServer, new KillParam(jobInfo.getId()));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            runResult = new ReturnT<String>(500, e.getMessage());
        }

        if (ReturnT.SUCCESS_CODE == runResult.getCode()) {
            log.setHandleCode(ReturnT.FAIL_CODE);
            log.setHandleMsg(I18nUtil.getString("joblog_kill_log_byman") + ":" + (runResult.getMsg() != null ? runResult.getMsg() : ""));
            log.setHandleTime(new Date());
            XxlJobCompleter.updateHandleInfoAndFinish(log);
            return new ReturnT<String>(runResult.getMsg());
        } else {
            return new ReturnT<String>(500, runResult.getMsg());
        }
    }

    @RequestMapping("/clearLog")
    @ResponseBody
    public ReturnT<String> clearLog(int jobGroup, int jobId, int type) {

        Date clearBeforeTime = null;
        int clearBeforeNum = 0;
        if (type == 1) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -1);    // 清理一个月之前日志数据
        } else if (type == 2) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -3);    // 清理三个月之前日志数据
        } else if (type == 3) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -6);    // 清理六个月之前日志数据
        } else if (type == 4) {
            clearBeforeTime = DateUtil.addYears(new Date(), -1);    // 清理一年之前日志数据
        } else if (type == 5) {
            clearBeforeNum = 1000;        // 清理一千条以前日志数据
        } else if (type == 6) {
            clearBeforeNum = 10000;        // 清理一万条以前日志数据
        } else if (type == 7) {
            clearBeforeNum = 30000;        // 清理三万条以前日志数据
        } else if (type == 8) {
            clearBeforeNum = 100000;    // 清理十万条以前日志数据
        } else if (type == 9) {
            clearBeforeNum = 0;            // 清理所有日志数据
        } else {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("joblog_clean_type_unvalid"));
        }

        List<Long> logIds = null;
        do {
            logIds = xxlJobLogDao.findClearLogIds(jobGroup, jobId, clearBeforeTime, clearBeforeNum, 1000);
            if (logIds != null && logIds.size() > 0) {
                xxlJobLogDao.clearLog(logIds);
            }
        } while (logIds != null && logIds.size() > 0);

        return ReturnT.SUCCESS;
    }

}
