package com.xxl.job.core.biz.websocket.model;

import com.xxl.job.core.biz.websocket.EndpointType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 中转消息，微服务调用消息中心
 *
 * @author xull
 */
@Data
@ApiModel("中转消息，微服务调用消息中心")
public class TransferMessage {
    @ApiModelProperty("所属系统")
    private String belongSystem;

    @ApiModelProperty("消息对象")
    private String messageObject;

    @ApiModelProperty("消息接收者")
    private String messageReceiver;

    @ApiModelProperty("存储在redis中的Endpoint的类型")
    private EndpointType endpointType;

    @ApiModelProperty("消息体")
    private String body;

}

