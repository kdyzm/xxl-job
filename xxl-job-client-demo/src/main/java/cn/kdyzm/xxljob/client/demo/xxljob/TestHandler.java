package cn.kdyzm.xxljob.client.demo.xxljob;

import cn.kdyzm.component.xxljob.context.XxlJobHelper;
import cn.kdyzm.component.xxljob.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author kdyzm
 * @date 2025/2/8
 */
@Component
@Slf4j
public class TestHandler {


    @XxlJob("test")
    public void handle(String args) {
        XxlJobHelper.log("处理参数：{}", args);
        XxlJobHelper.handleSuccess("处理成功");
    }
}
