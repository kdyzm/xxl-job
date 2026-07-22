package com.xxl.job.admin.core.thread;

import com.xxl.job.admin.core.complete.XxlJobCompleter;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * job lose-monitor instance
 *
 * @author xuxueli 2015-9-1 18:05:56
 */
public class JobCompleteHelper {
	private static Logger logger = LoggerFactory.getLogger(JobCompleteHelper.class);

	private static JobCompleteHelper instance = new JobCompleteHelper();
	public static JobCompleteHelper getInstance(){
		return instance;
	}

	// ---------------------- monitor ----------------------

	private ThreadPoolExecutor callbackThreadPool = null;
	private Thread monitorThread;
	private volatile boolean toStop = false;
	public void start(){

		// for callback
		callbackThreadPool = new ThreadPoolExecutor(
				2,
				20,
				30L,
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(3000),
				new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						return new Thread(r, "xxl-job, admin JobLosedMonitorHelper-callbackThreadPool-" + r.hashCode());
					}
				},
				new RejectedExecutionHandler() {
					@Override
					public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
						r.run();
						logger.warn(">>>>>>>>>>> xxl-job, callback too fast, match threadpool rejected handler(run now).");
					}
				});


		// for monitor
		monitorThread = new Thread(new Runnable() {

			@Override
			public void run() {

				// wait for JobTriggerPoolHelper-init
				try {
					TimeUnit.MILLISECONDS.sleep(50);
				} catch (InterruptedException e) {
					if (!toStop) {
						logger.error(e.getMessage(), e);
					}
				}

				// monitor
				while (!toStop) {
					try {
						// 任务结果丢失处理：调度记录停留在 "运行中" 状态超过10min，且对应执行器心跳注册失败不在线，则将本地调度主动标记失败；
						Date losedTime = DateUtil.addMinutes(new Date(), -10);
						List<Long> losedJobIds  = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().findLostJobIds(losedTime);

						if (losedJobIds!=null && losedJobIds.size()>0) {
							logger.warn(">>>>>>>>>>>> xxl-job lost job monitor found {} lost jobs (triggerTime before {})", losedJobIds.size(), losedTime);
							for (Long logId: losedJobIds) {
								XxlJobLog jobLog = new XxlJobLog();
								jobLog.setId(logId);

								jobLog.setHandleTime(new Date());
								jobLog.setHandleCode(ReturnT.FAIL_CODE);
								jobLog.setHandleMsg( I18nUtil.getString("joblog_lost_fail") );

								XxlJobCompleter.updateHandleInfoAndFinish(jobLog);
								logger.warn(">>>>>>>>>>>> xxl-job lost job monitor marked logId={} as FAIL (handleCode=500, msg=joblog_lost_fail)", logId);
							}
						} else {
							logger.debug(">>>>>>>>>>>> xxl-job lost job monitor: no lost jobs found (triggerTime before {})", losedTime);
						}
					} catch (Exception e) {
						if (!toStop) {
							logger.error(">>>>>>>>>>> xxl-job, job fail monitor thread error:{}", e);
						}
					}

					try {
						TimeUnit.SECONDS.sleep(60);
					} catch (Exception e) {
						if (!toStop) {
							logger.error(e.getMessage(), e);
						}
					}

				}

				logger.info(">>>>>>>>>>> xxl-job, JobLosedMonitorHelper stop");

			}
		});
		monitorThread.setDaemon(true);
		monitorThread.setName("xxl-job, admin JobLosedMonitorHelper");
		monitorThread.start();
	}

	public void toStop(){
		toStop = true;

		// stop registryOrRemoveThreadPool
		callbackThreadPool.shutdownNow();

		// stop monitorThread (interrupt and wait)
		monitorThread.interrupt();
		try {
			monitorThread.join();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}


	// ---------------------- helper ----------------------

	public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
		logger.info(">>>>>>>>>>>> xxl-job callback received, batchSize={}", callbackParamList != null ? callbackParamList.size() : 0);
		if (callbackParamList != null) {
			for (HandleCallbackParam param : callbackParamList) {
				logger.info(">>>>>>>>>>>> xxl-job callback item, logId={}, logDateTime={}, handleCode={}, handleMsg={}",
						param.getLogId(), param.getLogDateTim(), param.getHandleCode(), param.getHandleMsg());
			}
		}

		callbackThreadPool.execute(new Runnable() {
			@Override
			public void run() {
				for (HandleCallbackParam handleCallbackParam: callbackParamList) {
					long cbStart = System.currentTimeMillis();
					ReturnT<String> callbackResult = callback(handleCallbackParam);
					long cbCost = System.currentTimeMillis() - cbStart;
					logger.info(">>>>>>>>> xxl-job callback process {}, logId={}, handleCode={}, callbackCode={}, callbackMsg={}, cost={}ms",
							(callbackResult.getCode()== ReturnT.SUCCESS_CODE?"success":"fail"),
							handleCallbackParam.getLogId(), handleCallbackParam.getHandleCode(),
							callbackResult.getCode(), callbackResult.getMsg(), cbCost);
				}
			}
		});

		return ReturnT.SUCCESS;
	}

	private ReturnT<String> callback(HandleCallbackParam handleCallbackParam) {
		long cbStart = System.currentTimeMillis();
		logger.info(">>>>>>>>>>>> xxl-job callback handler START, logId={}, handleCode={}, handleMsg={}",
				handleCallbackParam.getLogId(), handleCallbackParam.getHandleCode(), handleCallbackParam.getHandleMsg());

		// valid log item
		XxlJobLog log = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().load(handleCallbackParam.getLogId());
		if (log == null) {
			logger.warn(">>>>>>>>>>>> xxl-job callback FAIL, log item not found, logId={}", handleCallbackParam.getLogId());
			return new ReturnT<String>(ReturnT.FAIL_CODE, "log item not found.");
		}

		logger.info(">>>>>>>>>>>> xxl-job callback load log, logId={}, currentHandleCode={}, triggerCode={}, executorAddress={}",
				log.getId(), log.getHandleCode(), log.getTriggerCode(), log.getExecutorAddress());

		if (log.getHandleCode() > 0) {
			logger.warn(">>>>>>>>>>>> xxl-job callback IGNORE, duplicate callback, logId={}, existingHandleCode={}, newHandleCode={}",
					log.getId(), log.getHandleCode(), handleCallbackParam.getHandleCode());
			return new ReturnT<String>(ReturnT.FAIL_CODE, "log repeate callback.");     // avoid repeat callback, trigger child job etc
		}

		// handle msg
		StringBuffer handleMsg = new StringBuffer();
		if (log.getHandleMsg()!=null) {
			handleMsg.append(log.getHandleMsg()).append("<br>");
		}
		if (handleCallbackParam.getHandleMsg() != null) {
			handleMsg.append(handleCallbackParam.getHandleMsg());
		}

		// success, save log
		log.setHandleTime(new Date());
		log.setHandleCode(handleCallbackParam.getHandleCode());
		log.setHandleMsg(handleMsg.toString());
		XxlJobCompleter.updateHandleInfoAndFinish(log);

		long cbCost = System.currentTimeMillis() - cbStart;
		logger.info(">>>>>>>>>>>> xxl-job callback handler SUCCESS, logId={}, handleCode={}, cost={}ms", log.getId(), handleCallbackParam.getHandleCode(), cbCost);

		return ReturnT.SUCCESS;
	}

}
