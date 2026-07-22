package com.xxl.job.admin.core.trigger;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.xxl.job.admin.core.scheduler.XxlJobScheduler;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.util.IpUtil;
import com.xxl.job.core.util.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;

/**
 * xxl-job trigger
 * Created by xuxueli on 17/7/13.
 */
public class XxlJobTrigger {
    private static Logger logger = LoggerFactory.getLogger(XxlJobTrigger.class);

    /**
     * trigger job
     *
     * @param jobId
     * @param triggerType
     * @param failRetryCount
     * 			>=0: use this param
     * 			<0: use param from job info config
     * @param executorShardingParam
     * @param executorParam
     *          null: use job param
     *          not null: cover job param
     * @param addressList
     *          null: use executor addressList
     *          not null: cover
     */
    public static void trigger(int jobId,
                               TriggerTypeEnum triggerType,
                               int failRetryCount,
                               String executorShardingParam,
                               String executorParam,
                               String addressList) {

        logger.info(">>>>>>>>>>>> xxl-job trigger START, jobId={}, triggerType={}, failRetryCount={}, executorShardingParam={}, executorParam={}, addressList={}",
                jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList);

        // load data
        XxlJobInfo jobInfo = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().loadById(jobId);
        if (jobInfo == null) {
            logger.warn(">>>>>>>>>>>> trigger fail, jobId invalid，jobId={}", jobId);
            return;
        }
        if (executorParam != null) {
            jobInfo.setExecutorParam(executorParam);
        }
        int finalFailRetryCount = failRetryCount>=0?failRetryCount:jobInfo.getExecutorFailRetryCount();
        XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().load(jobInfo.getJobGroup());

        logger.info(">>>>>>>>>>>> xxl-job trigger load data, jobInfo=[jobDesc={}, executorHandler={}, executorRouteStrategy={}, executorBlockStrategy={}, executorTimeout={}, executorFailRetryCount={}]",
                jobInfo.getJobDesc(), jobInfo.getExecutorHandler(), jobInfo.getExecutorRouteStrategy(), jobInfo.getExecutorBlockStrategy(), jobInfo.getExecutorTimeout(), jobInfo.getExecutorFailRetryCount());
        if (group != null) {
            logger.info(">>>>>>>>>>>> xxl-job trigger load group, group=[appname={}, title={}, addressType={}, addressList={}, registryList={}]",
                    group.getAppname(), group.getTitle(), group.getAddressType(), group.getAddressList(), group.getRegistryList());
        } else {
            logger.warn(">>>>>>>>>>>> xxl-job trigger load group FAIL, group is null, jobGroup={}", jobInfo.getJobGroup());
        }

        // cover addressList
        if (addressList!=null && addressList.trim().length()>0) {
            group.setAddressType(1);
            group.setAddressList(addressList.trim());
            logger.info(">>>>>>>>>>>> xxl-job trigger cover addressList={}", addressList);
        }

        // sharding param
        int[] shardingParam = null;
        if (executorShardingParam!=null){
            String[] shardingArr = executorShardingParam.split("/");
            if (shardingArr.length==2 && isNumeric(shardingArr[0]) && isNumeric(shardingArr[1])) {
                shardingParam = new int[2];
                shardingParam[0] = Integer.valueOf(shardingArr[0]);
                shardingParam[1] = Integer.valueOf(shardingArr[1]);
            }
        }

        ExecutorRouteStrategyEnum routeStrategy = ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null);
        boolean isBroadcast = ExecutorRouteStrategyEnum.SHARDING_BROADCAST == routeStrategy
                && group != null && group.getRegistryList()!=null && !group.getRegistryList().isEmpty()
                && shardingParam==null;

        logger.info(">>>>>>>>>>>> xxl-job trigger route strategy: routeStrategy={}, isBroadcast={}, shardingParam={}, registryList={}",
                routeStrategy, isBroadcast, shardingParam != null ? Arrays.toString(shardingParam) : null,
                group != null ? group.getRegistryList() : null);

        if (isBroadcast) {
            for (int i = 0; i < group.getRegistryList().size(); i++) {
                logger.info(">>>>>>>>>>>> xxl-job trigger SHARDING_BROADCAST, index={}/{}", i, group.getRegistryList().size());
                processTrigger(group, jobInfo, finalFailRetryCount, triggerType, i, group.getRegistryList().size());
            }
        } else {
            if (shardingParam == null) {
                shardingParam = new int[]{0, 1};
            }
            logger.info(">>>>>>>>>>>> xxl-job trigger NORMAL, shardingIndex={}/{}", shardingParam[0], shardingParam[1]);
            processTrigger(group, jobInfo, finalFailRetryCount, triggerType, shardingParam[0], shardingParam[1]);
        }

        logger.info(">>>>>>>>>>>> xxl-job trigger END, jobId={}", jobId);
    }

    private static boolean isNumeric(String str){
        try {
            int result = Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * @param group                     job group, registry list may be empty
     * @param jobInfo
     * @param finalFailRetryCount
     * @param triggerType
     * @param index                     sharding index
     * @param total                     sharding index
     */
    private static void processTrigger(XxlJobGroup group, XxlJobInfo jobInfo, int finalFailRetryCount, TriggerTypeEnum triggerType, int index, int total){

        long processStartTime = System.currentTimeMillis();

        // param
        ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), ExecutorBlockStrategyEnum.SERIAL_EXECUTION);  // block strategy
        ExecutorRouteStrategyEnum executorRouteStrategyEnum = ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null);    // route strategy
        String shardingParam = (ExecutorRouteStrategyEnum.SHARDING_BROADCAST==executorRouteStrategyEnum)?String.valueOf(index).concat("/").concat(String.valueOf(total)):null;

        logger.info(">>>>>>>>>>>> xxl-job processTrigger START, jobId={}, jobDesc={}, executorHandler={}, triggerType={}, blockStrategy={}, routeStrategy={}, shardingParam={}, index={}, total={}, finalFailRetryCount={}",
                jobInfo.getId(), jobInfo.getJobDesc(), jobInfo.getExecutorHandler(), triggerType, blockStrategy, executorRouteStrategyEnum, shardingParam, index, total, finalFailRetryCount);

        // 1、save log-id
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setJobGroup(jobInfo.getJobGroup());
        jobLog.setJobId(jobInfo.getId());
        jobLog.setTriggerTime(new Date());
        XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().save(jobLog);
        logger.info(">>>>>>>>>>>> xxl-job processTrigger [1] save log-id SUCCESS, logId={}, jobGroup={}, jobId={}, triggerTime={}",
                jobLog.getId(), jobLog.getJobGroup(), jobLog.getJobId(), jobLog.getTriggerTime());

        // save trigger info with default values first (ensure log won't be stuck at triggerCode=0)
        jobLog.setExecutorFailRetryCount(finalFailRetryCount);

        // 2、init trigger-param
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(jobInfo.getId());
        triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
        triggerParam.setExecutorParams(jobInfo.getExecutorParam());
        triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        triggerParam.setExecutorTimeout(jobInfo.getExecutorTimeout());
        triggerParam.setLogId(jobLog.getId());
        triggerParam.setLogDateTime(jobLog.getTriggerTime().getTime());
        triggerParam.setGlueType(jobInfo.getGlueType());
        triggerParam.setGlueSource(jobInfo.getGlueSource());
        triggerParam.setGlueUpdatetime(jobInfo.getGlueUpdatetime().getTime());
        triggerParam.setBroadcastIndex(index);
        triggerParam.setBroadcastTotal(total);

        logger.info(">>>>>>>>>>>> xxl-job processTrigger [2] triggerParam prepared, logId={}, executorHandler={}, executorParams={}, executorTimeout={}, glueType={}, broadcastIndex={}, broadcastTotal={}",
                triggerParam.getLogId(), triggerParam.getExecutorHandler(), triggerParam.getExecutorParams(), triggerParam.getExecutorTimeout(), triggerParam.getGlueType(), triggerParam.getBroadcastIndex(), triggerParam.getBroadcastTotal());

        ReturnT<String> triggerResult = null;
        try {
            // 3、init address
            String address = null;
            ReturnT<String> routeAddressResult = null;
            if (group != null && group.getRegistryList()!=null && !group.getRegistryList().isEmpty()) {
                logger.info(">>>>>>>>>>>> xxl-job processTrigger [3.1] registry available, registryList={}", group.getRegistryList());
                if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum) {
                    if (index < group.getRegistryList().size()) {
                        address = group.getRegistryList().get(index);
                    } else {
                        address = group.getRegistryList().get(0);
                    }
                    logger.info(">>>>>>>>>>>> xxl-job processTrigger [3.2] SHARDING_BROADCAST, resolved address={} (index={}, total={})", address, index, total);
                } else if (executorRouteStrategyEnum != null) {
                    logger.info(">>>>>>>>>>>> xxl-job processTrigger [3.2] routing with strategy={}, registryCount={}", executorRouteStrategyEnum.getTitle(), group.getRegistryList().size());
                    routeAddressResult = executorRouteStrategyEnum.getRouter().route(triggerParam, group.getRegistryList());
                    logger.info(">>>>>>>>>>>> xxl-job processTrigger [3.3] route result, code={}, content={}, msg={}",
                            routeAddressResult.getCode(), routeAddressResult.getContent(), routeAddressResult.getMsg());
                    if (routeAddressResult.getCode() == ReturnT.SUCCESS_CODE) {
                        address = routeAddressResult.getContent();
                    }
                } else {
                    logger.warn(">>>>>>>>>>>> xxl-job processTrigger [3.2] executorRouteStrategyEnum is null, route strategy unknown!");
                    routeAddressResult = new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("jobconf_trigger_address_empty"));
                }
            } else {
                if (group == null) {
                    logger.warn(">>>>>>>>>>>> xxl-job processTrigger [3.1] group is null, cannot route!");
                } else if (group.getRegistryList() == null) {
                    logger.warn(">>>>>>>>>>>> xxl-job processTrigger [3.1] registryList is null, addressList={}, addressType={}", group.getAddressList(), group.getAddressType());
                } else {
                    logger.warn(">>>>>>>>>>>> xxl-job processTrigger [3.1] registryList is EMPTY, addressList={}, addressType={}", group.getAddressList(), group.getAddressType());
                }
                routeAddressResult = new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("jobconf_trigger_address_empty"));
            }

            logger.info(">>>>>>>>>>>> xxl-job processTrigger [3] address resolved: address={}", address);

            // 4、trigger remote executor
            if (address != null) {
                logger.info(">>>>>>>>>>>> xxl-job processTrigger [4] calling runExecutor, address={}, timeout={}s", address, triggerParam.getExecutorTimeout());
                long rpcStartTime = System.currentTimeMillis();
                triggerResult = runExecutor(triggerParam, address);
                long rpcCost = System.currentTimeMillis() - rpcStartTime;
                logger.info(">>>>>>>>>>>> xxl-job processTrigger [4] runExecutor complete, code={}, msg={}, cost={}ms",
                        triggerResult.getCode(), triggerResult.getMsg(), rpcCost);
            } else {
                logger.warn(">>>>>>>>>>>> xxl-job processTrigger [4] address is null, cannot call executor, marking as FAIL");
                triggerResult = new ReturnT<String>(ReturnT.FAIL_CODE, null);
            }

            // 5、collection trigger info
            StringBuffer triggerMsgSb = new StringBuffer();
            triggerMsgSb.append(I18nUtil.getString("jobconf_trigger_type")).append("：").append(triggerType.getTitle());
            triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_admin_adress")).append("：").append(IpUtil.getIp());
            triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_exe_regtype")).append("：")
                    .append( (group != null && group.getAddressType() == 0)?I18nUtil.getString("jobgroup_field_addressType_0"):I18nUtil.getString("jobgroup_field_addressType_1") );
            triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_exe_regaddress")).append("：").append(group != null ? group.getRegistryList() : "null");
            triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorRouteStrategy")).append("：").append(executorRouteStrategyEnum != null ? executorRouteStrategyEnum.getTitle() : "null");
            if (shardingParam != null) {
                triggerMsgSb.append("("+shardingParam+")");
            }
            triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorBlockStrategy")).append("：").append(blockStrategy.getTitle());
            triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_timeout")).append("：").append(jobInfo.getExecutorTimeout());
            triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorFailRetryCount")).append("：").append(finalFailRetryCount);

            triggerMsgSb.append("<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>"+ I18nUtil.getString("jobconf_trigger_run") +"<<<<<<<<<<< </span><br>")
                    .append((routeAddressResult!=null&&routeAddressResult.getMsg()!=null)?routeAddressResult.getMsg()+"<br><br>":"").append(triggerResult.getMsg()!=null?triggerResult.getMsg():"");

            // 6、save log trigger-info
            jobLog.setExecutorAddress(address);
            jobLog.setExecutorHandler(jobInfo.getExecutorHandler());
            jobLog.setExecutorParam(jobInfo.getExecutorParam());
            jobLog.setExecutorShardingParam(shardingParam);
            jobLog.setTriggerCode(triggerResult.getCode());
            jobLog.setTriggerMsg(triggerMsgSb.toString());

            logger.info(">>>>>>>>>>>> xxl-job processTrigger [6] trigger result, address={}, triggerCode={}, shardingParam={}",
                    address, triggerResult.getCode(), shardingParam);

        } catch (Exception e) {
            logger.error(">>>>>>>>>>>> xxl-job trigger error, processTrigger EXCEPTION for jobId={}, jobDesc={}, logId={}",
                    jobInfo.getId(), jobInfo.getJobDesc(), jobLog.getId(), e);
            // Ensure the log record is marked as failed, not stuck at triggerCode=0
            StringBuffer errorSb = new StringBuffer();
            errorSb.append(I18nUtil.getString("jobconf_trigger_type")).append("：").append(triggerType.getTitle());
            errorSb.append("<br>").append("trigger error：").append(e.getMessage());
            jobLog.setTriggerCode(ReturnT.FAIL_CODE);
            jobLog.setTriggerMsg(errorSb.toString());
        }

        // Persist trigger info no matter what
        XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateTriggerInfo(jobLog);
        logger.info(">>>>>>>>>>>> xxl-job processTrigger [6] updateTriggerInfo persisted, logId={}, triggerCode={}", jobLog.getId(), jobLog.getTriggerCode());

        long processCost = System.currentTimeMillis() - processStartTime;
        logger.info(">>>>>>>>>>>> xxl-job processTrigger END, logId={}, totalCost={}ms", jobLog.getId(), processCost);
    }

    /**
     * run executor
     * @param triggerParam
     * @param address
     * @return
     */
    public static ReturnT<String> runExecutor(TriggerParam triggerParam, String address){
        long rpcStart = System.currentTimeMillis();
        ReturnT<String> runResult = null;
        try {
            ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(address);
            logger.info(">>>>>>>>>>>> xxl-job runExecutor calling, address={}, logId={}, executorHandler={}, executorTimeout={}",
                    address, triggerParam.getLogId(), triggerParam.getExecutorHandler(), triggerParam.getExecutorTimeout());
            runResult = executorBiz.run(triggerParam);
            long rpcCost = System.currentTimeMillis() - rpcStart;
            logger.info(">>>>>>>>>>>> xxl-job runExecutor response, address={}, code={}, msg={}, cost={}ms",
                    address, runResult.getCode(), runResult.getMsg(), rpcCost);
        } catch (Exception e) {
            long rpcCost = System.currentTimeMillis() - rpcStart;
            logger.error(">>>>>>>>>>> xxl-job trigger error, please check if the executor[{}] is running, cost={}ms.", address, rpcCost, e);
            runResult = new ReturnT<String>(ReturnT.FAIL_CODE, ThrowableUtil.toString(e));
        }

        StringBuffer runResultSB = new StringBuffer(I18nUtil.getString("jobconf_trigger_run") + "：");
        runResultSB.append("<br>address：").append(address);
        runResultSB.append("<br>code：").append(runResult.getCode());
        runResultSB.append("<br>msg：").append(runResult.getMsg());

        runResult.setMsg(runResultSB.toString());
        return runResult;
    }

}
