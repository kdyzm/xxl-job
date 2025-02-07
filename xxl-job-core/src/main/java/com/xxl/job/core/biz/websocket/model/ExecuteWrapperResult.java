package com.xxl.job.core.biz.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author kdyzm
 * @date 2025/2/6
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteWrapperResult<T> {

    private String path;

    private T data;
}
