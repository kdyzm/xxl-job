package com.xxl.job.admin.handler.callback;

/**
 * @author kdyzm
 * @date 2025/2/7
 */
public interface CallbackHandler<T> {

    public void handler(T callbackData);
}
